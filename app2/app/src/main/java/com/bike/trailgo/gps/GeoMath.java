package com.bike.trailgo.gps;

/** Small geodesy helpers that work on the JVM (android.location.Location is not available there). */
public final class GeoMath {
    private static final double EARTH_RADIUS_METERS = 6_371_008.8;

    private GeoMath() {
    }

    /** Great-circle distance; within ~0.5% of {@code Location.distanceBetween} at ride scales. */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
