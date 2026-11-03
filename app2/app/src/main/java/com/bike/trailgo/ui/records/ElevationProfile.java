package com.bike.trailgo.ui.records;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Elevation profile built from the track points of a recorded GPX file.
 *
 * <p>Meanings, kept apart on purpose:</p>
 * <ul>
 *   <li>{@code minElevation}/{@code maxElevation}: lowest/highest valid GPS altitude in the GPX
 *       ("해발" as reported by the device).</li>
 *   <li>{@code RidingRecord.rr_high}: accumulated climbing (획득 고도). Never used here.</li>
 * </ul>
 *
 * <p>The x axis is the cumulative horizontal distance along the track. The GPX written by this app
 * has no usable time axis, so none is invented. Points whose altitude is missing, non-finite,
 * implausible or exactly 0.0 (the app writes 0.0 when the phone gave no altitude) are skipped.</p>
 *
 * <p>Pure Java so it can be unit tested on the JVM.</p>
 */
public final class ElevationProfile {
    /** Fewer valid samples than this cannot draw a meaningful graph. */
    public static final int MIN_SAMPLES = 5;
    public static final double MIN_DISTANCE_METERS = 10.0;
    static final double MIN_PLAUSIBLE_METERS = -500.0;
    static final double MAX_PLAUSIBLE_METERS = 9000.0;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public static final class Sample {
        public final double distanceMeters;
        public final double elevationMeters;

        Sample(double distanceMeters, double elevationMeters) {
            this.distanceMeters = distanceMeters;
            this.elevationMeters = elevationMeters;
        }
    }

    private final List<Sample> samples;
    private final double minElevation;
    private final double maxElevation;
    private final double totalDistanceMeters;

    private ElevationProfile(List<Sample> samples, double totalDistanceMeters) {
        this.samples = Collections.unmodifiableList(samples);
        this.totalDistanceMeters = totalDistanceMeters;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Sample sample : samples) {
            min = Math.min(min, sample.elevationMeters);
            max = Math.max(max, sample.elevationMeters);
        }
        this.minElevation = min;
        this.maxElevation = max;
    }

    /**
     * True only when there is enough real altitude data, over a real distance, to show a graph and
     * min/max. A track that never moved would otherwise draw a meaningless vertical line.
     */
    public boolean isAvailable() {
        return getAvailability() == Availability.AVAILABLE;
    }

    /** Why a graph can or cannot be drawn; shared by the save screen and the record detail. */
    public enum Availability {
        AVAILABLE(null),
        NO_ELEVATION("이 기록에는 유효한 고도 정보가 없습니다"),
        TOO_FEW_SAMPLES("고도 샘플이 부족해 그래프를 표시하지 않습니다"),
        TOO_SHORT("이동 거리가 너무 짧아 고도 그래프를 표시하지 않습니다");

        public final String message;

        Availability(String message) {
            this.message = message;
        }
    }

    public Availability getAvailability() {
        if (samples.isEmpty()) {
            return Availability.NO_ELEVATION;
        }
        if (samples.size() < MIN_SAMPLES) {
            return Availability.TOO_FEW_SAMPLES;
        }
        if (totalDistanceMeters < MIN_DISTANCE_METERS) {
            return Availability.TOO_SHORT;
        }
        return Availability.AVAILABLE;
    }

    public List<Sample> getSamples() {
        return samples;
    }

    public double getMinElevation() {
        return minElevation;
    }

    public double getMaxElevation() {
        return maxElevation;
    }

    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public static boolean isValidElevation(@Nullable Double elevation) {
        return elevation != null
                && !elevation.isNaN()
                && !elevation.isInfinite()
                && elevation != 0.0
                && elevation >= MIN_PLAUSIBLE_METERS
                && elevation <= MAX_PLAUSIBLE_METERS;
    }

    public static final class Builder {
        private final List<Sample> samples = new ArrayList<>();
        private double distance;
        private boolean hasPrevious;
        private double previousLat;
        private double previousLon;

        /** Adds the next track point in file order; the elevation may be null. */
        public Builder add(double latitude, double longitude, @Nullable Double elevation) {
            if (hasPrevious) {
                distance += haversineMeters(previousLat, previousLon, latitude, longitude);
            }
            hasPrevious = true;
            previousLat = latitude;
            previousLon = longitude;
            if (isValidElevation(elevation)) {
                samples.add(new Sample(distance, elevation));
            }
            return this;
        }

        public ElevationProfile build() {
            return new ElevationProfile(new ArrayList<>(samples), distance);
        }
    }

    static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
