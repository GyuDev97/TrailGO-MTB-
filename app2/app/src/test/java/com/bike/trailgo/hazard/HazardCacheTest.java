package com.bike.trailgo.hazard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HazardCacheTest {

    private static final double LAT = 37.5;
    private static final double LON = 127.0;
    private static final double METERS_PER_DEGREE_LAT = 111_195.0;
    private static final long DAY = 24L * 60L * 60L * 1000L;

    private static final class MemoryStorage implements HazardCache.Storage {
        String json;
        boolean failWrites;
        boolean failReads;

        @Override
        public String read() {
            if (failReads) throw new IllegalStateException("unreadable");
            return json;
        }

        @Override
        public void write(String value) {
            if (failWrites) throw new IllegalStateException("full");
            json = value;
        }
    }

    private MemoryStorage storage;
    private HazardCache cache;

    @Before
    public void setUp() {
        storage = new MemoryStorage();
        cache = HazardCache.create(storage);
    }

    private static Hazard hazard(int id, double northMeters) {
        return new Hazard(id, LAT + northMeters / METERS_PER_DEGREE_LAT, LON, 30, HazardType.BAD_ROAD, "z" + id);
    }

    @Test
    public void whatWasMergedSurvivesARestart() {
        cache.merge(LAT, LON, 2000, Arrays.asList(hazard(1, 100), hazard(2, 500)), true, 1_000L);
        HazardCache restarted = HazardCache.create(storage);
        restarted.load(2_000L);
        assertEquals(2, restarted.snapshot().size());
    }

    @Test
    public void offlineLoadGivesTheLastKnownZones() {
        cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(1, 100)), true, 1_000L);
        HazardCache offline = HazardCache.create(storage);
        offline.load(5_000L);
        assertFalse(offline.isEmpty());
        assertEquals(1, offline.snapshot().get(0).id);
    }

    @Test
    public void aCompleteAnswerRemovesZonesThatWereHidden() {
        cache.merge(LAT, LON, 2000, Arrays.asList(hazard(1, 100), hazard(2, 500)), true, 1_000L);
        List<Hazard> now = cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(2, 500)), true, 2_000L);
        assertEquals(1, now.size());
        assertEquals(2, now.get(0).id);
    }

    @Test
    public void aTruncatedAnswerRemovesNothing() {
        cache.merge(LAT, LON, 2000, Arrays.asList(hazard(1, 100), hazard(2, 500)), true, 1_000L);
        List<Hazard> now = cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(2, 500)), false, 2_000L);
        assertEquals(2, now.size());
    }

    @Test
    public void zonesOutsideTheQueriedAreaAreKept() {
        cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(1, 100)), true, 1_000L);
        // a query centred 20 km away says nothing about zone 1
        double farLat = LAT + 20_000 / METERS_PER_DEGREE_LAT;
        cache.merge(farLat, LON, 2000, Collections.<Hazard>emptyList(), true, 2_000L);
        assertEquals(1, cache.snapshot().size());
    }

    @Test
    public void zonesNearTheEdgeOfTheQueriedCircleAreNotDropped() {
        // 1,980 m away: inside the circle but within the safety margin of its edge
        cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(1, 1_980)), true, 1_000L);
        cache.merge(LAT, LON, 2000, Collections.<Hazard>emptyList(), true, 2_000L);
        assertEquals(1, cache.snapshot().size());
    }

    @Test
    public void refreshedZonesReplaceOlderCopies() {
        cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(1, 100)), true, 1_000L);
        Hazard changed = hazard(1, 100);
        changed.title = "수정됨";
        changed.verified = true;
        cache.merge(LAT, LON, 2000, Collections.singletonList(changed), true, 2_000L);
        assertEquals(1, cache.snapshot().size());
        assertEquals("수정됨", cache.snapshot().get(0).title);
        assertTrue(cache.snapshot().get(0).verified);
    }

    @Test
    public void oldZonesAreDroppedOnLoad() {
        cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(1, 100)), true, 1_000L);
        HazardCache later = HazardCache.create(storage);
        later.load(1_000L + 8 * DAY);
        assertTrue(later.isEmpty());
    }

    @Test
    public void theNumberOfZonesIsCappedKeepingTheNearest() {
        HazardCache small = new HazardCache(storage, 7 * DAY, 3);
        List<Hazard> many = new ArrayList<>();
        for (int i = 1; i <= 6; i++) many.add(hazard(i, i * 100));
        small.merge(LAT, LON, 2000, many, true, 1_000L);
        assertEquals(3, small.snapshot().size());
        for (Hazard kept : small.snapshot()) assertTrue(kept.id <= 3);
    }

    @Test
    public void corruptStorageStartsEmpty() {
        storage.json = "{not json";
        cache.load(1_000L);
        assertTrue(cache.isEmpty());
        storage.json = "[{\"id\":1,\"latitude\":\"x\"}]";
        cache.load(1_000L);
        assertTrue(cache.isEmpty());
    }

    @Test
    public void storageFailuresNeverBreakTheCache() {
        storage.failWrites = true;
        List<Hazard> now = cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(1, 100)), true, 1_000L);
        assertEquals(1, now.size());
        storage.failReads = true;
        cache.load(1_000L);
        assertTrue(cache.isEmpty());
    }

    @Test
    public void invalidZonesFromTheServerAreNotCached() {
        Hazard bad = new Hazard();
        bad.id = 5;
        bad.latitude = 200;
        bad.longitude = 0;
        cache.merge(LAT, LON, 2000, Arrays.asList(bad, hazard(1, 100)), true, 1_000L);
        assertEquals(1, cache.snapshot().size());
    }

    @Test
    public void aWithdrawnReportIsRemovedAtOnce() {
        cache.merge(LAT, LON, 2000, Arrays.asList(hazard(1, 100), hazard(2, 200)), true, 1_000L);
        cache.remove(1);
        HazardCache restarted = HazardCache.create(storage);
        restarted.load(2_000L);
        assertEquals(1, restarted.snapshot().size());
        assertNotNull(restarted.snapshot().get(0));
        assertEquals(2, restarted.snapshot().get(0).id);
    }

    // ---- merged / hidden zones reach phones that already stored them ------------------------------------

    @Test
    public void aMergedOriginalIsDroppedByTheServersRemovedList() {
        // the phone knows the original (id 1) and the representative (id 2) 20 m apart
        cache.merge(LAT, LON, 2000, Arrays.asList(hazard(1, 100), hazard(2, 120)), true, 1_000L);
        // an operator merged 1 into 2: the next answer lists only 2 and names 1 as removed
        List<Hazard> now = cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(2, 120)), true, 2_000L,
                Collections.singletonList(1));
        assertEquals(1, now.size());
        assertEquals(2, now.get(0).id);
        HazardCache restarted = HazardCache.create(storage);
        restarted.load(3_000L);
        assertEquals(1, restarted.snapshot().size());
    }

    @Test
    public void removalWorksEvenWhenTheAnswerIsTruncatedOrTheZoneIsNearTheEdge() {
        // 1,990 m away: the "absent from a complete answer" rule would keep it (inside the safety margin)
        cache.merge(LAT, LON, 2000, Arrays.asList(hazard(1, 1_990), hazard(2, 100)), true, 1_000L);
        List<Hazard> now = cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(2, 100)), false, 2_000L,
                Collections.singletonList(1));
        assertEquals(1, now.size());
        assertEquals(2, now.get(0).id);
    }

    @Test
    public void aRemovedZoneNeverComesBackFromTheSameAnswer() {
        List<Hazard> now = cache.merge(LAT, LON, 2000, Arrays.asList(hazard(1, 100), hazard(2, 120)), true, 1_000L,
                Collections.singletonList(1));
        assertEquals(1, now.size());
        assertEquals(2, now.get(0).id);
    }

    @Test
    public void aMergedOriginalAndItsRepresentativeDoNotWarnTwice() {
        cache.merge(LAT, LON, 2000, Arrays.asList(hazard(1, 0), hazard(2, 20)), true, 1_000L);
        HazardProximityTracker before = new HazardProximityTracker();
        before.setHazards(cache.snapshot());
        assertEquals("both zones warn while the phone still has the stale original", 2,
                before.evaluate(LAT + 120 / METERS_PER_DEGREE_LAT, LON, 8.0, 5_000L, HazardAlertConfig.defaults()).alerts.size());

        cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(2, 20)), true, 2_000L, Collections.singletonList(1));
        HazardProximityTracker after = new HazardProximityTracker();
        after.setHazards(cache.snapshot());
        assertEquals("after the sync only the representative warns", 1,
                after.evaluate(LAT + 120 / METERS_PER_DEGREE_LAT, LON, 8.0, 5_000L, HazardAlertConfig.defaults()).alerts.size());
    }

    @Test
    public void aRemovalMidRideKeepsTheAlreadyAnnouncedRepresentativeQuiet() {
        cache.merge(LAT, LON, 2000, Arrays.asList(hazard(1, 0), hazard(2, 20)), true, 1_000L);
        HazardProximityTracker tracker = new HazardProximityTracker();
        tracker.setHazards(cache.snapshot());
        HazardAlertConfig config = HazardAlertConfig.defaults();
        double d = LAT + 120 / METERS_PER_DEGREE_LAT;
        assertEquals(2, tracker.evaluate(d, LON, 8.0, 5_000L, config).alerts.size());

        cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(2, 20)), true, 6_000L, Collections.singletonList(1));
        tracker.setHazards(cache.snapshot());
        assertTrue("the representative was already announced, not repeated",
                tracker.evaluate(d, LON, 8.0, 7_000L, config).alerts.isEmpty());
        assertEquals(2, tracker.status(7_000L, config).hazard.id);
    }

    @Test
    public void cachedZonesDriveTheTrackerWithoutAnyNetwork() {
        cache.merge(LAT, LON, 2000, Collections.singletonList(hazard(1, 0)), true, 1_000L);
        HazardCache offline = HazardCache.create(storage);
        offline.load(2_000L);
        HazardProximityTracker tracker = new HazardProximityTracker();
        tracker.setHazards(offline.snapshot());
        HazardProximityTracker.Update update = tracker.evaluate(LAT + 120 / METERS_PER_DEGREE_LAT, LON, 8.0, 3_000L,
                HazardAlertConfig.defaults());
        assertEquals(1, update.alerts.size());
    }
}
