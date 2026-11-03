package com.bike.trailgo.hazard;

import com.bike.trailgo.gps.GeoMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decides, fix by fix, which hazard zones to announce. Pure Java, no clock of its own, not thread-safe:
 * feed it from the thread that receives locations.
 *
 * <p>Each zone has a small state machine so that GPS noise around a boundary cannot make the app repeat itself:</p>
 * <pre>
 * FAR --edge&lt;=warn--> APPROACHING --edge&lt;=0--> INSIDE --edge&gt;=exitMargin--> EXITED
 *  ^                       |                                                  |
 *  +--- edge &gt; warn+rearm -+--------------------------------------------------+
 * </pre>
 * <ul>
 * <li>A pre-warning is given once per approach, and only while the rider is getting closer.</li>
 * <li>Entering is announced from reasonably accurate fixes only; a worse fix can warn but never "enters".</li>
 * <li>After leaving, entering again within the cooldown is silent; going far away (past the warning
 * distance plus {@link HazardAlertConfig#rearmExtraM}) re-arms the zone for a new pre-warning.</li>
 * <li>Fixes with unknown or poor accuracy are ignored; without a usable fix the status expires.</li>
 * </ul>
 */
public final class HazardProximityTracker {

    private enum Phase {
        FAR, APPROACHING, INSIDE, EXITED
    }

    private static final long NEVER = Long.MIN_VALUE;
    /** 108 km/h: faster than any rider, so a displacement needing more is a GPS jump. */
    private static final double MAX_PLAUSIBLE_SPEED_MPS = 30.0;
    private static final double JUMP_MIN_DISTANCE_M = 100.0;
    private static final double JUMP_CONFIRM_RADIUS_M = 50.0;
    private static final double JUMP_CHECK_WINDOW_S = 60.0;

    private static final class State {
        Phase phase = Phase.FAR;
        long lastEnterAlertMs = NEVER;
        double lastEdgeM = Double.NaN;
    }

    /** Result of one {@link #evaluate} call. */
    public static final class Update {
        /** Announcements to make now, most important first (entries before pre-warnings, then nearest). */
        public final List<HazardAlert> alerts;
        /** The fix was good enough to be used. */
        public final boolean usable;

        Update(List<HazardAlert> alerts, boolean usable) {
            this.alerts = alerts;
            this.usable = usable;
        }
    }

    private final Map<Integer, State> states = new HashMap<>();
    private List<Hazard> hazards = Collections.emptyList();
    private HazardStatus lastStatus;
    private long lastUsableMs = NEVER;
    private double lastGoodLat;
    private double lastGoodLon;
    private long lastGoodMs = NEVER;
    private boolean pendingJump;
    private double pendingLat;
    private double pendingLon;

    /** Replaces the zones to watch. Zones that are still present keep their state (no repeat announcements). */
    public void setHazards(List<Hazard> next) {
        List<Hazard> valid = new ArrayList<>();
        if (next != null) {
            for (Hazard hazard : next) {
                if (hazard != null && hazard.hasValidPosition()) valid.add(hazard);
            }
        }
        hazards = valid;
        Map<Integer, State> kept = new HashMap<>();
        for (Hazard hazard : valid) {
            State state = states.get(hazard.id);
            if (state != null) kept.put(hazard.id, state);
        }
        states.clear();
        states.putAll(kept);
        if (lastStatus != null && !containsId(valid, lastStatus.hazard.id)) lastStatus = null;
    }

    public int hazardCount() {
        return hazards.size();
    }

    /** Forgets all progress (a new ride): every zone can warn again. */
    public void reset() {
        states.clear();
        lastStatus = null;
        lastUsableMs = NEVER;
        lastGoodMs = NEVER;
        pendingJump = false;
    }

    /**
     * @param accuracyM horizontal accuracy of the fix in metres, NaN when unknown
     * @param nowMs     time of the fix
     */
    public Update evaluate(double latitude, double longitude, double accuracyM, long nowMs, HazardAlertConfig config) {
        boolean usable = !Double.isNaN(accuracyM) && accuracyM > 0 && accuracyM <= config.maxAccuracyM
                && !Double.isNaN(latitude) && !Double.isNaN(longitude);
        if (!usable) {
            return new Update(Collections.<HazardAlert>emptyList(), false);
        }
        if (isUnconfirmedJump(latitude, longitude, nowMs)) {
            return new Update(Collections.<HazardAlert>emptyList(), false);
        }
        lastUsableMs = nowMs;
        lastGoodLat = latitude;
        lastGoodLon = longitude;
        lastGoodMs = nowMs;
        pendingJump = false;
        boolean accurateEnough = accuracyM <= config.enterMaxAccuracyM;

        List<HazardAlert> alerts = new ArrayList<>();
        List<Candidate> nearby = new ArrayList<>();
        for (Hazard hazard : hazards) {
            double edge = GeoMath.distanceMeters(latitude, longitude, hazard.latitude, hazard.longitude)
                    - hazard.effectiveRadiusM();
            State state = states.get(hazard.id);
            if (state == null) {
                state = new State();
                states.put(hazard.id, state);
            }
            step(hazard, state, edge, accurateEnough, nowMs, config, alerts);
            state.lastEdgeM = edge;
            if (state.phase == Phase.INSIDE || (state.phase != Phase.FAR && edge <= config.warnDistanceM)) {
                nearby.add(new Candidate(hazard, state.phase == Phase.INSIDE, Math.max(0.0, edge)));
            }
        }

        Collections.sort(alerts, new Comparator<HazardAlert>() {
            @Override
            public int compare(HazardAlert a, HazardAlert b) {
                if (a.kind != b.kind) return a.kind == HazardAlert.Kind.ENTER ? -1 : 1;
                return Double.compare(a.edgeDistanceM, b.edgeDistanceM);
            }
        });
        lastStatus = pickStatus(nearby);
        return new Update(alerts, true);
    }

    /**
     * The banner content, or null when nothing is near or the GPS has been unusable for too long, so a
     * lost signal does not leave an old distance on the screen.
     */
    public HazardStatus status(long nowMs, HazardAlertConfig config) {
        if (lastStatus == null || lastUsableMs == NEVER || nowMs - lastUsableMs > config.staleAfterMs) return null;
        return lastStatus;
    }

    /**
     * A fix that would need an impossible speed from the last good one is held back; only a second fix near
     * it (a real relocation, e.g. after a tunnel or a long gap) is believed. A lone outlier never warns.
     */
    private boolean isUnconfirmedJump(double lat, double lon, long nowMs) {
        if (lastGoodMs == NEVER) return false;
        double seconds = Math.max(1.0, (nowMs - lastGoodMs) / 1000.0);
        if (seconds > JUMP_CHECK_WINDOW_S) return false;
        double jump = GeoMath.distanceMeters(lastGoodLat, lastGoodLon, lat, lon);
        if (jump < JUMP_MIN_DISTANCE_M || jump / seconds <= MAX_PLAUSIBLE_SPEED_MPS) return false;
        if (pendingJump && GeoMath.distanceMeters(pendingLat, pendingLon, lat, lon) <= JUMP_CONFIRM_RADIUS_M) {
            return false;
        }
        pendingJump = true;
        pendingLat = lat;
        pendingLon = lon;
        return true;
    }

    private void step(Hazard hazard, State state, double edge, boolean accurateEnough, long nowMs,
                      HazardAlertConfig config, List<HazardAlert> alerts) {
        boolean closing = Double.isNaN(state.lastEdgeM) || edge < state.lastEdgeM + 1.0;
        switch (state.phase) {
            case FAR:
                if (edge <= 0 && accurateEnough) {
                    enter(hazard, state, edge, nowMs, alerts);
                } else if (edge <= config.warnDistanceM && closing) {
                    state.phase = Phase.APPROACHING;
                    alerts.add(new HazardAlert(HazardAlert.Kind.PRE_WARN, hazard, Math.max(0.0, edge)));
                }
                break;
            case APPROACHING:
                if (edge <= 0 && accurateEnough) {
                    enter(hazard, state, edge, nowMs, alerts);
                } else if (edge > config.warnDistanceM + config.rearmExtraM) {
                    state.phase = Phase.FAR;
                }
                break;
            case INSIDE:
                if (edge >= config.exitMarginM) state.phase = Phase.EXITED;
                break;
            case EXITED:
                if (edge <= 0 && accurateEnough) {
                    boolean cooledDown = state.lastEnterAlertMs == NEVER
                            || nowMs - state.lastEnterAlertMs >= config.reenterCooldownMs;
                    if (cooledDown) {
                        enter(hazard, state, edge, nowMs, alerts);
                    } else {
                        state.phase = Phase.INSIDE; // back in, but too soon to say it again
                    }
                } else if (edge > config.warnDistanceM + config.rearmExtraM) {
                    state.phase = Phase.FAR;
                }
                break;
            default:
                break;
        }
    }

    private static void enter(Hazard hazard, State state, double edge, long nowMs, List<HazardAlert> alerts) {
        state.phase = Phase.INSIDE;
        state.lastEnterAlertMs = nowMs;
        alerts.add(new HazardAlert(HazardAlert.Kind.ENTER, hazard, edge));
    }

    private static HazardStatus pickStatus(List<Candidate> candidates) {
        if (candidates.isEmpty()) return null;
        Candidate best = candidates.get(0);
        for (Candidate candidate : candidates) {
            if (candidate.inside != best.inside ? candidate.inside : candidate.edgeM < best.edgeM) best = candidate;
        }
        return new HazardStatus(best.hazard, best.inside, best.edgeM, candidates.size() - 1);
    }

    private static boolean containsId(List<Hazard> list, int id) {
        for (Hazard hazard : list) {
            if (hazard.id == id) return true;
        }
        return false;
    }

    private static final class Candidate {
        final Hazard hazard;
        final boolean inside;
        final double edgeM;

        Candidate(Hazard hazard, boolean inside, double edgeM) {
            this.hazard = hazard;
            this.inside = inside;
            this.edgeM = edgeM;
        }
    }
}
