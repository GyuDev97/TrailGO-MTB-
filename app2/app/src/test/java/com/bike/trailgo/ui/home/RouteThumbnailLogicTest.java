package com.bike.trailgo.ui.home;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.bike.trailgo.gpx.GpxTrackPoints;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Route projection, GPX reading for thumbnails and the on-disk picture cache. */
public class RouteThumbnailLogicTest {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private static List<double[]> line(int n) {
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            points.add(new double[]{35.83 + i * 0.0005, 128.56 + i * 0.001});
        }
        return points;
    }

    // ---- projection --------------------------------------------------------------------------

    @Test
    public void aRouteIsFittedInsideThePictureWithItsMargin() {
        RoutePath.Projected p = RoutePath.project(line(20), 200f, 150f, 20f);
        assertNotNull(p);
        assertEquals(20, p.size());
        assertFalse(p.singlePlace);
        for (int i = 0; i < p.size(); i++) {
            assertTrue(p.x[i] >= 20f - 0.01f && p.x[i] <= 180f + 0.01f);
            assertTrue(p.y[i] >= 20f - 0.01f && p.y[i] <= 130f + 0.01f);
        }
    }

    @Test
    public void northIsUpAndEastIsRight() {
        RoutePath.Projected p = RoutePath.project(line(10), 200f, 200f, 10f);
        assertTrue(p.x[9] > p.x[0]);   // later points are further east
        assertTrue(p.y[9] < p.y[0]);   // ... and further north, which is higher up
    }

    @Test
    public void theShapeKeepsItsProportionsInsteadOfFillingTheBox() {
        // a long thin east-west route in a square picture stays thin
        List<double[]> flat = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            flat.add(new double[]{35.83 + (i % 2) * 0.0001, 128.5 + i * 0.01});
        }
        RoutePath.Projected p = RoutePath.project(flat, 200f, 200f, 10f);
        float width = max(p.x) - min(p.x);
        float height = max(p.y) - min(p.y);
        assertTrue(width > 10 * height);
        // and it is centred vertically
        assertEquals(100f, (max(p.y) + min(p.y)) / 2f, 1f);
    }

    @Test
    public void aStraightVerticalRouteStillDraws() {
        List<double[]> vertical = Arrays.asList(new double[]{35.80, 128.5}, new double[]{35.81, 128.5}, new double[]{35.82, 128.5});
        RoutePath.Projected p = RoutePath.project(vertical, 100f, 100f, 10f);
        assertFalse(p.singlePlace);
        assertEquals(p.x[0], p.x[2], 0.001f);
        assertEquals(90f, p.y[0], 0.01f);
        assertEquals(10f, p.y[2], 0.01f);
    }

    @Test
    public void onePointOrAllPointsAtTheSamePlaceIsADotInTheMiddle() {
        RoutePath.Projected one = RoutePath.project(Arrays.asList(new double[]{35.8, 128.5}), 100f, 80f, 10f);
        assertTrue(one.singlePlace);
        assertEquals(50f, one.x[0], 0.001f);
        assertEquals(40f, one.y[0], 0.001f);
        RoutePath.Projected same = RoutePath.project(
                Arrays.asList(new double[]{35.8, 128.5}, new double[]{35.8, 128.5}, new double[]{35.8, 128.5}), 100f, 80f, 10f);
        assertTrue(same.singlePlace);
    }

    @Test
    public void twoPointsMakeALine() {
        RoutePath.Projected two = RoutePath.project(
                Arrays.asList(new double[]{35.80, 128.50}, new double[]{35.81, 128.52}), 100f, 100f, 10f);
        assertEquals(2, two.size());
        assertFalse(two.singlePlace);
    }

    @Test
    public void invalidPointsAreSkippedAndNothingValidMeansNoPicture() {
        List<double[]> mixed = Arrays.asList(
                new double[]{Double.NaN, 128.5}, new double[]{0, 0}, new double[]{95, 10}, null,
                new double[]{35.80, 128.50}, new double[]{35.81, 128.52}, new double[]{35.0});
        assertEquals(2, RoutePath.project(mixed, 100f, 100f, 10f).size());
        assertNull(RoutePath.project(Arrays.asList(new double[]{0, 0}, new double[]{Double.NaN, 1}), 100f, 100f, 10f));
        assertNull(RoutePath.project(new ArrayList<double[]>(), 100f, 100f, 10f));
        assertNull(RoutePath.project(null, 100f, 100f, 10f));
        assertNull(RoutePath.project(line(5), 0f, 100f, 10f));
    }

    @Test
    public void veryLongTracksAreThinnedButKeepTheirEnds() {
        List<double[]> points = line(5000);
        RoutePath.Projected p = RoutePath.project(points, 200f, 200f, 10f);
        assertEquals(RoutePath.MAX_POINTS, p.size());
        RoutePath.Projected full = RoutePath.project(line(RoutePath.MAX_POINTS), 200f, 200f, 10f);
        assertEquals(full.x[0], p.x[0], 0.5f);
        assertEquals(full.x[full.size() - 1], p.x[p.size() - 1], 0.5f);
    }

    private static float max(float[] values) {
        float m = -Float.MAX_VALUE;
        for (float v : values) m = Math.max(m, v);
        return m;
    }

    private static float min(float[] values) {
        float m = Float.MAX_VALUE;
        for (float v : values) m = Math.min(m, v);
        return m;
    }

    // ---- GPX reading -------------------------------------------------------------------------

    private static List<double[]> read(String gpx) throws Exception {
        return GpxTrackPoints.read(new ByteArrayInputStream(gpx.getBytes("UTF-8")), new KXmlParser());
    }

    @Test
    public void readsThePositionsOfANormalGpx() throws Exception {
        List<double[]> points = read("<gpx version=\"1.1\" creator=\"x\"><trk><trkseg>"
                + "<trkpt lat=\"35.8329\" lon=\"128.565\"><ele>100</ele></trkpt>"
                + "<trkpt lat=\"35.8339\" lon=\"128.566\"><ele>101</ele></trkpt>"
                + "</trkseg></trk></gpx>");
        assertEquals(2, points.size());
        assertArrayEquals(new double[]{35.8329, 128.565}, points.get(0), 0.0);
    }

    @Test
    public void gpxWrittenByTheAppWithoutElevationReadsToo() throws Exception {
        List<double[]> points = read("<gpx version=\"1.1\" creator=\"TrailGO\"><trk><trkseg>"
                + "<trkpt lat=\"35.8329\" lon=\"128.565\"/><trkpt lat=\"35.8339\" lon=\"128.566\"/></trkseg></trk></gpx>");
        assertEquals(2, points.size());
    }

    @Test
    public void pointsWithMissingOrBadCoordinatesAreSkipped() throws Exception {
        List<double[]> points = read("<gpx version=\"1.1\" creator=\"x\"><trk><trkseg>"
                + "<trkpt lat=\"35.8329\" lon=\"128.565\"/>"
                + "<trkpt lon=\"128.566\"/>"
                + "<trkpt lat=\"N/A\" lon=\"128.566\"/>"
                + "<trkpt lat=\"35.8349\" lon=\"128.567\"/>"
                + "</trkseg></trk></gpx>");
        assertEquals(2, points.size());
    }

    @Test
    public void aGpxWithoutPointsGivesAnEmptyListAndOnePointIsFine() throws Exception {
        assertTrue(read("<gpx version=\"1.1\" creator=\"x\"><trk><trkseg></trkseg></trk></gpx>").isEmpty());
        assertEquals(1, read("<gpx version=\"1.1\" creator=\"x\"><trk><trkseg><trkpt lat=\"35.8\" lon=\"128.5\"/></trkseg></trk></gpx>").size());
    }

    @Test
    public void aDamagedFileFailsCleanlyForTheCallerToHandle() throws Exception {
        try {
            read("<gpx version=\"1.1\"><trk><trkseg><trkpt lat=\"35.8\" lon=");
            fail("expected a parse failure");
        } catch (XmlPullParserException | java.io.IOException expected) {
            // the loader turns this into "no picture", not a crash
        }
        try {
            read("this is not xml at all");
            fail("expected a parse failure");
        } catch (XmlPullParserException | java.io.IOException expected) {
            // fine
        }
    }

    // ---- disk cache --------------------------------------------------------------------------

    private DiskThumbnailStore store(int files, long bytes, long ageMs) {
        return new DiskThumbnailStore(new File(temp.getRoot(), "thumbs"), files, bytes, ageMs);
    }

    @Test
    public void aStoredPictureComesBackAndAMissOnlyOnceNothingWasStored() throws Exception {
        DiskThumbnailStore store = store(10, 1_000_000, 100_000);
        String key = DiskThumbnailStore.key("gpx_a.gpx", 168, 128);
        assertNull(store.get(key, 1000));
        store.put(key, new byte[]{1, 2, 3}, 1000);
        assertArrayEquals(new byte[]{1, 2, 3}, store.get(key, 2000));
    }

    @Test
    public void tooOldPicturesAreDroppedWhenAsked() throws Exception {
        DiskThumbnailStore store = store(10, 1_000_000, 1000);
        String key = DiskThumbnailStore.key("gpx_a.gpx", 168, 128);
        store.put(key, new byte[]{9}, 5000);
        assertNull(store.get(key, 7000));
        assertEquals(0, store.count());
    }

    @Test
    public void theFileCountIsBoundedAndTheLeastRecentlyUsedGoFirst() throws Exception {
        DiskThumbnailStore store = store(3, 1_000_000, 1_000_000);
        for (int i = 0; i < 3; i++) {
            store.put(DiskThumbnailStore.key("gpx_" + i + ".gpx", 10, 10), new byte[]{(byte) i}, 1000 + i);
        }
        // use the oldest one again, then add a fourth
        assertNotNull(store.get(DiskThumbnailStore.key("gpx_0.gpx", 10, 10), 5000));
        store.put(DiskThumbnailStore.key("gpx_3.gpx", 10, 10), new byte[]{3}, 6000);
        assertEquals(3, store.count());
        assertNotNull(store.get(DiskThumbnailStore.key("gpx_0.gpx", 10, 10), 6001));
        assertNull(store.get(DiskThumbnailStore.key("gpx_1.gpx", 10, 10), 6001)); // evicted
    }

    @Test
    public void theTotalSizeIsBounded() throws Exception {
        DiskThumbnailStore store = store(100, 250, 1_000_000);
        for (int i = 0; i < 5; i++) {
            store.put(DiskThumbnailStore.key("gpx_" + i + ".gpx", 10, 10), new byte[100], 1000 + i);
        }
        assertTrue(store.count() <= 2);
    }

    @Test
    public void deletingARideRemovesAllItsPicturesAndNoOthers() throws Exception {
        DiskThumbnailStore store = store(10, 1_000_000, 1_000_000);
        store.put(DiskThumbnailStore.key("local:ride-3.gpx", 168, 128), new byte[]{1}, 1000);
        store.put(DiskThumbnailStore.key("local:ride-3.gpx", 252, 192), new byte[]{2}, 1000);
        store.put(DiskThumbnailStore.key("local:ride-4.gpx", 168, 128), new byte[]{3}, 1000);
        assertEquals(2, store.removeReference("local:ride-3.gpx"));
        assertEquals(1, store.count());
        assertNotNull(store.get(DiskThumbnailStore.key("local:ride-4.gpx", 168, 128), 2000));
    }

    @Test
    public void picturesOfRidesThatNoLongerExistCanBeSweptAway() throws Exception {
        DiskThumbnailStore store = store(10, 1_000_000, 1_000_000);
        store.put(DiskThumbnailStore.key("local:ride-1.gpx", 10, 10), new byte[]{1}, 1000);
        store.put(DiskThumbnailStore.key("local:ride-2.gpx", 10, 10), new byte[]{2}, 1000);
        store.put(DiskThumbnailStore.key("local:ride-9.gpx", 10, 10), new byte[]{9}, 1000);
        Set<String> alive = new HashSet<>(Arrays.asList("local:ride-2.gpx"));
        assertEquals(2, store.retainOnly(alive));
        assertEquals(1, store.count());
    }

    @Test
    public void keysAreStableDistinctAndSafeAsFileNames() {
        String a = DiskThumbnailStore.key("gpx_20261010_194900_123.gpx", 168, 128);
        assertEquals(a, DiskThumbnailStore.key("gpx_20261010_194900_123.gpx", 168, 128));
        assertNotEquals(a, DiskThumbnailStore.key("gpx_20261010_194900_124.gpx", 168, 128));
        assertNotEquals(a, DiskThumbnailStore.key("gpx_20261010_194900_123.gpx", 252, 192));
        assertTrue(a.matches("[0-9a-f]{12}_168x128"));
        // a hostile reference can not reach outside the folder: only the hash is used
        assertTrue(DiskThumbnailStore.key("../../etc/passwd", 1, 1).matches("[0-9a-f]{12}_1x1"));
    }

    @Test
    public void aBrokenKeyIsRefused() throws Exception {
        try {
            store(3, 100, 100).put("../escape", new byte[]{1}, 1);
            fail("expected a refusal");
        } catch (IllegalArgumentException expected) {
            // fine
        }
    }
}
