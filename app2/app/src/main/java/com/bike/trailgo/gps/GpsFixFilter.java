package com.bike.trailgo.gps;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Decides, fix by fix, whether a location becomes a recorded track point and how much distance it
 * adds. It never changes a coordinate: a fix is either recorded exactly as measured or not recorded.
 *
 * <p>Order of checks: validity, staleness (cached last-known locations), ordering, hard accuracy
 * limit, then movement plausibility relative to the last recorded point (the <em>anchor</em>):</p>
 * <ul>
 * <li><b>Stationary jitter</b>: displacement from the anchor inside the noise radius (derived from the
 * reported accuracy, clamped) is not movement. The anchor stays put, so slow real movement still
 * accumulates until it leaves the radius; nothing is lost, only jitter is not counted.</li>
 * <li><b>Poor accuracy</b> alone never discards a ride: such fixes are deferred while better ones are
 * arriving, and recorded after {@link GpsQualityConfig#poorAccuracyPatienceMillis}.</li>
 * <li><b>Jumps</b>: a displacement that would need more than {@link GpsQualityConfig#maxSpeedMps} is
 * rejected, but if later fixes consistently confirm the new place, it is accepted as a re-sync
 * (no distance is added across the discontinuity).</li>
 * </ul>
 *
 * <p>Pure Java, not thread-safe: feed it from the thread that handles location updates.</p>
 */
public final class GpsFixFilter {

    public enum Verdict {
        RECORDED_FIRST(true),
        RECORDED_MOVE(true),
        RECORDED_RESYNC(true),
        IGNORED_STATIONARY(false),
        WAITING_FIRST_FIX(false),
        DEFERRED_POOR_ACCURACY(false),
        REJECTED_STALE(false),
        REJECTED_OUT_OF_ORDER(false),
        REJECTED_INVALID(false),
        REJECTED_POOR_ACCURACY(false),
        REJECTED_JUMP(false);

        public final boolean recorded;

        Verdict(boolean recorded) {
            this.recorded = recorded;
        }
    }

    public static final class Result {
        public final Verdict verdict;
        /** Distance to add to the ride; only non-zero for {@link Verdict#RECORDED_MOVE}. */
        public final double distanceMeters;
        public final double impliedSpeedMps;
        /** True when the point was recorded although its accuracy is worse than "poor". */
        public final boolean lowQuality;
        public final String detail;

        Result(Verdict verdict, double distanceMeters, double impliedSpeedMps, boolean lowQuality, String detail) {
            this.verdict = verdict;
            this.distanceMeters = distanceMeters;
            this.impliedSpeedMps = impliedSpeedMps;
            this.lowQuality = lowQuality;
            this.detail = detail;
        }

        public boolean isRecorded() {
            return verdict.recorded;
        }

        @Override
        public String toString() {
            return verdict + (lowQuality ? "(lowQuality)" : "") + " " + detail;
        }
    }

    private static final long NONE = Long.MIN_VALUE;

    private final GpsQualityConfig config;
    private final Map<Verdict, Integer> counts = new EnumMap<>(Verdict.class);

    private GpsFix anchor;
    private double anchorAccuracy;
    private long anchorTimeNanos;
    private long lastRecordedNanos = NONE;
    private long lastSeenNanos = NONE;
    private long waitStartNanos = NONE;
    private int jumpRun;
    private GpsFix jumpCandidate;

    public GpsFixFilter(GpsQualityConfig config) {
        this.config = config;
    }

    /** Forget the anchor (ride paused or restored): the next fix starts a fresh segment without distance. */
    public void reset() {
        anchor = null;
        lastRecordedNanos = NONE;
        waitStartNanos = NONE;
        jumpRun = 0;
        jumpCandidate = null;
    }

    public boolean hasRecordedPoint() {
        return anchor != null;
    }

    public Result evaluate(GpsFix fix, long nowElapsedNanos) {
        Result result = decide(fix, nowElapsedNanos);
        Integer previous = counts.get(result.verdict);
        counts.put(result.verdict, previous == null ? 1 : previous + 1);
        return result;
    }

    /** "RECORDED_MOVE=120 IGNORED_STATIONARY=14 ..." for a ride-end debug log. */
    public String summary() {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Verdict, Integer> entry : counts.entrySet()) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return text.toString();
    }

    private Result decide(GpsFix fix, long nowNanos) {
        if (!validCoordinates(fix)) {
            return reject(Verdict.REJECTED_INVALID, "invalid coordinates");
        }
        long t = fix.elapsedRealtimeNanos > 0 ? fix.elapsedRealtimeNanos : nowNanos;
        if (fix.elapsedRealtimeNanos > 0) {
            long ageMs = (nowNanos - t) / 1_000_000L;
            if (ageMs > config.maxFixAgeMillis) {
                return reject(Verdict.REJECTED_STALE, "age=" + ageMs + "ms > " + config.maxFixAgeMillis + "ms");
            }
        }
        if (lastSeenNanos != NONE && t <= lastSeenNanos) {
            return reject(Verdict.REJECTED_OUT_OF_ORDER, "not newer than the previous fix");
        }
        lastSeenNanos = t;

        double accuracy = fix.hasHorizontalAccuracy() ? fix.horizontalAccuracyMeters : config.unknownAccuracyMeters;
        if (accuracy > config.hardMaxHorizontalAccuracyMeters) {
            return reject(Verdict.REJECTED_POOR_ACCURACY,
                    fmt("accuracy=%.0fm > hard max %.0fm", accuracy, config.hardMaxHorizontalAccuracyMeters));
        }
        boolean poor = accuracy > config.poorHorizontalAccuracyMeters;

        if (anchor == null) {
            if (poor) {
                if (waitStartNanos == NONE) {
                    waitStartNanos = t;
                }
                if (t - waitStartNanos < config.poorAccuracyPatienceMillis * 1_000_000L) {
                    return new Result(Verdict.WAITING_FIRST_FIX, 0, 0, false,
                            fmt("first fix accuracy=%.0fm, waiting for a better one", accuracy));
                }
            }
            record(fix, accuracy, t);
            return new Result(Verdict.RECORDED_FIRST, 0, 0, poor, fmt("accuracy=%.0fm", accuracy));
        }

        if (poor && t - lastRecordedNanos < config.poorAccuracyPatienceMillis * 1_000_000L) {
            return new Result(Verdict.DEFERRED_POOR_ACCURACY, 0, 0, false,
                    fmt("accuracy=%.0fm, waiting for a better fix", accuracy));
        }

        double distance = GeoMath.distanceMeters(anchor.latitude, anchor.longitude, fix.latitude, fix.longitude);
        // Both the anchor and the new fix carry error, so their separation is noisier than either alone.
        double radius = noiseRadius(Math.hypot(accuracy, anchorAccuracy));
        if (distance <= radius) {
            jumpRun = 0;
            jumpCandidate = null;
            return new Result(Verdict.IGNORED_STATIONARY, 0, 0, false,
                    fmt("moved %.1fm within noise radius %.1fm", distance, radius));
        }
        if (dopplerSaysStationary(fix) && distance <= radius * config.stationaryDisplacementFactor) {
            return new Result(Verdict.IGNORED_STATIONARY, 0, 0, false,
                    fmt("speed=%.2fm/s says stationary, moved %.1fm (radius %.1fm)", fix.speedMps, distance, radius));
        }

        double seconds = (t - anchorTimeNanos) / 1e9;
        double implied = seconds > 0 ? distance / seconds : Double.POSITIVE_INFINITY;
        if (implied > config.maxSpeedMps) {
            if (jumpRun > 0 && jumpCandidate != null
                    && GeoMath.distanceMeters(jumpCandidate.latitude, jumpCandidate.longitude,
                            fix.latitude, fix.longitude) <= radius) {
                jumpRun++;
            } else {
                jumpRun = 1;
            }
            jumpCandidate = fix;
            if (jumpRun >= config.resyncAfterJumps) {
                record(fix, accuracy, t);
                return new Result(Verdict.RECORDED_RESYNC, 0, implied, poor,
                        fmt("confirmed %d times: new position accepted, %.0fm gap not counted", jumpRun, distance));
            }
            return new Result(Verdict.REJECTED_JUMP, 0, implied, false,
                    fmt("%.0fm in %.1fs = %.1fm/s > %.1fm/s", distance, seconds, implied, config.maxSpeedMps));
        }

        record(fix, accuracy, t);
        return new Result(Verdict.RECORDED_MOVE, distance, implied, poor,
                fmt("%.1fm in %.1fs (accuracy=%.0fm)", distance, seconds, accuracy));
    }

    private void record(GpsFix fix, double accuracy, long t) {
        anchor = fix;
        anchorAccuracy = accuracy;
        anchorTimeNanos = t;
        lastRecordedNanos = t;
        waitStartNanos = NONE;
        jumpRun = 0;
        jumpCandidate = null;
    }

    private double noiseRadius(double accuracy) {
        double radius = accuracy * config.noiseRadiusFactor;
        return Math.max(config.minNoiseRadiusMeters, Math.min(config.maxNoiseRadiusMeters, radius));
    }

    private boolean dopplerSaysStationary(GpsFix fix) {
        if (!fix.hasSpeed()) {
            return false;
        }
        boolean accuracyOk = Double.isNaN(fix.speedAccuracyMps)
                || fix.speedAccuracyMps <= config.maxTrustedSpeedAccuracyMps;
        return accuracyOk && fix.speedMps < config.stationarySpeedMps;
    }

    private static boolean validCoordinates(GpsFix fix) {
        double lat = fix.latitude;
        double lon = fix.longitude;
        if (Double.isNaN(lat) || Double.isNaN(lon) || Double.isInfinite(lat) || Double.isInfinite(lon)) {
            return false;
        }
        if (Math.abs(lat) > 90.0 || Math.abs(lon) > 180.0) {
            return false;
        }
        // (0,0) is what an unset location looks like.
        return !(Math.abs(lat) < 1e-6 && Math.abs(lon) < 1e-6);
    }

    private static Result reject(Verdict verdict, String detail) {
        return new Result(verdict, 0, 0, false, detail);
    }

    private static String fmt(String pattern, Object... args) {
        return String.format(Locale.ROOT, pattern, args);
    }
}
