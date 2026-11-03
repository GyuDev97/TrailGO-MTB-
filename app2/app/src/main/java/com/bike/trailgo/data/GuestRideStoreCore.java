package com.bike.trailgo.data;

import androidx.annotation.Nullable;

import com.bike.trailgo.dto.RecordPhoto;
import com.bike.trailgo.dto.RecordWeather;
import com.bike.trailgo.dto.RidingRecord;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Everything {@link LocalRideStore} does, on a plain directory (no Android Context), so it can be
 * unit tested on the JVM.
 *
 * <p>Layout: {@code records.json} (index), {@code ride-N.gpx}, and {@code photos/<file>.jpg}.
 * Index and GPX files written by earlier app versions load unchanged; fields added later (memo,
 * photos, weather) are simply absent there. Every write goes through {@link AtomicFiles}, and a
 * failed save never leaves a half-written record or an orphaned file visible.</p>
 */
public final class GuestRideStoreCore {
    public static final int MAX_PHOTOS = 10;
    public static final int MAX_MEMO_LENGTH = 500;

    static final String INDEX_FILE = "records.json";
    static final String PHOTO_DIRECTORY = "photos";
    static final String GPX_PREFIX = "local:";

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Gson GSON = new Gson();
    private static final Type RECORD_LIST_TYPE = new TypeToken<ArrayList<RidingRecord>>() { }.getType();
    private static final String GPX_NAME = "ride-[0-9]+\\.gpx";
    private static final String PHOTO_NAME = "[A-Za-z0-9][A-Za-z0-9._-]{0,80}";

    /** Hands out new (negative) local ride ids; backed by SharedPreferences in the app. */
    public interface IdSource {
        int nextLocalId();
    }

    private final File directory;
    private final IdSource ids;

    public GuestRideStoreCore(File directory, IdSource ids) {
        this.directory = directory;
        this.ids = ids;
    }

    public synchronized RidingRecord save(
            String name, int distanceMeters, int topSpeedKmh, int averageSpeedKmh, int elevationGainMeters,
            int breakSeconds, int durationSeconds, String area, File sourceGpx,
            @Nullable String memo, @Nullable RecordWeather weather, @Nullable List<File> photoSources
    ) throws IOException {
        if (sourceGpx == null || !sourceGpx.isFile() || sourceGpx.length() == 0L) {
            throw new IOException("Missing guest GPX file");
        }
        ensureDirectory(directory);

        ArrayList<RidingRecord> records = new ArrayList<>(load());
        String sourceId = sourceGpx.getName();
        for (RidingRecord existing : records) {
            if (sourceId.equals(existing.getLocal_source_id())) {
                // The previous attempt completed before its UI/process could clear the
                // unfinished-ride checkpoint. Do not create a duplicate on retry.
                return existing;
            }
        }

        int localId = ids.nextLocalId();
        String gpxName = "ride-" + Math.abs((long) localId) + ".gpx";
        File targetGpx = new File(directory, gpxName);
        AtomicFiles.copy(sourceGpx, targetGpx);

        ArrayList<File> copiedPhotos = new ArrayList<>();
        ArrayList<RecordPhoto> photos = new ArrayList<>();
        try {
            if (photoSources != null) {
                int order = 0;
                for (File source : photoSources) {
                    if (photos.size() >= MAX_PHOTOS) {
                        break;
                    }
                    if (source == null || !source.isFile()) {
                        continue; // a photo that vanished must not block the ride from being saved
                    }
                    File copied = copyPhoto(localId, order, source);
                    copiedPhotos.add(copied);
                    photos.add(new RecordPhoto(copied.getName(), order, copied.getName()));
                    order++;
                }
            }

            RidingRecord record = new RidingRecord();
            record.setRr_num(localId);
            record.setRr_rider("guest");
            record.setRr_date(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(new Date()));
            record.setRr_name(isBlank(name) ? "라이딩 기록" : name.trim());
            record.setRr_distance(Math.max(0, distanceMeters));
            record.setRr_topspeed(Math.max(0, topSpeedKmh));
            record.setRr_avgspeed(Math.max(0, averageSpeedKmh));
            record.setRr_high(Math.max(0, elevationGainMeters));
            record.setRr_breaktime(Math.max(0, breakSeconds));
            record.setRr_time(Math.max(0, durationSeconds));
            record.setRr_area(area == null ? "" : area);
            // Guest records are always private and their GPX name is never sent to the server.
            record.setRr_open(0);
            record.setRr_gpx(GPX_PREFIX + gpxName);
            record.setLocal_source_id(sourceId);
            record.setRr_memo(normalizeMemo(memo));
            record.setPhotos(photos);
            if (weather != null && weather.isAvailable()) {
                record.setWeather(weather);
            }

            records.add(record);
            sortNewestFirst(records);
            writeIndex(records);
            return record;
        } catch (IOException | RuntimeException error) {
            // A record without an index must not become a visible partial save.
            targetGpx.delete();
            for (File copied : copiedPhotos) {
                copied.delete();
            }
            throw error;
        }
    }

