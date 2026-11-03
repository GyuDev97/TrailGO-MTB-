package com.bike.trailgo.gps;

import static com.bike.trailgo.gps.TrackSim.altitudeOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

public class ElevationGainTrackerTest {

    private final ElevationGainTracker tracker = new ElevationGainTracker(GpsQualityConfig.DEFAULT);

    /** The algorithm this replaces: raw altitude, 3 m threshold, baseline moves on every step. */
    private static double legacyGain(double[] altitudes) {
        double gain = 0;
        double baseline = altitudes[0];
        for (int i = 1; i < altitudes.length; i++) {
            double change = altitudes[i] - baseline;
            if (change >= 3.0) {
                gain += Math.round(change);
                baseline = altitudes[i];
            } else if (change <= -3.0) {
                baseline = altitudes[i];
            }
        }
        return gain;
    }

    private double feedAll(double[] altitudes, double verticalAccuracy) {
        for (int s = 0; s < altitudes.length; s++) {
            tracker.onFix(altitudeOnly(s, altitudes[s], verticalAccuracy));
        }
        return tracker.getGainMeters();
    }

    private static double[] noisyConstant(int seconds, double altitude, double sigma, long seed) {
        Random random = new Random(seed);
        double[] values = new double[seconds];
        for (int i = 0; i < seconds; i++) {
            values[i] = altitude + random.nextGaussian() * sigma;
        }
        return values;
    }

    private static double[] ramp(int seconds, double start, double metersPerSecond, double sigma, long seed) {
        Random random = new Random(seed);
        double[] values = new double[seconds];
        for (int i = 0; i < seconds; i++) {
            values[i] = start + metersPerSecond * i + random.nextGaussian() * sigma;
        }
        return values;
    }

    @Test
    public void theFirstUsableAltitudeIsOnlyABaseline() {
        assertEquals(ElevationGainTracker.Status.BASELINE, tracker.onFix(altitudeOnly(0, 142.0, Double.NaN)));
        assertEquals(0.0, tracker.getGainMeters(), 0.0);
        assertEquals(0.0, tracker.getDescentMeters(), 0.0);
    }

    @Test
    public void missingAltitudeIsSkippedAndNeverBecomesZeroMetres() {
        assertEquals(ElevationGainTracker.Status.SKIPPED_NO_ALTITUDE, tracker.onFix(altitudeOnly(0, Double.NaN, Double.NaN)));
        tracker.onFix(altitudeOnly(1, 120.0, Double.NaN));
        // a missing sample between real ones must not look like a drop to 0 m and back up
        tracker.onFix(altitudeOnly(2, Double.NaN, Double.NaN));
        tracker.onFix(altitudeOnly(3, 120.5, Double.NaN));
        assertEquals(0.0, tracker.getGainMeters(), 0.0);
        assertEquals(0.0, tracker.getDescentMeters(), 0.0);
        assertEquals(120.5, tracker.getSmoothedAltitude(), 1.0);
    }

    @Test
    public void aRealAltitudeOfZeroIsValidAndDistinctFromMissing() {
        assertTrue(tracker.isUsable(altitudeOnly(0, 0.0, Double.NaN)));
        assertFalse(tracker.isUsable(altitudeOnly(0, Double.NaN, Double.NaN)));
        assertFalse(tracker.isUsable(altitudeOnly(0, 12000.0, Double.NaN)));
    }

    @Test
    public void poorVerticalAccuracyIsNotUsed() {
        assertFalse(tracker.isUsable(altitudeOnly(0, 100.0, 80.0)));
        assertEquals(ElevationGainTracker.Status.SKIPPED_POOR_ACCURACY, tracker.onFix(altitudeOnly(0, 100.0, 80.0)));
        assertTrue(tracker.isUsable(altitudeOnly(0, 100.0, 8.0)));
    }

    @Test
    public void altitudeNoiseWhileStayingAtTheSameLevelBarelyCountsAsClimbing() {
        double[] altitudes = noisyConstant(600, 100.0, 5.0, 11);
        double legacy = legacyGain(altitudes);
        double gain = feedAll(altitudes, Double.NaN);

        assertTrue("legacy algorithm would report " + legacy, legacy > 150);
        assertTrue("gain " + gain, gain < 20);
    }

