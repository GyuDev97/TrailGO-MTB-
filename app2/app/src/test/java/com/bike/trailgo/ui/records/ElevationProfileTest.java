package com.bike.trailgo.ui.records;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ElevationProfileTest {
    // ~11 m per 0.0001 degree of latitude.
    private static ElevationProfile.Builder walk(int points, Double... elevations) {
        ElevationProfile.Builder builder = new ElevationProfile.Builder();
        for (int i = 0; i < points; i++) {
            Double elevation = i < elevations.length ? elevations[i] : null;
            builder.add(35.0 + i * 0.0005, 128.0, elevation);
        }
        return builder;
    }

    @Test
    public void minAndMaxComeFromGpxAltitudeNotFromAccumulatedClimb() {
        ElevationProfile profile = walk(6, 120.0, 140.0, 180.0, 160.0, 130.0, 125.0).build();
        assertTrue(profile.isAvailable());
        assertEquals(120.0, profile.getMinElevation(), 0.0);
        assertEquals(180.0, profile.getMaxElevation(), 0.0);
        assertTrue(profile.getTotalDistanceMeters() > 200.0);
    }

    @Test
    public void xAxisIsCumulativeDistanceAndNondecreasing() {
        ElevationProfile profile = walk(6, 100.0, 101.0, 102.0, 103.0, 104.0, 105.0).build();
        double previous = -1;
        for (ElevationProfile.Sample sample : profile.getSamples()) {
            assertTrue(sample.distanceMeters >= previous);
            previous = sample.distanceMeters;
        }
        assertEquals(0.0, profile.getSamples().get(0).distanceMeters, 0.0);
    }

    @Test
    public void missingAltitudeIsNotAvailable() {
        assertFalse(walk(10).build().isAvailable());
    }

    @Test
    public void zeroAltitudeWrittenForMissingGpsAltitudeIsIgnored() {
        ElevationProfile profile = walk(6, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0).build();
        assertEquals(0, profile.getSamples().size());
        assertFalse(profile.isAvailable());
    }

    @Test
    public void invalidAndImplausibleValuesAreDropped() {
        ElevationProfile profile = walk(7, 100.0, Double.NaN, Double.POSITIVE_INFINITY,
                20000.0, -9000.0, 105.0, 110.0).build();
        assertEquals(3, profile.getSamples().size());
        assertFalse(profile.isAvailable());
    }

    @Test
    public void tooFewSamplesAreNotAvailable() {
        assertFalse(walk(4, 100.0, 101.0, 102.0, 103.0).build().isAvailable());
    }

    @Test
    public void trackThatNeverMovedIsNotAvailable() {
        ElevationProfile.Builder builder = new ElevationProfile.Builder();
        for (int i = 0; i < 10; i++) {
            builder.add(35.0, 128.0, 100.0 + i);
        }
        assertFalse(builder.build().isAvailable());
    }

    @Test
    public void emptyTrackIsNotAvailable() {
        assertFalse(new ElevationProfile.Builder().build().isAvailable());
    }

    @Test
    public void unavailableProfilesSayWhyAndTheMessageIsSharedByBothScreens() {
        assertEquals(ElevationProfile.Availability.NO_ELEVATION, walk(10).build().getAvailability());
        assertEquals(ElevationProfile.Availability.TOO_FEW_SAMPLES,
                walk(4, 100.0, 101.0, 102.0, 103.0).build().getAvailability());
        ElevationProfile.Builder still = new ElevationProfile.Builder();
        for (int i = 0; i < 8; i++) {
            still.add(35.0, 128.0, 100.0 + i);
        }
        assertEquals(ElevationProfile.Availability.TOO_SHORT, still.build().getAvailability());
        assertEquals(ElevationProfile.Availability.AVAILABLE,
                walk(6, 120.0, 140.0, 180.0, 160.0, 130.0, 125.0).build().getAvailability());
        assertTrue(ElevationProfile.Availability.TOO_SHORT.message.length() > 0);
        assertFalse(ElevationProfile.Availability.AVAILABLE.message != null);
    }
}
