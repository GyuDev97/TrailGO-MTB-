package com.bike.trailgo.rideassistant;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Emits significant changes once, with cooldowns independent from current state. */
public final class RideEventDetector {
    private final RideAssistantPolicy policy;
    private final Map<RideEventType, Long> lastEventMillis = new EnumMap<>(RideEventType.class);
    private RideState previousState = RideState.FLAT;
    private double nextMilestoneMeters;
    private long paceDropSinceMillis = -1L;

    public RideEventDetector(RideAssistantPolicy policy) {
        this.policy = policy;
        this.nextMilestoneMeters = policy.milestoneDistanceMeters;
    }

    public List<RideEvent> detect(RideContext context) {
        List<RideEvent> result = new ArrayList<>();
        if (context.currentRideState == RideState.LONG_CLIMB && previousState != RideState.LONG_CLIMB) {
            addIfReady(result, RideEventType.LONG_CLIMB_STARTED, context);
        }
        detectSustainedClimbPaceDrop(result, context);
        if (context.currentRideState == RideState.DESCENT
                && context.currentSpeedKmh >= policy.fastDescentKmh) {
            addIfReady(result, RideEventType.FAST_DESCENT, context);
        }
        if (context.rideDistanceMeters >= nextMilestoneMeters) {
            addIfReady(result, RideEventType.RIDE_MILESTONE, context);
            while (context.rideDistanceMeters >= nextMilestoneMeters) {
                nextMilestoneMeters += policy.milestoneDistanceMeters;
            }
        }
        previousState = context.currentRideState;
        return result;
    }

    private void detectSustainedClimbPaceDrop(List<RideEvent> result, RideContext context) {
        boolean isClimbing = context.currentRideState == RideState.CLIMB
                || context.currentRideState == RideState.LONG_CLIMB;
        boolean hasPaceDrop = context.avgSpeed5mKmh > 0.0d
                && context.avgSpeed1mKmh + policy.paceDropKmh <= context.avgSpeed5mKmh;
        boolean isMeaningful = isClimbing
                && context.currentSpeedKmh >= policy.movingSpeedKmh
                && context.stoppedSeconds == 0L
                && context.estimatedGradePercent >= policy.paceDropMinimumGradePercent;
        if (!hasPaceDrop || !isMeaningful) {
            paceDropSinceMillis = -1L;
            return;
        }
        if (paceDropSinceMillis < 0L) paceDropSinceMillis = context.timestampMs;
        if (context.timestampMs - paceDropSinceMillis >= policy.paceDropMinimumSustainedSeconds * 1000L) {
            addIfReady(result, RideEventType.PACE_DROPPED, context);
        }
    }

    private void addIfReady(List<RideEvent> output, RideEventType type, RideContext context) {
        Long last = lastEventMillis.get(type);
        long cooldownMillis = policy.eventCooldownSeconds * 1000L;
        if (last != null && context.timestampMs - last < cooldownMillis) return;
        lastEventMillis.put(type, context.timestampMs);
        output.add(new RideEvent(type, context.timestampMs, context));
    }
}
