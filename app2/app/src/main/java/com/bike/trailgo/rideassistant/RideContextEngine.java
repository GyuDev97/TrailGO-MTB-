package com.bike.trailgo.rideassistant;

import java.util.ArrayDeque;
import java.util.Deque;

/** Maintains a bounded 10-minute history and derives rolling ride statistics. */
public final class RideContextEngine {
    private static final long HISTORY_MILLIS = 10L * 60L * 1000L;
    private static final double ALTITUDE_SMOOTHING = 0.25d;

    private final RideAssistantPolicy policy;
    private final Deque<Node> history = new ArrayDeque<>();
    private RideSample lastSample;
    private long firstTimestampMs = -1L;
    private long stoppedSinceMs = -1L;
    private long climbSinceMs = -1L;
    private long descentSinceMs = -1L;
    private long lastClimbEvidenceMs = -1L;
    private long lastDescentEvidenceMs = -1L;
    private long manualPauseStartedMs = -1L;
    private long manualPausedMillis;
    private double filteredAltitude;
    private double significantAltitudeReference;
    private double totalDistanceMeters;
    private double totalElevationGainMeters;

    public RideContextEngine(RideAssistantPolicy policy) {
        this.policy = policy;
    }

    public void pause(long timestampMs) {
        if (manualPauseStartedMs < 0L) manualPauseStartedMs = timestampMs;
    }

    public void resume(long timestampMs) {
        if (manualPauseStartedMs >= 0L) {
            manualPausedMillis += Math.max(0L, timestampMs - manualPauseStartedMs);
            manualPauseStartedMs = -1L;
        }
        lastSample = null; // Do not turn an app pause into a synthetic route segment.
        climbSinceMs = -1L;
        descentSinceMs = -1L;
        stoppedSinceMs = -1L;
    }

    public RideContext add(RideSample sample) {
        if (firstTimestampMs < 0L) {
            firstTimestampMs = sample.timestampMs;
            filteredAltitude = sample.altitudeMeters;
            significantAltitudeReference = filteredAltitude;
            lastSample = sample;
            history.addLast(new Node(sample, 0, 0, 0));
            return buildContext(sample, RideState.FLAT);
        }

        if (sample.timestampMs <= lastSample.timestampMs) {
            return buildContext(lastSample, RideState.FLAT);
        }

        filteredAltitude += ALTITUDE_SMOOTHING * (sample.altitudeMeters - filteredAltitude);
        double altitudeDelta = filteredAltitude - significantAltitudeReference;
        double distance = distanceMeters(lastSample.latitude, lastSample.longitude,
                sample.latitude, sample.longitude);
        double gain = altitudeDelta >= policy.minimumAltitudeChangeMeters ? altitudeDelta : 0.0d;
        double loss = altitudeDelta <= -policy.minimumAltitudeChangeMeters ? -altitudeDelta : 0.0d;
        if (gain > 0.0d || loss > 0.0d) significantAltitudeReference = filteredAltitude;
        totalDistanceMeters += distance;
        totalElevationGainMeters += gain;
        history.addLast(new Node(sample, distance, gain, loss, filteredAltitude));
        lastSample = sample;
        trimHistory(sample.timestampMs);
        updateContinuousPeriods(sample.timestampMs, gain, loss, sample.speedKmh);
        return buildContext(sample, RideState.FLAT);
    }

    private void updateContinuousPeriods(long timestampMs, double gain, double loss, double speedKmh) {
        if (speedKmh < policy.movingSpeedKmh) {
            if (stoppedSinceMs < 0L) stoppedSinceMs = timestampMs;
            climbSinceMs = -1L;
            descentSinceMs = -1L;
            return;
        } else {
            stoppedSinceMs = -1L;
        }
        if (gain > 0.0d && speedKmh >= policy.movingSpeedKmh) {
            if (climbSinceMs < 0L) climbSinceMs = timestampMs;
            lastClimbEvidenceMs = timestampMs;
            descentSinceMs = -1L;
        } else if (loss > 0.0d && speedKmh >= policy.movingSpeedKmh) {
            if (descentSinceMs < 0L) descentSinceMs = timestampMs;
            lastDescentEvidenceMs = timestampMs;
            climbSinceMs = -1L;
        } else if (speedKmh >= policy.movingSpeedKmh) {
            long graceMillis = policy.elevationTrendGraceSeconds * 1000L;
            if (climbSinceMs >= 0L && timestampMs - lastClimbEvidenceMs > graceMillis) climbSinceMs = -1L;
            if (descentSinceMs >= 0L && timestampMs - lastDescentEvidenceMs > graceMillis) descentSinceMs = -1L;
        }
    }

