package com.bike.trailgo.gps;

import static com.bike.trailgo.gps.TrackSim.fix;
import static com.bike.trailgo.gps.TrackSim.nanos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

public class GpsFixFilterTest {

    private final GpsFixFilter filter = new GpsFixFilter(GpsQualityConfig.DEFAULT);

    /** Evaluates a fix as if it was delivered the moment it was measured. */
    private GpsFixFilter.Result feed(GpsFixFilter f, GpsFix fix) {
        return f.evaluate(fix, fix.elapsedRealtimeNanos);
    }

    private static final class Totals {
        double distance;
        int recorded;
        double maxGapSeconds;
        private double lastRecordedAt = Double.NaN;
    }

    private Totals ride(GpsFixFilter f, Iterable<GpsFix> fixes) {
        Totals totals = new Totals();
        for (GpsFix fix : fixes) {
            GpsFixFilter.Result result = feed(f, fix);
            if (result.isRecorded()) {
                totals.recorded++;
                totals.distance += result.distanceMeters;
                double at = (fix.elapsedRealtimeNanos - TrackSim.T0) / 1e9;
                if (!Double.isNaN(totals.lastRecordedAt)) {
                    totals.maxGapSeconds = Math.max(totals.maxGapSeconds, at - totals.lastRecordedAt);
                }
                totals.lastRecordedAt = at;
            }
        }
        return totals;
    }

    private static java.util.List<GpsFix> straightRide(double speedMps, int seconds, double accuracy) {
        java.util.List<GpsFix> fixes = new java.util.ArrayList<>();
        for (int s = 0; s <= seconds; s++) {
            fixes.add(fix(s, speedMps * s, 0, accuracy));
        }
        return fixes;
    }

    // ---- cached / stale / invalid -------------------------------------------------------------

    @Test
    public void cachedLastKnownLocationIsNotTheFirstRecordedPoint() {
        // FusedLocation getLastLocation(): measured 5 minutes ago, delivered "now".
        GpsFix cached = fix(0, 500, 500, 10);
        GpsFixFilter.Result stale = filter.evaluate(cached, nanos(300));
        assertEquals(GpsFixFilter.Verdict.REJECTED_STALE, stale.verdict);
        assertFalse(filter.hasRecordedPoint());

        GpsFixFilter.Result first = filter.evaluate(fix(300, 0, 0, 6), nanos(300));
        assertEquals(GpsFixFilter.Verdict.RECORDED_FIRST, first.verdict);
        assertEquals(0.0, first.distanceMeters, 0.0);
    }

    @Test
    public void aFreshLastKnownLocationIsFine() {
        GpsFixFilter.Result result = filter.evaluate(fix(0, 0, 0, 8), nanos(3));
        assertEquals(GpsFixFilter.Verdict.RECORDED_FIRST, result.verdict);
    }

    @Test
    public void invalidCoordinatesAndOutOfOrderFixesAreRejected() {
        assertEquals(GpsFixFilter.Verdict.REJECTED_INVALID,
                feed(filter, new GpsFix(0.0, 0.0, Double.NaN, 5, Double.NaN, Double.NaN, Double.NaN, nanos(1))).verdict);
        assertEquals(GpsFixFilter.Verdict.REJECTED_INVALID,
                feed(filter, new GpsFix(Double.NaN, 128, Double.NaN, 5, Double.NaN, Double.NaN, Double.NaN, nanos(1))).verdict);
        assertEquals(GpsFixFilter.Verdict.RECORDED_FIRST, feed(filter, fix(10, 0, 0, 5)).verdict);
        assertEquals(GpsFixFilter.Verdict.REJECTED_OUT_OF_ORDER, feed(filter, fix(9, 20, 0, 5)).verdict);
        assertEquals(GpsFixFilter.Verdict.REJECTED_OUT_OF_ORDER, feed(filter, fix(10, 20, 0, 5)).verdict);
    }

    // ---- stationary jitter --------------------------------------------------------------------

