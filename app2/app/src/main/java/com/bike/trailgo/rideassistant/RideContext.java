package com.bike.trailgo.rideassistant;

/** Immutable rolling summary sent to detectors and, only on events, to GYUVIS. */
public final class RideContext {
    public final long timestampMs;
    public final double latitude;
    public final double longitude;
    public final double currentSpeedKmh;
    public final double currentAltitudeMeters;
    public final double avgSpeed1mKmh;
    public final double avgSpeed5mKmh;
    public final double avgSpeed10mKmh;
    public final double distance1mMeters;
    public final double elevationGain1mMeters;
    public final double elevationGain5mMeters;
    public final double elevationGain10mMeters;
    public final double elevationLoss1mMeters;
    public final double elevationLoss5mMeters;
    public final double elevationLoss10mMeters;
    public final long continuousClimbSeconds;
    public final long continuousDescentSeconds;
    public final long stoppedSeconds;
    public final long rideDurationSeconds;
    public final double rideDistanceMeters;
    public final double totalElevationGainMeters;
    public final double estimatedGradePercent;
    public final RideState currentRideState;

    RideContext(long timestampMs, RideSample sample, double avgSpeed1mKmh, double avgSpeed5mKmh,
                double avgSpeed10mKmh, double distance1mMeters, double elevationGain1mMeters,
                double elevationGain5mMeters, double elevationGain10mMeters,
                double elevationLoss1mMeters, double elevationLoss5mMeters,
                double elevationLoss10mMeters, long continuousClimbSeconds,
                long continuousDescentSeconds, long stoppedSeconds, long rideDurationSeconds,
                double rideDistanceMeters, double totalElevationGainMeters,
                double estimatedGradePercent, RideState currentRideState) {
        this.timestampMs = timestampMs;
        this.latitude = sample.latitude;
        this.longitude = sample.longitude;
        this.currentSpeedKmh = sample.speedKmh;
        this.currentAltitudeMeters = sample.altitudeMeters;
        this.avgSpeed1mKmh = avgSpeed1mKmh;
        this.avgSpeed5mKmh = avgSpeed5mKmh;
        this.avgSpeed10mKmh = avgSpeed10mKmh;
        this.distance1mMeters = distance1mMeters;
        this.elevationGain1mMeters = elevationGain1mMeters;
        this.elevationGain5mMeters = elevationGain5mMeters;
        this.elevationGain10mMeters = elevationGain10mMeters;
        this.elevationLoss1mMeters = elevationLoss1mMeters;
        this.elevationLoss5mMeters = elevationLoss5mMeters;
        this.elevationLoss10mMeters = elevationLoss10mMeters;
        this.continuousClimbSeconds = continuousClimbSeconds;
        this.continuousDescentSeconds = continuousDescentSeconds;
        this.stoppedSeconds = stoppedSeconds;
        this.rideDurationSeconds = rideDurationSeconds;
        this.rideDistanceMeters = rideDistanceMeters;
        this.totalElevationGainMeters = totalElevationGainMeters;
        this.estimatedGradePercent = estimatedGradePercent;
        this.currentRideState = currentRideState;
    }

    RideContext withState(RideState state) {
        return new RideContext(timestampMs, new RideSample(timestampMs, latitude, longitude,
                currentSpeedKmh, currentAltitudeMeters, 0), avgSpeed1mKmh, avgSpeed5mKmh,
                avgSpeed10mKmh, distance1mMeters, elevationGain1mMeters, elevationGain5mMeters,
                elevationGain10mMeters, elevationLoss1mMeters, elevationLoss5mMeters,
                elevationLoss10mMeters, continuousClimbSeconds, continuousDescentSeconds,
                stoppedSeconds, rideDurationSeconds, rideDistanceMeters, totalElevationGainMeters,
                estimatedGradePercent, state);
    }
}
