package com.bike.trailgo.hazard;

/**
 * Thresholds for hazard warnings. The radius of a zone and the distance at which the rider is warned are
 * different things: a warning fires when the rider is {@link #warnDistanceM} from the zone's <em>edge</em>.
 */
public final class HazardAlertConfig {
    public static final int DEFAULT_WARN_DISTANCE_M = 100;
    public static final int[] WARN_DISTANCE_CHOICES_M = {50, 100, 150, 200};

    /** Pre-warning distance from the zone edge. */
    public final double warnDistanceM;
    /** Fixes less accurate than this are ignored completely (neither warn nor change any state). */
    public final double maxAccuracyM;
    /** Entering a zone is only announced from a fix at least this accurate. */
    public final double enterMaxAccuracyM;
    /** The rider must be this far outside the edge before leaving the zone counts. */
    public final double exitMarginM;
    /** After a warning the rider must get this much further than {@link #warnDistanceM} before the zone re-arms. */
    public final double rearmExtraM;
    /** Minimum time between two entry announcements of the same zone. */
    public final long reenterCooldownMs;
    /** Without a usable fix for this long the on-screen status is withdrawn. */
    public final long staleAfterMs;

    public HazardAlertConfig(double warnDistanceM) {
        this(warnDistanceM, 50.0, 30.0, 10.0, 100.0, 120_000L, 10_000L);
    }

    public HazardAlertConfig(double warnDistanceM, double maxAccuracyM, double enterMaxAccuracyM,
                             double exitMarginM, double rearmExtraM, long reenterCooldownMs, long staleAfterMs) {
        this.warnDistanceM = warnDistanceM;
        this.maxAccuracyM = maxAccuracyM;
        this.enterMaxAccuracyM = enterMaxAccuracyM;
        this.exitMarginM = exitMarginM;
        this.rearmExtraM = rearmExtraM;
        this.reenterCooldownMs = reenterCooldownMs;
        this.staleAfterMs = staleAfterMs;
    }

    public static HazardAlertConfig defaults() {
        return new HazardAlertConfig(DEFAULT_WARN_DISTANCE_M);
    }

    /** Snaps any stored value to a supported choice, so a corrupt preference never disables or inflates warnings. */
    public static int sanitizeWarnDistance(int meters) {
        for (int choice : WARN_DISTANCE_CHOICES_M) {
            if (choice == meters) return meters;
        }
        return DEFAULT_WARN_DISTANCE_M;
    }
}