    public synchronized ArrayList<RidingRecord> load() {
        File index = new File(directory, INDEX_FILE);
        if (!index.isFile()) {
            return new ArrayList<>();
        }
        try {
            ArrayList<RidingRecord> records = GSON.fromJson(new String(AtomicFiles.read(index), UTF8), RECORD_LIST_TYPE);
            if (records == null) {
                return new ArrayList<>();
            }
            sortNewestFirst(records);
            return records;
        } catch (Exception ignored) {
            quarantineUnreadableIndex(index);
            return new ArrayList<>();
        }
    }

    @Nullable
    public synchronized RidingRecord find(int localId) {
        for (RidingRecord record : load()) {
            if (record.getRr_num() == localId) {
                return record;
            }
        }
        return null;
    }

    /** Replaces the memo (blank clears it). The previous memo stays if the write fails. */
    @Nullable
    public synchronized RidingRecord updateMemo(int localId, @Nullable String memo) throws IOException {
        if (memo != null && memo.length() > MAX_MEMO_LENGTH) {
            throw new IOException("Memo is longer than " + MAX_MEMO_LENGTH + " characters");
        }
        ArrayList<RidingRecord> records = new ArrayList<>(load());
        RidingRecord target = findIn(records, localId);
        if (target == null) {
            return null;
        }
        target.setRr_memo(normalizeMemo(memo));
        writeIndex(records);
        return target;
    }

    /**
     * Adds photos (up to the 10-photo limit) from already prepared files, which are copied into the
     * private photo folder. Returns the updated record, or null for an unknown id. When the index
     * cannot be written, the copies made so far are removed again.
     */
    @Nullable
    public synchronized RidingRecord addPhotos(int localId, List<File> sources) throws IOException {
        ArrayList<RidingRecord> records = new ArrayList<>(load());
        RidingRecord target = findIn(records, localId);
        if (target == null) {
            return null;
        }
        ArrayList<RecordPhoto> photos = new ArrayList<>(target.getPhotos());
        ArrayList<File> copied = new ArrayList<>();
        try {
            int order = nextOrder(photos);
            for (File source : sources) {
                if (photos.size() >= MAX_PHOTOS) {
                    break;
                }
                if (source == null || !source.isFile()) {
                    continue;
                }
                File file = copyPhoto(localId, order, source);
                copied.add(file);
                photos.add(new RecordPhoto(file.getName(), order, file.getName()));
                order++;
            }
            target.setPhotos(photos);
            writeIndex(records);
            return target;
        } catch (IOException | RuntimeException error) {
            for (File file : copied) {
                file.delete();
            }
            throw error;
        }
    }

    /** Removes one photo from the record and then its file. */
    @Nullable
    public synchronized RidingRecord removePhoto(int localId, String fileName) throws IOException {
        ArrayList<RidingRecord> records = new ArrayList<>(load());
        RidingRecord target = findIn(records, localId);
        if (target == null) {
            return null;
        }
        ArrayList<RecordPhoto> photos = new ArrayList<>(target.getPhotos());
        RecordPhoto found = null;
        for (RecordPhoto photo : photos) {
            if (fileName != null && fileName.equals(photo.getFile())) {
                found = photo;
                break;
            }
        }
        if (found == null) {
            return target;
        }
        photos.remove(found);
        target.setPhotos(photos);
        writeIndex(records);
        if (fileName.matches(PHOTO_NAME)) {
            new File(photoDirectory(), fileName).delete();
        }
        return target;
    }

