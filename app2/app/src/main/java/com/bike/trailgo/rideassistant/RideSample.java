package com.bike.trailgo.rideassistant;

/** A location-derived sample in stable units, independent from Android framework classes. */
public final class RideSample {
    public final long timestampMs;
    public final double latitude;
    public final double longitude;
    public final double speedKmh;
    public final double altitudeMeters;
    public final double accuracyMeters;

    public RideSample(long timestampMs, double latitude, double longitude, double speedKmh,
                      double altitudeMeters, double accuracyMeters) {
        this.timestampMs = timestampMs;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speedKmh = Math.max(0.0d, speedKmh);
        this.altitudeMeters = altitudeMeters;
        this.accuracyMeters = accuracyMeters;
    }
}