    private RideContext buildContext(RideSample sample, RideState state) {
        Window oneMinute = calculateWindow(sample.timestampMs, policy.windowSeconds[0]);
        Window fiveMinutes = calculateWindow(sample.timestampMs, policy.windowSeconds[1]);
        Window tenMinutes = calculateWindow(sample.timestampMs, policy.windowSeconds[2]);
        long activeMillis = Math.max(0L, sample.timestampMs - firstTimestampMs - manualPausedMillis);
        long stoppedSeconds = stoppedSinceMs < 0L ? 0L
                : Math.max(0L, (sample.timestampMs - stoppedSinceMs) / 1000L);
        long climbSeconds = climbSinceMs < 0L ? 0L
                : Math.max(0L, (sample.timestampMs - climbSinceMs) / 1000L);
        long descentSeconds = descentSinceMs < 0L ? 0L
                : Math.max(0L, (sample.timestampMs - descentSinceMs) / 1000L);
        double grade = oneMinute.distanceMeters < 5.0d ? 0.0d
                : (oneMinute.gainMeters - oneMinute.lossMeters) * 100.0d / oneMinute.distanceMeters;
        return new RideContext(sample.timestampMs, sample, oneMinute.averageSpeedKmh,
                fiveMinutes.averageSpeedKmh, tenMinutes.averageSpeedKmh, oneMinute.distanceMeters,
                oneMinute.gainMeters, fiveMinutes.gainMeters, tenMinutes.gainMeters,
                oneMinute.lossMeters, fiveMinutes.lossMeters, tenMinutes.lossMeters,
                climbSeconds, descentSeconds, stoppedSeconds, activeMillis / 1000L,
                totalDistanceMeters, totalElevationGainMeters, grade, state);
    }

    private Window calculateWindow(long nowMs, long seconds) {
        long cutoff = nowMs - seconds * 1000L;
        double speedTotal = 0.0d;
        int speedCount = 0;
        double distance = 0.0d;
        double gain = 0.0d;
        double loss = 0.0d;
        for (Node node : history) {
            if (node.sample.timestampMs < cutoff) continue;
            speedTotal += node.sample.speedKmh;
            speedCount++;
            distance += node.distanceMeters;
            gain += node.gainMeters;
            loss += node.lossMeters;
        }
        return new Window(speedCount == 0 ? 0.0d : speedTotal / speedCount, distance, gain, loss);
    }

    private void trimHistory(long nowMs) {
        long cutoff = nowMs - HISTORY_MILLIS;
        while (history.size() > 1 && history.getFirst().sample.timestampMs < cutoff) history.removeFirst();
    }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000.0d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadius * 2.0d * Math.atan2(Math.sqrt(a), Math.sqrt(1.0d - a));
    }

    private static final class Node {
        final RideSample sample;
        final double distanceMeters;
        final double gainMeters;
        final double lossMeters;
        final double filteredAltitude;

        Node(RideSample sample, double distanceMeters, double gainMeters, double lossMeters) {
            this(sample, distanceMeters, gainMeters, lossMeters, sample.altitudeMeters);
        }

        Node(RideSample sample, double distanceMeters, double gainMeters, double lossMeters,
             double filteredAltitude) {
            this.sample = sample;
            this.distanceMeters = distanceMeters;
            this.gainMeters = gainMeters;
            this.lossMeters = lossMeters;
            this.filteredAltitude = filteredAltitude;
        }
    }

    private static final class Window {
        final double averageSpeedKmh;
        final double distanceMeters;
        final double gainMeters;
        final double lossMeters;

        Window(double averageSpeedKmh, double distanceMeters, double gainMeters, double lossMeters) {
            this.averageSpeedKmh = averageSpeedKmh;
            this.distanceMeters = distanceMeters;
            this.gainMeters = gainMeters;
            this.lossMeters = lossMeters;
        }
    }
}