    /** Removes a guest record with its GPX and photos. Server records are never touched. */
    public synchronized boolean delete(int localId) throws IOException {
        if (localId >= 0) {
            return false;
        }
        ArrayList<RidingRecord> records = new ArrayList<>(load());
        RidingRecord target = findIn(records, localId);
        if (target == null) {
            return false;
        }
        records.remove(target);
        writeIndex(records);
        String gpxReference = target.getRr_gpx();
        if (isLocalGpxReference(gpxReference)) {
            String fileName = gpxReference.substring(GPX_PREFIX.length());
            if (fileName.matches(GPX_NAME)) {
                new File(directory, fileName).delete();
            }
        }
        for (RecordPhoto photo : target.getPhotos()) {
            String file = photo.getFile();
            if (file != null && file.matches(PHOTO_NAME)) {
                new File(photoDirectory(), file).delete();
            }
        }
        return true;
    }

    /** The file of a guest photo; may not exist (the UI shows a placeholder then). */
    public File photoFile(String fileName) throws IOException {
        if (fileName == null || !fileName.matches(PHOTO_NAME)) {
            throw new IOException("Invalid photo name");
        }
        return new File(photoDirectory(), fileName);
    }

    public File gpxFile(String reference) throws IOException {
        if (!isLocalGpxReference(reference)) {
            throw new IOException("Not a local GPX reference");
        }
        String fileName = reference.substring(GPX_PREFIX.length());
        if (!fileName.matches(GPX_NAME)) {
            throw new IOException("Invalid local GPX name");
        }
        return new File(directory, fileName);
    }

    public static boolean isLocalGpxReference(@Nullable String reference) {
        return reference != null && reference.startsWith(GPX_PREFIX);
    }

    /** Blank means "no memo"; the text is otherwise kept exactly as typed. */
    @Nullable
    public static String normalizeMemo(@Nullable String memo) {
        if (memo == null || memo.trim().isEmpty()) {
            return null;
        }
        return memo.length() > MAX_MEMO_LENGTH ? memo.substring(0, MAX_MEMO_LENGTH) : memo;
    }

    private File photoDirectory() {
        return new File(directory, PHOTO_DIRECTORY);
    }

    private File copyPhoto(int localId, int order, File source) throws IOException {
        ensureDirectory(photoDirectory());
        String name = "p" + Math.abs((long) localId) + "-" + System.currentTimeMillis() + "-" + order + ".jpg";
        File target = new File(photoDirectory(), name);
        AtomicFiles.copy(source, target);
        return target;
    }

    private static int nextOrder(List<RecordPhoto> photos) {
        int max = -1;
        for (RecordPhoto photo : photos) {
            max = Math.max(max, photo.getSortOrder());
        }
        return max + 1;
    }

    private static RidingRecord findIn(List<RidingRecord> records, int localId) {
        for (RidingRecord record : records) {
            if (record.getRr_num() == localId) {
                return record;
            }
        }
        return null;
    }

    private void writeIndex(List<RidingRecord> records) throws IOException {
        ensureDirectory(directory);
        AtomicFiles.write(new File(directory, INDEX_FILE), GSON.toJson(records).getBytes(UTF8));
    }

    private static void ensureDirectory(File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create " + directory.getName());
        }
    }

    private static void quarantineUnreadableIndex(File index) {
        File parent = index.getParentFile();
        if (parent == null) {
            return;
        }
        File backup = new File(parent, "records-corrupt-" + System.currentTimeMillis() + ".json");
        // Best effort only: read failures must never crash or block a new local ride.
        index.renameTo(backup);
    }

    private static void sortNewestFirst(ArrayList<RidingRecord> records) {
        Collections.sort(records, new Comparator<RidingRecord>() {
            @Override
            public int compare(RidingRecord left, RidingRecord right) {
                String leftDate = left.getRr_date() == null ? "" : left.getRr_date();
                String rightDate = right.getRr_date() == null ? "" : right.getRr_date();
                return rightDate.compareTo(leftDate);
            }
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