    private java.util.List<GpsFix> jitter(int seconds, double accuracy, boolean withDoppler, long seed) {
        Random random = new Random(seed);
        double sigma = accuracy / 1.51; // accuracy is the 68% radius of a 2-D Rayleigh error
        java.util.List<GpsFix> fixes = new java.util.ArrayList<>();
        for (int s = 0; s <= seconds; s++) {
            double east = random.nextGaussian() * sigma;
            double north = random.nextGaussian() * sigma;
            fixes.add(withDoppler ? fix(s, east, north, accuracy, 0.0, 0.3) : fix(s, east, north, accuracy));
        }
        return fixes;
    }

    @Test
    public void stationaryGpsWobbleDoesNotBuildUpDistance() {
        java.util.List<GpsFix> wobble = jitter(300, 10, false, 42);
        double naive = 0;
        for (int i = 1; i < wobble.size(); i++) {
            naive += GeoMath.distanceMeters(wobble.get(i - 1).latitude, wobble.get(i - 1).longitude,
                    wobble.get(i).latitude, wobble.get(i).longitude);
        }
        Totals totals = ride(filter, wobble);

        assertTrue("an unfiltered sum would be " + naive, naive > 1500);
        assertTrue("filtered distance " + totals.distance + " vs unfiltered " + naive, totals.distance < naive * 0.15);
        assertTrue("recorded points " + totals.recorded, totals.recorded < 10);
    }

    @Test
    public void dopplerSpeedZeroKeepsEvenLargeWobbleAtZeroDistance() {
        Totals totals = ride(filter, jitter(300, 10, true, 7));
        assertEquals(0.0, totals.distance, 0.0);
        assertEquals(1, totals.recorded); // just the first point
    }

    @Test
    public void tinyMovementWhileStandingStillIsNotDistance() {
        // The reported case: 1 m of drift in 8 s with good accuracy must stay ~0.
        GpsFixFilter f = new GpsFixFilter(GpsQualityConfig.DEFAULT);
        double[][] drift = {{0, 0}, {0.3, 0.2}, {0.6, 0.1}, {0.9, 0.4}, {1.0, 0.5}, {0.8, 0.9}, {0.5, 1.0}, {0.2, 0.6}, {0.0, 0.3}};
        double total = 0;
        for (int s = 0; s < drift.length; s++) {
            total += feed(f, fix(s, drift[s][0], drift[s][1], 4)).distanceMeters;
        }
        assertEquals(0.0, total, 0.0);
    }

    // ---- real movement is preserved -----------------------------------------------------------

    @Test
    public void normalFlatRideKeepsAlmostAllOfTheDistance() {
        Totals totals = ride(filter, straightRide(5.0, 120, 5)); // 600 m at 18 km/h
        assertTrue("distance " + totals.distance, totals.distance >= 600 * 0.96);
        assertTrue(totals.distance <= 600.5);
        assertTrue("max gap " + totals.maxGapSeconds, totals.maxGapSeconds <= 5);
    }

    @Test
    public void slowSteepMountainClimbStaysContinuous() {
        // 1.5 m/s uphill with mediocre accuracy under tree cover
        Totals totals = ride(filter, straightRide(1.5, 600, 8)); // 900 m
        assertTrue("distance " + totals.distance, totals.distance >= 900 * 0.95);
        assertTrue("max gap " + totals.maxGapSeconds, totals.maxGapSeconds <= 20);
    }

    @Test
    public void walkingPaceAccumulatesThroughTheNoiseRadius() {
        // 0.8 m/s: no single step is larger than the radius, yet the movement must not vanish.
        Totals totals = ride(filter, straightRide(0.8, 300, 5)); // 240 m
        assertTrue("distance " + totals.distance, totals.distance >= 240 * 0.88);
    }

    @Test
    public void fastDescentIsNotTreatedAsAJump() {
        Totals totals = ride(filter, straightRide(20.0, 60, 6)); // 72 km/h, 1200 m
        assertTrue("distance " + totals.distance, totals.distance >= 1200 * 0.98);
    }

