package com.bike.trailgo.ui.riding;

import android.content.Context;
import android.util.AtomicFile;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Small, private recovery checkpoint for an unfinished ride.
 *
 * <p>The foreground service keeps GPS delivery alive, but the legacy ride UI owns the
 * calculated distance and GPX points.  A checkpoint prevents a configuration change or a
 * process recreation from silently turning an in-progress ride into a new, empty one.</p>
 */
final class RideSessionStore {
    private static final String FILE_NAME = "unfinished-ride.bin";

    private RideSessionStore() {
    }

    static synchronized void save(Context context, Snapshot snapshot) {
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), FILE_NAME));
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            ObjectOutputStream stream = new ObjectOutputStream(output);
            stream.writeObject(snapshot);
            stream.flush();
            output.getFD().sync();
            file.finishWrite(output);
        } catch (IOException exception) {
            if (output != null) {
                file.failWrite(output);
            }
        }
    }

    @Nullable
    static synchronized Snapshot load(Context context) {
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), FILE_NAME));
        if (!file.getBaseFile().isFile()) {
            return null;
        }
        try (FileInputStream input = file.openRead();
             ObjectInputStream stream = new ObjectInputStream(input)) {
            Object value = stream.readObject();
            return value instanceof Snapshot ? (Snapshot) value : null;
        } catch (IOException | ClassNotFoundException exception) {
            clear(context);
            return null;
        }
    }

    static synchronized void clear(Context context) {
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), FILE_NAME));
        file.delete();
        RideWeatherFile.clear(context.getFilesDir());
    }

    static final class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        final boolean followMode;
        final int status;
        final long baseTime;
        final long pauseTime;
        final int restSeconds;
        final int activeSeconds;
        final double distanceMeters;
        final double maxSpeedKmh;
        final double averageSpeedKmh;
        final int elevationGainMeters;
        final int descentMeters;
        final double maxLat;
        final double maxLon;
        final double minLat;
        final double minLon;
        final double lastLatitude;
        final double lastLongitude;
        final double lastAltitude;
        final String address;
        final String addressDong;
        final ArrayList<Double> latitudes;
        final ArrayList<Double> longitudes;
        final ArrayList<Double> elevations;
        final ArrayList<Float> elevationSamples;
        final String courseGpxName;
        final String courseName;

        Snapshot(
                boolean followMode,
                int status,
                long baseTime,
                long pauseTime,
                int restSeconds,
                int activeSeconds,
                double distanceMeters,
                double maxSpeedKmh,
                double averageSpeedKmh,
                int elevationGainMeters,
                int descentMeters,
                double maxLat,
                double maxLon,
                double minLat,
                double minLon,
                double lastLatitude,
                double lastLongitude,
                double lastAltitude,
                String address,
                String addressDong,
                ArrayList<Double> latitudes,
                ArrayList<Double> longitudes,
                ArrayList<Double> elevations,
                ArrayList<Float> elevationSamples
        ) {
            this(followMode, status, baseTime, pauseTime, restSeconds, activeSeconds,
                    distanceMeters, maxSpeedKmh, averageSpeedKmh, elevationGainMeters,
                    descentMeters, maxLat, maxLon, minLat, minLon, lastLatitude, lastLongitude,
                    lastAltitude, address, addressDong, latitudes, longitudes, elevations,
                    elevationSamples, null, null);
        }

        Snapshot(
                boolean followMode,
                int status,
                long baseTime,
                long pauseTime,
                int restSeconds,
                int activeSeconds,
                double distanceMeters,
                double maxSpeedKmh,
                double averageSpeedKmh,
                int elevationGainMeters,
                int descentMeters,
                double maxLat,
                double maxLon,
                double minLat,
                double minLon,
                double lastLatitude,
                double lastLongitude,
                double lastAltitude,
                String address,
                String addressDong,
                ArrayList<Double> latitudes,
                ArrayList<Double> longitudes,
                ArrayList<Double> elevations,
                ArrayList<Float> elevationSamples,
                String courseGpxName,
                String courseName
        ) {
            this.followMode = followMode;
            this.status = status;
            this.baseTime = baseTime;
            this.pauseTime = pauseTime;
            this.restSeconds = restSeconds;
            this.activeSeconds = activeSeconds;
            this.distanceMeters = distanceMeters;
            this.maxSpeedKmh = maxSpeedKmh;
            this.averageSpeedKmh = averageSpeedKmh;
            this.elevationGainMeters = elevationGainMeters;
            this.descentMeters = descentMeters;
            this.maxLat = maxLat;
            this.maxLon = maxLon;
            this.minLat = minLat;
            this.minLon = minLon;
            this.lastLatitude = lastLatitude;
            this.lastLongitude = lastLongitude;
            this.lastAltitude = lastAltitude;
            this.address = address;
            this.addressDong = addressDong;
            this.latitudes = new ArrayList<>(latitudes);
            this.longitudes = new ArrayList<>(longitudes);
            this.elevations = new ArrayList<>(elevations);
            this.elevationSamples = new ArrayList<>(elevationSamples);
            this.courseGpxName = courseGpxName;
            this.courseName = courseName;
        }
    }
}
