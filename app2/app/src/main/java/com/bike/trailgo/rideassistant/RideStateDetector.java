package com.bike.trailgo.rideassistant;

public final class RideStateDetector {
    private final RideAssistantPolicy policy;

    public RideStateDetector(RideAssistantPolicy policy) {
        this.policy = policy;
    }

    public RideState detect(RideContext context) {
        if (context.stoppedSeconds >= policy.stoppedAfterSeconds) return RideState.STOPPED;
        if (context.continuousClimbSeconds >= policy.longClimbMinimumSeconds
                && context.elevationGain10mMeters >= policy.longClimbMinimumGainMeters) {
            return RideState.LONG_CLIMB;
        }
        if (context.elevationGain1mMeters >= policy.climbGainPerMinuteMeters
                && context.estimatedGradePercent > 0.5d) return RideState.CLIMB;
        if (context.elevationLoss1mMeters >= policy.descentLossPerMinuteMeters
                && context.estimatedGradePercent < -0.5d) return RideState.DESCENT;
        return RideState.FLAT;
    }
}