    // ---- jumps --------------------------------------------------------------------------------

    @Test
    public void aSingleWildPositionJumpIsNotCountedAndDoesNotBreakTheTrack() {
        Totals before = ride(filter, straightRide(5.0, 30, 5));
        GpsFixFilter.Result jump = feed(filter, fix(31, 150 + 800, 0, 5));
        assertEquals(GpsFixFilter.Verdict.REJECTED_JUMP, jump.verdict);
        assertTrue(jump.impliedSpeedMps > GpsQualityConfig.DEFAULT.maxSpeedMps);

        GpsFixFilter.Result next = feed(filter, fix(32, 175, 0, 5));
        assertEquals(GpsFixFilter.Verdict.RECORDED_MOVE, next.verdict);
        assertTrue("step " + next.distanceMeters, next.distanceMeters > 14 && next.distanceMeters < 40); // from the last good point, never from the outlier
        assertTrue(before.distance + next.distanceMeters < 190);
    }

    @Test
    public void aConfirmedNewPositionIsAdoptedWithoutCountingTheGap() {
        ride(filter, straightRide(5.0, 20, 5));
        assertEquals(GpsFixFilter.Verdict.REJECTED_JUMP, feed(filter, fix(21, 5000, 0, 5)).verdict);
        assertEquals(GpsFixFilter.Verdict.REJECTED_JUMP, feed(filter, fix(22, 5004, 1, 5)).verdict);
        GpsFixFilter.Result resync = feed(filter, fix(23, 5002, 0, 5));
        assertEquals(GpsFixFilter.Verdict.RECORDED_RESYNC, resync.verdict);
        assertEquals(0.0, resync.distanceMeters, 0.0);

        GpsFixFilter.Result after = feed(filter, fix(27, 5030, 0, 5));
        assertEquals(GpsFixFilter.Verdict.RECORDED_MOVE, after.verdict);
        assertEquals(28.0, after.distanceMeters, 3.0);
    }

    @Test
    public void speedLimitIsConfigurable() {
        GpsFixFilter strict = new GpsFixFilter(GpsQualityConfig.builder().maxSpeedMps(10.0).build());
        feed(strict, fix(0, 0, 0, 5));
        assertEquals(GpsFixFilter.Verdict.REJECTED_JUMP, feed(strict, fix(1, 40, 0, 5)).verdict); // 40 m/s

        GpsFixFilter lenient = new GpsFixFilter(GpsQualityConfig.builder().maxSpeedMps(60.0).build());
        feed(lenient, fix(0, 0, 0, 5));
        assertEquals(GpsFixFilter.Verdict.RECORDED_MOVE, feed(lenient, fix(1, 40, 0, 5)).verdict);
    }
    // ---- accuracy -----------------------------------------------------------------------------

    @Test
    public void poorAccuracyAloneDoesNotDiscardTheRide() {
        feed(filter, fix(0, 0, 0, 5));
        // 60 m accuracy right after a good point: wait for something better ...
        assertEquals(GpsFixFilter.Verdict.DEFERRED_POOR_ACCURACY, feed(filter, fix(3, 60, 0, 60)).verdict);
        // ... but if nothing better shows up within the patience window it is recorded, flagged.
        GpsFixFilter.Result late = feed(filter, fix(20, 100, 0, 60));
        assertEquals(GpsFixFilter.Verdict.RECORDED_MOVE, late.verdict);
        assertTrue(late.lowQuality);
    }

    @Test
    public void aBetterFixArrivingInTimeIsPreferredOverPoorOnes() {
        feed(filter, fix(0, 0, 0, 5));
        assertEquals(GpsFixFilter.Verdict.DEFERRED_POOR_ACCURACY, feed(filter, fix(1, 8, 0, 70)).verdict);
        GpsFixFilter.Result good = feed(filter, fix(2, 30, 0, 5));
        assertEquals(GpsFixFilter.Verdict.RECORDED_MOVE, good.verdict);
        assertFalse(good.lowQuality);
    }

