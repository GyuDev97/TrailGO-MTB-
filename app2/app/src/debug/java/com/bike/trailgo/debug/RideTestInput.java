package com.bike.trailgo.debug;

import android.location.Location;

/** Timestamped test input shared by scenario simulation and future recorded-ride replay. */
final class RideTestInput {
    enum Type { LOCATION, PAUSE, RESUME }

    final Type type;
    final long timestampMs;
    final double latitude;
    final double longitude;
    final double altitudeMeters;
    final double speedKmh;
    final float accuracyMeters;

    private RideTestInput(Type type, long timestampMs, double latitude, double longitude,
                          double altitudeMeters, double speedKmh, float accuracyMeters) {
        this.type = type;
        this.timestampMs = timestampMs;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
        this.speedKmh = speedKmh;
        this.accuracyMeters = accuracyMeters;
    }

    static RideTestInput location(long timestampMs, double latitude, double longitude,
                                  double altitudeMeters, double speedKmh) {
        return new RideTestInput(Type.LOCATION, timestampMs, latitude, longitude,
                altitudeMeters, speedKmh, 5.0f);
    }

    static RideTestInput command(Type type, long timestampMs) {
        return new RideTestInput(type, timestampMs, 0.0d, 0.0d, 0.0d, 0.0d, 0.0f);
    }

    Location toLocation() {
        Location location = new Location("ride-simulator");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAltitude(altitudeMeters);
        location.setSpeed((float) (speedKmh / 3.6d));
        location.setAccuracy(accuracyMeters);
        location.setTime(timestampMs);
        location.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());
        return location;
    }
}
