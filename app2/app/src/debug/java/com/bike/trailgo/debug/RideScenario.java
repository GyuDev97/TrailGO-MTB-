package com.bike.trailgo.debug;

import java.util.ArrayList;
import java.util.List;

/** Deterministic ride inputs. Events are never inserted here; the production detectors create them. */
enum RideScenario {
    FLAT_NORMAL("평지"),
    CLIMB_NORMAL("일반 업힐"),
    LONG_CLIMB("장시간 업힐"),
    LONG_CLIMB_WITH_PACE_DROP("업힐 + 페이스 저하"),
    DESCENT_NORMAL("다운힐"),
    FAST_DESCENT("빠른 다운힐"),
    STOP_AND_RESUME("정차 → 재출발");

    private static final long STEP_MS = 30_000L;
    private static final double START_LATITUDE = 37.5665d;
    private static final double START_LONGITUDE = 126.9780d;

    final String label;

    RideScenario(String label) {
        this.label = label;
    }

    static RideScenario fromLabel(String label) {
        for (RideScenario scenario : values()) if (scenario.label.equals(label)) return scenario;
        return FLAT_NORMAL;
    }

    List<RideTestInput> create(long startTimestampMs) {
        switch (this) {
            case CLIMB_NORMAL:
                return slope(startTimestampMs, 13, 10.0d, 5.0d);
            case LONG_CLIMB:
                return slope(startTimestampMs, 25, 9.0d, 5.0d);
            case LONG_CLIMB_WITH_PACE_DROP:
                return longClimbWithPaceDrop(startTimestampMs);
            case DESCENT_NORMAL:
                return slope(startTimestampMs, 13, 20.0d, -5.0d);
            case FAST_DESCENT:
                return slope(startTimestampMs, 13, 42.0d, -6.0d);
            case STOP_AND_RESUME:
                return stopAndResume(startTimestampMs);
            case FLAT_NORMAL:
            default:
                return slope(startTimestampMs, 13, 18.0d, 0.0d);
        }
    }

    private static List<RideTestInput> longClimbWithPaceDrop(long startTimestampMs) {
        List<RideTestInput> inputs = new ArrayList<>();
        double latitude = START_LATITUDE;
        double longitude = START_LONGITUDE;
        double altitude = 100.0d;
        for (int index = 0; index < 26; index++) {
            // Keep pace steady until LONG_CLIMB is reached, then create a sustained slowdown.
            double speed = index <= 20 ? 9.0d : Math.max(4.0d, 9.0d - (index - 20));
            inputs.add(RideTestInput.location(startTimestampMs + index * STEP_MS,
                    latitude, longitude, altitude, speed));
            longitude = moveLongitude(longitude, latitude, speed, STEP_MS);
            altitude += 5.0d;
        }
        return inputs;
    }

    private static List<RideTestInput> slope(long startTimestampMs, int sampleCount,
                                              double speedKmh, double altitudeStepMeters) {
        List<RideTestInput> inputs = new ArrayList<>();
        double latitude = START_LATITUDE;
        double longitude = START_LONGITUDE;
        double altitude = 100.0d;
        for (int index = 0; index < sampleCount; index++) {
            inputs.add(RideTestInput.location(startTimestampMs + index * STEP_MS,
                    latitude, longitude, altitude, speedKmh));
            longitude = moveLongitude(longitude, latitude, speedKmh, STEP_MS);
            altitude += altitudeStepMeters;
        }
        return inputs;
    }

    private static List<RideTestInput> stopAndResume(long startTimestampMs) {
        List<RideTestInput> inputs = slope(startTimestampMs, 5, 16.0d, 0.0d);
        long pauseAt = startTimestampMs + 5L * STEP_MS;
        inputs.add(RideTestInput.command(RideTestInput.Type.PAUSE, pauseAt));
        inputs.add(RideTestInput.command(RideTestInput.Type.RESUME, pauseAt + 120_000L));
        double longitude = moveLongitude(START_LONGITUDE, START_LATITUDE, 16.0d, STEP_MS * 4L);
        for (int index = 0; index < 5; index++) {
            long timestamp = pauseAt + 120_000L + index * STEP_MS;
            inputs.add(RideTestInput.location(timestamp, START_LATITUDE, longitude, 100.0d, 16.0d));
            longitude = moveLongitude(longitude, START_LATITUDE, 16.0d, STEP_MS);
        }
        return inputs;
    }

    private static double moveLongitude(double longitude, double latitude, double speedKmh, long elapsedMs) {
        double distanceMeters = speedKmh / 3.6d * elapsedMs / 1000.0d;
        return longitude + distanceMeters / (111_320.0d * Math.cos(Math.toRadians(latitude)));
    }
}
