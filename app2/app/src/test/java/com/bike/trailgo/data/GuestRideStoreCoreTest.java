package com.bike.trailgo.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.bike.trailgo.dto.RecordPhoto;
import com.bike.trailgo.dto.RecordWeather;
import com.bike.trailgo.dto.RidingRecord;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GuestRideStoreCoreTest {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private File directory;
    private GuestRideStoreCore store;
    private int nextId = -1;

    @Before
    public void setUp() {
        directory = new File(temp.getRoot(), "guest-rides");
        store = new GuestRideStoreCore(directory, new GuestRideStoreCore.IdSource() {
            @Override
            public int nextLocalId() {
                return nextId--;
            }
        });
    }

    private File gpx(String name) throws IOException {
        File file = temp.newFile(name);
        java.nio.file.Files.write(file.toPath(), "<gpx/>".getBytes("UTF-8"));
        return file;
    }

    private File photo(String name, int size) throws IOException {
        File file = temp.newFile(name);
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) 7);
        java.nio.file.Files.write(file.toPath(), bytes);
        return file;
    }

    private RidingRecord saveRide(String gpxName, String memo, RecordWeather weather, List<File> photos) throws IOException {
        return store.save("주말 라이딩", 12450, 28, 14, 365, 180, 3120, "대구", gpx(gpxName), memo, weather, photos);
    }

    private static RecordWeather weather(boolean available) {
        return new Gson().fromJson(available
                ? "{\"available\":true,\"observedAt\":\"2026-10-10T18:00:00+09:00\",\"temperatureC\":21.0,"
                + "\"sky\":\"CLEAR\",\"precipitation\":\"NONE\",\"label\":\"맑음\"}"
                : "{\"available\":false}", RecordWeather.class);
    }

    private File[] photoFiles() {
        File[] files = new File(directory, "photos").listFiles();
        return files == null ? new File[0] : files;
    }

    // ---- saving ------------------------------------------------------------------------------

    @Test
    public void savesMemoWeatherAndPhotosTogetherWithTheRide() throws Exception {
        RidingRecord saved = saveRide("a.gpx", "하산 구간 조심", weather(true),
                Arrays.asList(photo("p1.jpg", 100), photo("p2.jpg", 200)));

        assertEquals("guest", saved.getRr_rider());
        assertEquals(0, saved.getRr_open());
        assertEquals("하산 구간 조심", saved.getRr_memo());
        assertEquals(2, saved.getPhotos().size());
        assertEquals(21.0, saved.getWeather().getTemperatureC(), 0.0);
        assertEquals(2, photoFiles().length);

        RidingRecord loaded = store.load().get(0);
        assertEquals("하산 구간 조심", loaded.getRr_memo());
        assertEquals(2, loaded.getPhotos().size());
        assertEquals("맑음", loaded.getWeather().getLabel());
        assertEquals(100, store.photoFile(loaded.getPhotos().get(0).getFile()).length());
    }

    @Test
    public void aRideWithoutMemoPhotosOrWeatherStaysEmptyNotFake() throws Exception {
        RidingRecord saved = saveRide("b.gpx", "   ", weather(false), null);
        assertNull(saved.getRr_memo());
        assertTrue(saved.getPhotos().isEmpty());
        assertNull(saved.getWeather());
        assertEquals(0, photoFiles().length);
    }

    @Test
    public void aPhotoThatVanishedBeforeSavingDoesNotBlockTheRide() throws Exception {
        RidingRecord saved = saveRide("c.gpx", null, null,
                Arrays.asList(new File(temp.getRoot(), "gone.jpg"), photo("ok.jpg", 10)));
        assertEquals(1, saved.getPhotos().size());
    }

    @Test
    public void atMostTenPhotosAreKept() throws Exception {
        List<File> many = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            many.add(photo("m" + i + ".jpg", 5));
        }
        assertEquals(10, saveRide("d.gpx", null, null, many).getPhotos().size());
        assertEquals(10, photoFiles().length);
    }

    @Test
    public void retryingTheSameSaveDoesNotDuplicateTheRide() throws Exception {
        File source = gpx("e.gpx");
        RidingRecord first = store.save("r", 1, 1, 1, 1, 1, 1, "", source, null, null, null);
        RidingRecord second = store.save("r", 1, 1, 1, 1, 1, 1, "", source, null, null, null);
        assertEquals(first.getRr_num(), second.getRr_num());
        assertEquals(1, store.load().size());
    }

    @Test
    public void aFailedIndexWriteLeavesNoOrphanGpxOrPhotos() throws Exception {
        // records.json is a non-empty directory, so the index can neither be replaced nor deleted
        File blocker = new File(directory, "records.json");
        assertTrue(new File(blocker, "x").getParentFile().mkdirs());
        assertTrue(new File(blocker, "x").createNewFile());

        try {
            saveRide("f.gpx", "메모", null, Collections.singletonList(photo("p.jpg", 10)));
            fail("expected the save to fail");
        } catch (IOException expected) {
            // fine
        }
        assertEquals(0, photoFiles().length);
        File[] gpxFiles = directory.listFiles((dir, name) -> name.endsWith(".gpx"));
        assertEquals(0, gpxFiles == null ? 0 : gpxFiles.length);
    }

    // ---- compatibility -----------------------------------------------------------------------

    @Test
    public void indexWrittenByAnOlderVersionLoadsWithEmptyNewFields() throws Exception {
        assertTrue(directory.mkdirs());
        String legacy = "[{\"rr_num\":-3,\"rr_rider\":\"guest\",\"rr_date\":\"2026-09-01 10:00:00\","
                + "\"rr_distance\":5000,\"rr_topspeed\":30,\"rr_avgspeed\":15,\"rr_high\":120,"
                + "\"rr_gpx\":\"local:ride-3.gpx\",\"rr_open\":0,\"rr_breaktime\":60,\"rr_time\":1200,"
                + "\"rr_area\":\"대구\",\"rr_name\":\"옛 기록\",\"local_source_id\":\"old.gpx\"}]";
        java.nio.file.Files.write(new File(directory, "records.json").toPath(), legacy.getBytes("UTF-8"));
        java.nio.file.Files.write(new File(directory, "ride-3.gpx").toPath(), "<gpx/>".getBytes("UTF-8"));

        RidingRecord record = store.load().get(0);
        assertEquals("옛 기록", record.getRr_name());
        assertNull(record.getRr_memo());
        assertTrue(record.getPhotos().isEmpty());
        assertNull(record.getWeather());
        assertEquals(record.getRr_num(), -3);
        assertTrue(store.gpxFile(record.getRr_gpx()).isFile());

        // and it can be edited like any other record
        assertEquals("새 메모", store.updateMemo(-3, "새 메모").getRr_memo());
        assertEquals(1, store.load().size());
        assertEquals(5000, store.load().get(0).getRr_distance());
    }

    @Test
    public void anUnreadableIndexIsSetAsideAndANewRideCanStillBeSaved() throws Exception {
        assertTrue(directory.mkdirs());
        java.nio.file.Files.write(new File(directory, "records.json").toPath(), "{not json".getBytes("UTF-8"));
        assertTrue(store.load().isEmpty());
        assertNotNull(saveRide("g.gpx", null, null, null));
        assertEquals(1, store.load().size());
    }

    // ---- memo --------------------------------------------------------------------------------

    @Test
    public void memoCanBeChangedClearedAndIsLimitedToFiveHundred() throws Exception {
        RidingRecord saved = saveRide("h.gpx", "처음", null, null);
        int id = saved.getRr_num();

        assertEquals("바뀐 메모", store.updateMemo(id, "바뀐 메모").getRr_memo());
        assertEquals("바뀐 메모", store.find(id).getRr_memo());
        assertNull(store.updateMemo(id, "").getRr_memo());
        assertNull(store.find(id).getRr_memo());

        char[] full = new char[500];
        Arrays.fill(full, '가');
        assertEquals(500, store.updateMemo(id, new String(full)).getRr_memo().length());

        try {
            store.updateMemo(id, new String(full) + "x");
            fail("memo over 500 characters must be refused");
        } catch (IOException expected) {
            assertEquals(500, store.find(id).getRr_memo().length()); // unchanged
        }
        assertNull(store.updateMemo(-999, "x"));
    }

    // ---- photos ------------------------------------------------------------------------------

    @Test
    public void photosCanBeAddedAndRemovedAfterwards() throws Exception {
        RidingRecord saved = saveRide("i.gpx", null, null, Collections.singletonList(photo("one.jpg", 10)));
        int id = saved.getRr_num();

        RidingRecord afterAdd = store.addPhotos(id, Arrays.asList(photo("two.jpg", 20), photo("three.jpg", 30)));
        assertEquals(3, afterAdd.getPhotos().size());
        assertEquals(3, photoFiles().length);
        assertEquals(Arrays.asList(0, 1, 2), orders(afterAdd));

        String removedName = afterAdd.getPhotos().get(1).getFile();
        RidingRecord afterRemove = store.removePhoto(id, removedName);
        assertEquals(2, afterRemove.getPhotos().size());
        assertFalse(new File(new File(directory, "photos"), removedName).exists());
        assertEquals(2, store.find(id).getPhotos().size());

        // removing something unknown changes nothing; an unknown ride is reported as null
        assertEquals(2, store.removePhoto(id, "nope.jpg").getPhotos().size());
        assertNull(store.removePhoto(-999, "x.jpg"));
        assertNull(store.addPhotos(-999, Collections.singletonList(photo("z.jpg", 1))));
    }

    @Test
    public void photoCountNeverExceedsTen() throws Exception {
        RidingRecord saved = saveRide("j.gpx", null, null, null);
        List<File> batch = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            batch.add(photo("b" + i + ".jpg", 3));
        }
        assertEquals(10, store.addPhotos(saved.getRr_num(), batch).getPhotos().size());
        assertEquals(0, store.addPhotos(saved.getRr_num(), Collections.singletonList(photo("late.jpg", 3))).getPhotos().size() - 10);
        assertEquals(10, photoFiles().length);
    }

    @Test
    public void aMissingPhotoFileNeverBreaksLoadingOrDeleting() throws Exception {
        RidingRecord saved = saveRide("k.gpx", null, null, Collections.singletonList(photo("lost.jpg", 10)));
        for (File file : photoFiles()) {
            assertTrue(file.delete()); // e.g. the user cleared app storage partly
        }
        RidingRecord loaded = store.load().get(0);
        assertEquals(1, loaded.getPhotos().size());
        assertFalse(store.photoFile(loaded.getPhotos().get(0).getFile()).exists());
        assertTrue(store.delete(saved.getRr_num()));
    }

    @Test
    public void photoNamesCannotEscapeTheFolder() throws Exception {
        for (String bad : new String[]{"../records.json", "a/b.jpg", "", null, "..\\x"}) {
            try {
                store.photoFile(bad);
                fail("accepted " + bad);
            } catch (IOException expected) {
                // fine
            }
        }
    }

    // ---- deleting ----------------------------------------------------------------------------

    @Test
    public void deletingARideRemovesItsGpxAndPhotosAndOnlyThose() throws Exception {
        RidingRecord keep = saveRide("keep.gpx", "남김", null, Collections.singletonList(photo("k.jpg", 10)));
        RidingRecord drop = saveRide("drop.gpx", "삭제", null,
                Arrays.asList(photo("d1.jpg", 10), photo("d2.jpg", 10)));
        assertEquals(3, photoFiles().length);

        assertTrue(store.delete(drop.getRr_num()));
        assertEquals(1, photoFiles().length);
        assertFalse(store.gpxFile(drop.getRr_gpx()).exists());
        assertTrue(store.gpxFile(keep.getRr_gpx()).exists());
        assertEquals(1, store.load().size());
        assertEquals("남김", store.load().get(0).getRr_memo());

        assertFalse(store.delete(drop.getRr_num()));
        assertFalse(store.delete(5)); // server ids are never touched
    }

    @Test
    public void memoNormalisation() {
        assertNull(GuestRideStoreCore.normalizeMemo(null));
        assertNull(GuestRideStoreCore.normalizeMemo(" \n\t"));
        assertEquals(" 앞뒤 공백 유지 ", GuestRideStoreCore.normalizeMemo(" 앞뒤 공백 유지 "));
        char[] long600 = new char[600];
        Arrays.fill(long600, 'a');
        assertEquals(500, GuestRideStoreCore.normalizeMemo(new String(long600)).length());
    }

    private static List<Integer> orders(RidingRecord record) {
        List<Integer> orders = new ArrayList<>();
        for (RecordPhoto photo : record.getPhotos()) {
            orders.add(photo.getSortOrder());
        }
        return orders;
    }

    @SuppressWarnings("unused")
    private static final Charset UTF8 = Charset.forName("UTF-8");
}
