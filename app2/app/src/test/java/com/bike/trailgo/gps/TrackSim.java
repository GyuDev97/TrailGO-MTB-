package com.bike.trailgo.gps;

/** Test helper: builds fixes at metre offsets from a fixed origin (Daegu 앞산) on a synthetic clock. */
final class TrackSim {
    static final double LAT0 = 35.8329;
    static final double LON0 = 128.5650;
    static final long T0 = 1_000_000_000_000L;
    private static final double METERS_PER_DEG_LAT = 111_320.0;
    private static final double METERS_PER_DEG_LON = 111_320.0 * Math.cos(Math.toRadians(LAT0));

    private TrackSim() {
    }

    static long nanos(double seconds) {
        return T0 + (long) (seconds * 1e9);
    }

    /** A fix {@code east}/{@code north} metres from the origin at {@code seconds}. */
    static GpsFix fix(double seconds, double east, double north, double accuracy) {
        return new GpsFix(LAT0 + north / METERS_PER_DEG_LAT, LON0 + east / METERS_PER_DEG_LON,
                Double.NaN, accuracy, Double.NaN, Double.NaN, Double.NaN, nanos(seconds));
    }

    static GpsFix fix(double seconds, double east, double north, double accuracy, double speedMps, double speedAccuracy) {
        return new GpsFix(LAT0 + north / METERS_PER_DEG_LAT, LON0 + east / METERS_PER_DEG_LON,
                Double.NaN, accuracy, Double.NaN, speedMps, speedAccuracy, nanos(seconds));
    }

    /** A fix that carries an altitude (and optionally its vertical accuracy). */
    static GpsFix fixWithAltitude(double seconds, double east, double north, double accuracy,
                                  double altitude, double verticalAccuracy) {
        return new GpsFix(LAT0 + north / METERS_PER_DEG_LAT, LON0 + east / METERS_PER_DEG_LON,
                altitude, accuracy, verticalAccuracy, Double.NaN, Double.NaN, nanos(seconds));
    }

    static GpsFix altitudeOnly(double seconds, double altitude, double verticalAccuracy) {
        return fixWithAltitude(seconds, 0, 0, 5, altitude, verticalAccuracy);
    }
}
