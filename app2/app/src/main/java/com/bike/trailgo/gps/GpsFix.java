package com.bike.trailgo.gps;

/**
 * Android-independent snapshot of one location fix. Unknown values are {@code NaN} (never 0), so
 * "no altitude" can not be confused with an altitude of 0 m.
 */
public final class GpsFix {
    public final double latitude;
    public final double longitude;
    public final double altitudeMeters;
    public final double horizontalAccuracyMeters;
    public final double verticalAccuracyMeters;
    public final double speedMps;
    public final double speedAccuracyMps;
    /** {@code Location.getElapsedRealtimeNanos()}; 0 when the source gave no timestamp. */
    public final long elapsedRealtimeNanos;

    public GpsFix(double latitude, double longitude, double altitudeMeters,
                  double horizontalAccuracyMeters, double verticalAccuracyMeters,
                  double speedMps, double speedAccuracyMps, long elapsedRealtimeNanos) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
        this.horizontalAccuracyMeters = horizontalAccuracyMeters;
        this.verticalAccuracyMeters = verticalAccuracyMeters;
        this.speedMps = speedMps;
        this.speedAccuracyMps = speedAccuracyMps;
        this.elapsedRealtimeNanos = elapsedRealtimeNanos;
    }

    public boolean hasAltitude() {
        return !Double.isNaN(altitudeMeters) && !Double.isInfinite(altitudeMeters);
    }

    public boolean hasHorizontalAccuracy() {
        return !Double.isNaN(horizontalAccuracyMeters) && horizontalAccuracyMeters > 0;
    }

    public boolean hasVerticalAccuracy() {
        return !Double.isNaN(verticalAccuracyMeters) && verticalAccuracyMeters > 0;
    }

    public boolean hasSpeed() {
        return !Double.isNaN(speedMps) && speedMps >= 0;
    }
}