    @Test
    public void networkQualityFixesAreRejectedOutright() {
        assertEquals(GpsFixFilter.Verdict.REJECTED_POOR_ACCURACY, feed(filter, fix(0, 0, 0, 800)).verdict);
        assertFalse(filter.hasRecordedPoint());
    }

    @Test
    public void indoorStartWaitsForAFixThenStartsAnywayAfterThePatience() {
        assertEquals(GpsFixFilter.Verdict.WAITING_FIRST_FIX, feed(filter, fix(0, 0, 0, 80)).verdict);
        assertEquals(GpsFixFilter.Verdict.WAITING_FIRST_FIX, feed(filter, fix(5, 3, 0, 75)).verdict);
        GpsFixFilter.Result started = feed(filter, fix(16, 4, 0, 70));
        assertEquals(GpsFixFilter.Verdict.RECORDED_FIRST, started.verdict);
        assertTrue(started.lowQuality);
    }

    @Test
    public void indoorStartBecomesNormalAsSoonAsAGoodFixArrives() {
        assertEquals(GpsFixFilter.Verdict.WAITING_FIRST_FIX, feed(filter, fix(0, 0, 0, 80)).verdict);
        GpsFixFilter.Result started = feed(filter, fix(4, 12, 3, 6));
        assertEquals(GpsFixFilter.Verdict.RECORDED_FIRST, started.verdict);
        assertFalse(started.lowQuality);
    }

    // ---- signal loss, background, pause -------------------------------------------------------

    @Test
    public void signalLossAndRecoveryKeepsTheTrackAndCountsTheCoveredDistance() {
        Totals first = ride(filter, straightRide(5.0, 60, 5)); // 300 m
        // no fixes for 60 s (tunnel), rider kept going at ~5 m/s
        GpsFixFilter.Result back = feed(filter, fix(120, 600, 0, 8));
        assertEquals(GpsFixFilter.Verdict.RECORDED_MOVE, back.verdict);
        assertEquals(300.0, back.distanceMeters, 1.0);
        assertTrue(first.distance + back.distanceMeters <= 601);
    }

    @Test
    public void backlogOfOldFixesAfterReturningFromBackgroundIsDropped() {
        feed(filter, fix(0, 0, 0, 5));
        long now = nanos(200);
        // the OS flushes fixes measured while the screen was off
        assertEquals(GpsFixFilter.Verdict.REJECTED_STALE, filter.evaluate(fix(100, 400, 0, 5), now).verdict);
        assertEquals(GpsFixFilter.Verdict.REJECTED_STALE, filter.evaluate(fix(150, 600, 0, 5), now).verdict);
        // the first fresh fix continues the track from the last recorded point
        GpsFixFilter.Result fresh = filter.evaluate(fix(199, 900, 0, 5), now);
        assertEquals(GpsFixFilter.Verdict.RECORDED_MOVE, fresh.verdict);
    }

    @Test
    public void afterAPauseTheNextFixStartsANewSegmentWithoutDistance() {
        ride(filter, straightRide(5.0, 20, 5));
        filter.reset();
        GpsFixFilter.Result resumed = feed(filter, fix(300, 2000, 0, 5)); // rider carried the bike elsewhere
        assertEquals(GpsFixFilter.Verdict.RECORDED_FIRST, resumed.verdict);
        assertEquals(0.0, resumed.distanceMeters, 0.0);
    }

    @Test
    public void everyDecisionIsCountedForTheDebugSummary() {
        feed(filter, fix(0, 0, 0, 5));
        feed(filter, fix(1, 0.5, 0, 5));
        filter.evaluate(fix(2, 50, 0, 5), nanos(100));
        String summary = filter.summary();
        assertTrue(summary, summary.contains("RECORDED_FIRST=1"));
        assertTrue(summary, summary.contains("IGNORED_STATIONARY=1"));
        assertTrue(summary, summary.contains("REJECTED_STALE=1"));
    }
}
