package com.bike.trailgo.ui.riding;

import android.location.Location;
import android.os.Build;

import com.bike.trailgo.gps.GpsFix;

/** Converts an Android {@link Location} to the platform-independent {@link GpsFix}. */
final class LocationFixes {
    private LocationFixes() {
    }

    static GpsFix from(Location location) {
        double altitude = location.hasAltitude() ? location.getAltitude() : Double.NaN;
        double horizontalAccuracy = location.hasAccuracy() ? location.getAccuracy() : Double.NaN;
        double verticalAccuracy = Double.NaN;
        double speedAccuracy = Double.NaN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (location.hasVerticalAccuracy()) {
                verticalAccuracy = location.getVerticalAccuracyMeters();
            }
            if (location.hasSpeedAccuracy()) {
                speedAccuracy = location.getSpeedAccuracyMetersPerSecond();
            }
        }
        double speed = location.hasSpeed() ? location.getSpeed() : Double.NaN;
        return new GpsFix(location.getLatitude(), location.getLongitude(), altitude,
                horizontalAccuracy, verticalAccuracy, speed, speedAccuracy,
                location.getElapsedRealtimeNanos());
    }
}