    @Test
    public void noiseWithGoodReportedAccuracyIsSmoothedToo() {
        double[] altitudes = noisyConstant(600, 250.0, 3.0, 5);
        double gain = feedAll(altitudes, 3.0);
        assertTrue("gain " + gain + " (legacy " + legacyGain(altitudes) + ")", gain < 15);
    }

    @Test
    public void aSteadyClimbIsFollowedNotClipped() {
        double[] altitudes = ramp(300, 100.0, 0.7, 3.0, 3); // 210 m at 0.7 m/s
        double gain = feedAll(altitudes, 3.0);
        assertEquals(210.0, gain, 210 * 0.08);
    }

    @Test
    public void aVerySteepMountainClimbStillCountsFully() {
        // 25% grade at 6 km/h (1.67 m/s) is ~0.4 m/s; a brutal 30% at 3 m/s is 0.9 m/s; use 1.5 m/s.
        double[] altitudes = ramp(120, 400.0, 1.5, 3.0, 9); // 180 m
        double gain = feedAll(altitudes, 3.0);
        assertEquals(180.0, gain, 180 * 0.08);
    }

    @Test
    public void climbThenDescentSeparatesGainFromDescent() {
        double[] up = ramp(120, 100.0, 0.8, 2.0, 21);
        double[] down = ramp(120, 100.0 + 0.8 * 119, -1.2, 2.0, 22);
        for (int s = 0; s < up.length; s++) {
            tracker.onFix(altitudeOnly(s, up[s], 3.0));
        }
        for (int s = 0; s < down.length; s++) {
            tracker.onFix(altitudeOnly(up.length + s, down[s], 3.0));
        }
        assertEquals(95.2, tracker.getGainMeters(), 95.2 * 0.1);
        assertEquals(142.8, tracker.getDescentMeters(), 142.8 * 0.1);
    }

    @Test
    public void aSingleWildAltitudeSampleIsIgnored() {
        for (int s = 0; s < 20; s++) {
            tracker.onFix(altitudeOnly(s, 100.0, 4.0));
        }
        // the 18 m jump from the field report: one bad sample, then back to normal
        assertEquals(ElevationGainTracker.Status.OUTLIER, tracker.onFix(altitudeOnly(20, 118.0, 4.0)));
        for (int s = 21; s < 40; s++) {
            tracker.onFix(altitudeOnly(s, 100.0, 4.0));
        }
        assertTrue("gain " + tracker.getGainMeters(), tracker.getGainMeters() < 1.0);
    }

    @Test
    public void aPersistentLevelShiftIsAdoptedWithoutCountingItAsClimbing() {
        for (int s = 0; s < 20; s++) {
            tracker.onFix(altitudeOnly(s, 100.0, 4.0));
        }
        ElevationGainTracker.Status last = null;
        for (int s = 20; s < 40; s++) {
            last = tracker.onFix(altitudeOnly(s, 140.0, 4.0)); // e.g. the provider switched altitude source
        }
        assertEquals(ElevationGainTracker.Status.UPDATED, last);
        assertEquals(0.0, tracker.getGainMeters(), 1.0);
        assertEquals(140.0, tracker.getSmoothedAltitude(), 1.0);
    }

    @Test
    public void resumingAfterAPauseDoesNotCountTheAltitudeChangeDuringThePause() {
        for (int s = 0; s < 30; s++) {
            tracker.onFix(altitudeOnly(s, 100.0, 4.0));
        }
        double before = tracker.getGainMeters();
        tracker.resetBaseline();
        for (int s = 1000; s < 1030; s++) {
            tracker.onFix(altitudeOnly(s, 160.0, 4.0)); // carried up by car/lift while paused
        }
        assertEquals(before, tracker.getGainMeters(), 0.01);
    }

    @Test
    public void thresholdsAreConfigurable() {
        ElevationGainTracker sensitive = new ElevationGainTracker(
                GpsQualityConfig.builder().minGainThresholdMeters(1.0).build());
        ElevationGainTracker normal = new ElevationGainTracker(GpsQualityConfig.DEFAULT);
        double[] altitudes = ramp(200, 100.0, 0.05, 0.2, 4); // 10 m, essentially noise-free
        for (int s = 0; s < altitudes.length; s++) {
            sensitive.onFix(altitudeOnly(s, altitudes[s], 3.0));
            normal.onFix(altitudeOnly(s, altitudes[s], 3.0));
        }
        assertTrue(sensitive.getGainMeters() >= normal.getGainMeters());
    }
}
