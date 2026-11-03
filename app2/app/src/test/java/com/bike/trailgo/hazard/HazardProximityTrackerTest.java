package com.bike.trailgo.hazard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bike.trailgo.gps.GeoMath;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HazardProximityTrackerTest {

    private static final double LAT = 37.5;
    private static final double LON = 127.0;
    private static final double METERS_PER_DEGREE_LAT = 111_195.0;
    private static final double GOOD = 8.0;

    private HazardProximityTracker tracker;
    private HazardAlertConfig config;
    private long now;

    @Before
    public void setUp() {
        tracker = new HazardProximityTracker();
        config = HazardAlertConfig.defaults();
        now = 1_000_000L;
        tracker.setHazards(Collections.singletonList(hazard(1, 0, 30, HazardType.BAD_ROAD)));
    }

    /** A zone whose centre is {@code northMeters} north of the origin. */
    private static Hazard hazard(int id, double northMeters, int radius, HazardType type) {
        Hazard hazard = new Hazard(id, LAT + northMeters / METERS_PER_DEGREE_LAT, LON, radius, type, "제목" + id);
        hazard.verified = false;
        return hazard;
    }

    /** Fix at {@code d} metres north of the origin (so d - radius is the distance to the edge of a 0-centred zone). */
    private HazardProximityTracker.Update fix(double d, double accuracy, long advanceMs) {
        now += advanceMs;
        return tracker.evaluate(LAT + d / METERS_PER_DEGREE_LAT, LON, accuracy, now, config);
    }

    private double lastD = Double.NaN;

    /** A fix after a plausible ride (8 m/s at most) from the previous one, so the jump filter stays out of the way. */
    private HazardProximityTracker.Update fix(double d) {
        long travelMs = Double.isNaN(lastD) ? 1_000L : (long) (Math.abs(d - lastD) / 8.0 * 1_000L);
        lastD = d;
        return fix(d, GOOD, Math.max(1_000L, travelMs));
    }

    // ---- boundary geometry --------------------------------------------------------------------------

    @Test
    public void judgesByTheEdgeNotTheCentre() {
        // centre 120 m away, radius 30 m: the edge is 90 m away, inside the 100 m warning distance
        HazardProximityTracker.Update update = fix(120);
        assertEquals(1, update.alerts.size());
        assertEquals(HazardAlert.Kind.PRE_WARN, update.alerts.get(0).kind);
        assertEquals(90.0, update.alerts.get(0).edgeDistanceM, 1.0);
    }

    @Test
    public void noWarningOutsideTheWarningDistance() {
        assertTrue(fix(300).alerts.isEmpty());
        assertTrue(fix(131).alerts.isEmpty()); // edge 101 m
        assertEquals(1, fix(128).alerts.size());  // edge 98 m
    }

    @Test
    public void warningDistanceIsConfigurable() {
        config = new HazardAlertConfig(50);
        assertTrue(fix(120).alerts.isEmpty()); // edge 90 > 50
        assertTrue(fix(100).alerts.isEmpty()); // edge 70
        assertEquals(1, fix(78).alerts.size()); // edge 48
    }

    @Test
    public void entersWhenTheEdgeIsReached() {
        fix(200);
        fix(120);
        HazardProximityTracker.Update inside = fix(29);
        assertEquals(1, inside.alerts.size());
        assertEquals(HazardAlert.Kind.ENTER, inside.alerts.get(0).kind);
    }

    @Test
    public void startingInsideAZoneAnnouncesEntryOnce() {
        HazardProximityTracker.Update first = fix(10);
        assertEquals(1, first.alerts.size());
        assertEquals(HazardAlert.Kind.ENTER, first.alerts.get(0).kind);
        for (int i = 0; i < 60; i++) {
            assertTrue("standing still inside must stay quiet", fix(10).alerts.isEmpty());
        }
    }

    // ---- no repeated announcements --------------------------------------------------------------------

    @Test
    public void gpsJitterAroundTheBoundaryDoesNotRepeatAlerts() {
        fix(200);
        int alerts = 0;
        double[] wobble = {40, 28, 33, 26, 35, 29, 38, 27, 34, 25, 36, 28, 32, 24, 37, 29};
        for (double d : wobble) alerts += fix(d).alerts.size();
        // one pre-warning is possible on the way in, plus exactly one entry
        assertTrue("alerts=" + alerts, alerts <= 2);
        int entries = 0;
        tracker.reset();
        fix(200);
        for (double d : wobble) {
            for (HazardAlert alert : fix(d).alerts) if (alert.kind == HazardAlert.Kind.ENTER) entries++;
        }
        assertEquals(1, entries);
    }

    @Test
    public void preWarningIsGivenOncePerApproach() {
        assertEquals(1, fix(120).alerts.size());
        assertTrue(fix(110).alerts.isEmpty());
        assertTrue(fix(125).alerts.isEmpty()); // drifts back out a little
        assertTrue(fix(115).alerts.isEmpty());
        assertTrue(fix(160).alerts.isEmpty()); // still within warn + re-arm distance: not re-armed
        assertTrue(fix(125).alerts.isEmpty());
    }

    @Test
    public void reEntryWithinTheCooldownIsSilent() {
        fix(120);
        assertEquals(HazardAlert.Kind.ENTER, fix(25).alerts.get(0).kind);
        assertTrue(fix(50).alerts.isEmpty());   // edge 20 >= exit margin: left
        assertTrue(fix(25, GOOD, 30_000L).alerts.isEmpty()); // back in 30 s later: too soon to repeat
        assertTrue(fix(25).alerts.isEmpty());
        assertTrue(fix(50).alerts.isEmpty());
        // long after: announced again
        List<HazardAlert> again = fix(25, GOOD, 200_000L).alerts;
        assertEquals(1, again.size());
        assertEquals(HazardAlert.Kind.ENTER, again.get(0).kind);
    }

    @Test
    public void alertsAgainOnlyAfterGoingFarAway() {
        fix(120);
        fix(25);
        fix(50);
        assertTrue(fix(180).alerts.isEmpty()); // 150 m from the edge: still not far enough to re-arm (needs > 200 m)
        assertTrue(fix(125, GOOD, 300_000L).alerts.isEmpty());
        fix(400);                                // 370 m from the edge: re-armed
        List<HazardAlert> again = fix(125).alerts;
        assertEquals(1, again.size());
        assertEquals(HazardAlert.Kind.PRE_WARN, again.get(0).kind);
    }

    @Test
    public void resetAllowsEveryZoneToWarnAgain() {
        fix(120);
        fix(25);
        tracker.reset();
        assertEquals(HazardAlert.Kind.ENTER, fix(25).alerts.get(0).kind);
    }

    // ---- GPS quality ----------------------------------------------------------------------------------

    @Test
    public void fixesWithPoorOrUnknownAccuracyAreIgnored() {
        HazardProximityTracker.Update poor = fix(10, 80, 1_000L);
        assertFalse(poor.usable);
        assertTrue(poor.alerts.isEmpty());
        assertFalse(fix(10, Double.NaN, 1_000L).usable);
        assertFalse(fix(10, 0, 1_000L).usable);
        // nothing was learnt from them: a good fix still gets the full announcement
        assertEquals(HazardAlert.Kind.ENTER, fix(10).alerts.get(0).kind);
    }

    @Test
    public void entryIsNotAnnouncedFromAnInaccurateFix() {
        // 40 m accuracy: good enough to warn, not to claim "you are inside"
        HazardProximityTracker.Update vague = fix(20, 40, 1_000L);
        assertEquals(1, vague.alerts.size());
        assertEquals(HazardAlert.Kind.PRE_WARN, vague.alerts.get(0).kind);
        assertTrue(fix(20, 40, 1_000L).alerts.isEmpty());
        assertTrue(fix(15, 45, 1_000L).alerts.isEmpty());
        HazardProximityTracker.Update sure = fix(20, 10, 1_000L);
        assertEquals(HazardAlert.Kind.ENTER, sure.alerts.get(0).kind);
    }

    @Test
    public void aLoneGpsJumpIsIgnored() {
        fix(500);
        HazardProximityTracker.Update jump = fix(10, GOOD, 1_000L); // 490 m in one second: not a bicycle
        assertFalse(jump.usable);
        assertTrue(jump.alerts.isEmpty());
        assertTrue(fix(495, GOOD, 1_000L).alerts.isEmpty());
        assertNull(tracker.status(now, config));
    }

    @Test
    public void aConfirmedRelocationIsBelieved() {
        fix(500);
        assertFalse(fix(10, GOOD, 1_000L).usable);
        HazardProximityTracker.Update confirmed = fix(12, GOOD, 1_000L); // a second fix at the new place
        assertTrue(confirmed.usable);
        assertEquals(HazardAlert.Kind.ENTER, confirmed.alerts.get(0).kind);
    }

    @Test
    public void aLongGapIsNotAJump() {
        fix(500);
        // two minutes without fixes (tunnel): the next fix is simply the new position
        assertTrue(fix(130, GOOD, 120_000L).usable);
    }

    @Test
    public void statusExpiresWhenTheSignalIsLost() {
        fix(20);
        assertNotNull(tracker.status(now + 5_000L, config));
        assertNull(tracker.status(now + 11_000L, config));
        fix(20, GOOD, 20_000L);
        assertNotNull(tracker.status(now, config));
        // an unusable fix does not refresh the status
        fix(20, 90, 5_000L);
        assertNull(tracker.status(now + 6_000L, config));
    }

    // ---- the banner ---------------------------------------------------------------------------------------

    @Test
    public void statusShowsDistanceWhileApproachingAndInsideAfterEntering() {
        assertNull(tracker.status(now, config));
        fix(300);
        assertNull(tracker.status(now, config));
        fix(130 - 1); // edge 99
        HazardStatus approaching = tracker.status(now, config);
        assertFalse(approaching.inside);
        assertEquals(99.0, approaching.edgeDistanceM, 1.0);
        fix(60);
        assertEquals(30.0, tracker.status(now, config).edgeDistanceM, 1.0);
        fix(20);
        HazardStatus inside = tracker.status(now, config);
        assertTrue(inside.inside);
        assertEquals(0.0, inside.edgeDistanceM, 0.001);
    }

    @Test
    public void statusClearsAfterLeavingTheWarningArea() {
        fix(120);
        fix(25);
        fix(50);
        assertNotNull(tracker.status(now, config)); // just outside: still within warning distance
        fix(500);
        assertNull(tracker.status(now, config));
    }

    // ---- several zones at once ---------------------------------------------------------------------------

    @Test
    public void severalZonesGiveOneOrderedBatchAndTheNearestOnTheBanner() {
        tracker.setHazards(Arrays.asList(
                hazard(1, 0, 30, HazardType.BAD_ROAD),
                hazard(2, 60, 30, HazardType.TRAFFIC),     // the rider at d=100 is 10 m from its edge
                hazard(3, -20, 30, HazardType.SLIPPERY)));  // 90 m from its edge
        HazardProximityTracker.Update update = fix(100);
        assertEquals(3, update.alerts.size());
        // nearest first among pre-warnings: zone 2 (10 m), zone 1 (70 m), zone 3 (90 m)
        assertEquals(2, update.alerts.get(0).hazard.id);
        assertEquals(1, update.alerts.get(1).hazard.id);
        assertEquals(3, update.alerts.get(2).hazard.id);
        HazardStatus status = tracker.status(now, config);
        assertEquals(2, status.hazard.id);
        assertEquals(2, status.otherCount);
        // one sentence for the whole batch, so voices never overlap
        String speech = HazardMessages.speech(update.alerts);
        assertEquals("전방 약 10미터에 차량 주의 위험지역이 있습니다. 근처에 위험지역이 2곳 더 있습니다.", speech);
    }

    @Test
    public void entriesComeBeforePreWarnings() {
        tracker.setHazards(Arrays.asList(
                hazard(1, 0, 30, HazardType.BAD_ROAD),       // rider at d=20: inside
                hazard(2, 120, 30, HazardType.STEEP_SLOPE))); // edge 70 m
        HazardProximityTracker.Update update = fix(20);
        assertEquals(HazardAlert.Kind.ENTER, update.alerts.get(0).kind);
        assertEquals(1, update.alerts.get(0).hazard.id);
        assertEquals(HazardAlert.Kind.PRE_WARN, update.alerts.get(1).kind);
        assertTrue(tracker.status(now, config).inside);
    }

    // ---- updating the zone list during a ride -------------------------------------------------------------

    @Test
    public void replacingTheListKeepsProgressOfZonesThatRemain() {
        fix(120);
        Hazard sameZoneFreshObject = hazard(1, 0, 30, HazardType.BAD_ROAD);
        tracker.setHazards(Collections.singletonList(sameZoneFreshObject));
        assertTrue("a refresh from the server must not repeat the warning", fix(110).alerts.isEmpty());
        assertEquals(HazardAlert.Kind.ENTER, fix(20).alerts.get(0).kind);
    }

    @Test
    public void aRemovedZoneStopsWarningAndLeavesTheBanner() {
        fix(120);
        assertNotNull(tracker.status(now, config));
        tracker.setHazards(Collections.<Hazard>emptyList());
        assertNull(tracker.status(now, config));
        assertTrue(fix(20).alerts.isEmpty());
    }

    @Test
    public void invalidZonesAreSkippedWithoutBreakingTheOthers() {
        Hazard broken = new Hazard();
        broken.id = 9;
        broken.latitude = Double.NaN;
        broken.radiusM = 30;
        Hazard origin = new Hazard();
        origin.id = 10;
        origin.radiusM = 30; // 0,0 is "unset"
        List<Hazard> mixed = new ArrayList<>(Arrays.asList(broken, origin, null, hazard(1, 0, 30, HazardType.OTHER)));
        tracker.setHazards(mixed);
        assertEquals(1, tracker.hazardCount());
        assertEquals(1, fix(120).alerts.size());
    }

    @Test
    public void aZoneWithoutRadiusUsesTheDefault() {
        Hazard noRadius = hazard(1, 0, 0, HazardType.OTHER);
        tracker.setHazards(Collections.singletonList(noRadius));
        assertEquals(HazardAlert.Kind.ENTER, fix(25).alerts.get(0).kind); // default 30 m
    }

    @Test
    public void geometryHelperAgreesWithTheTestMapping() {
        assertEquals(100.0, GeoMath.distanceMeters(LAT, LON, LAT + 100 / METERS_PER_DEGREE_LAT, LON), 0.5);
    }
}
