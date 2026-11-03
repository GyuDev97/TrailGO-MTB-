package com.bike.trailgo.ui.riding;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Picks the position that represents where a ride started, to be sent with the saved record so the
 * server can attach the weather at that place. Pure Java so it can be unit tested on the JVM.
 */
public final class RideStartPoint {
    public final double latitude;
    public final double longitude;

    private RideStartPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** @return the first usable recorded position, or null when there is none. */
    @Nullable
    public static RideStartPoint firstValid(@Nullable List<Double> latitudes, @Nullable List<Double> longitudes) {
        if (latitudes == null || longitudes == null) {
            return null;
        }
        int count = Math.min(latitudes.size(), longitudes.size());
        for (int index = 0; index < count; index++) {
            Double lat = latitudes.get(index);
            Double lon = longitudes.get(index);
            if (isUsable(lat, lon)) {
                return new RideStartPoint(lat, lon);
            }
        }
        return null;
    }

    static boolean isUsable(@Nullable Double lat, @Nullable Double lon) {
        if (lat == null || lon == null || lat.isNaN() || lon.isNaN() || lat.isInfinite() || lon.isInfinite()) {
            return false;
        }
        if (Math.abs(lat) > 90.0 || Math.abs(lon) > 180.0) {
            return false;
        }
        // (0, 0) is what an unset location looks like, not a place anyone rides.
        return !(Math.abs(lat) < 1e-6 && Math.abs(lon) < 1e-6);
    }
}
