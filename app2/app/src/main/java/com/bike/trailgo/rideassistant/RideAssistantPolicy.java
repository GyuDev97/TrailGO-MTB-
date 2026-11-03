package com.bike.trailgo.rideassistant;

/** Tunable thresholds for the first on-device riding-assistant MVP. */
public final class RideAssistantPolicy {
    public final long[] windowSeconds;
    public final double movingSpeedKmh;
    public final long stoppedAfterSeconds;
    public final double minimumAltitudeChangeMeters;
    public final long elevationTrendGraceSeconds;
    public final double climbGainPerMinuteMeters;
    public final double descentLossPerMinuteMeters;
    public final long longClimbMinimumSeconds;
    public final double longClimbMinimumGainMeters;
    public final double paceDropKmh;
    public final long paceDropMinimumSustainedSeconds;
    public final double paceDropMinimumGradePercent;
    public final double fastDescentKmh;
    public final double milestoneDistanceMeters;
    public final long eventCooldownSeconds;
    public final long dangerCooldownSeconds;
    public final double dangerNearMeters;
    public final double dangerApproachMeters;

    public RideAssistantPolicy() {
        this(false);
    }

    public RideAssistantPolicy(boolean testMode) {
        windowSeconds = testMode ? new long[]{5L, 10L, 15L} : new long[]{60L, 300L, 600L};
        movingSpeedKmh = 2.0d;
        stoppedAfterSeconds = testMode ? 8L : 45L;
        minimumAltitudeChangeMeters = testMode ? 1.0d : 3.0d;
        elevationTrendGraceSeconds = testMode ? 4L : 45L;
        climbGainPerMinuteMeters = testMode ? 2.0d : 4.0d;
        descentLossPerMinuteMeters = testMode ? 3.0d : 6.0d;
        longClimbMinimumSeconds = testMode ? 15L : 600L;
        longClimbMinimumGainMeters = testMode ? 5.0d : 60.0d;
        paceDropKmh = testMode ? 0.5d : 1.5d;
        paceDropMinimumSustainedSeconds = testMode ? 4L : 30L;
        paceDropMinimumGradePercent = 0.5d;
        fastDescentKmh = testMode ? 15.0d : 35.0d;
        milestoneDistanceMeters = testMode ? 100.0d : 5000.0d;
        eventCooldownSeconds = testMode ? 20L : 600L;
        dangerCooldownSeconds = testMode ? 30L : 900L;
        dangerNearMeters = 100.0d;
        dangerApproachMeters = 300.0d;
    }
}
