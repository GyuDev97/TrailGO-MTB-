package com.bike.trailgo.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.bike.trailgo.dto.RecordWeather;
import com.bike.trailgo.dto.RidingRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Private, device-local records made while the rider is using guest mode.
 *
 * <p>Thin Android facade over {@link GuestRideStoreCore}, which holds the logic (index, GPX,
 * memo, photos) and is unit tested on the JVM.</p>
 */
public final class LocalRideStore {
    private static final String DIRECTORY = "guest-rides";
    private static final String PREFS = "guest_ride_store";
    private static final String KEY_NEXT_ID = "next_id";

    private LocalRideStore() {
    }

    private static GuestRideStoreCore core(Context context) {
        final Context appContext = context.getApplicationContext();
        return new GuestRideStoreCore(new File(appContext.getFilesDir(), DIRECTORY), new GuestRideStoreCore.IdSource() {
            @Override
            public int nextLocalId() {
                return LocalRideStore.nextLocalId(appContext);
            }
        });
    }

    public static synchronized RidingRecord save(
            Context context,
            String name,
            int distanceMeters,
            int topSpeedKmh,
            int averageSpeedKmh,
            int elevationGainMeters,
            int breakSeconds,
            int durationSeconds,
            String area,
            File sourceGpx
    ) throws IOException {
        return save(context, name, distanceMeters, topSpeedKmh, averageSpeedKmh, elevationGainMeters,
                breakSeconds, durationSeconds, area, sourceGpx, null, null, null);
    }

    /** Same as above, also storing the memo, the weather snapshot and the (already prepared) photo files. */
    public static synchronized RidingRecord save(
            Context context,
            String name,
            int distanceMeters,
            int topSpeedKmh,
            int averageSpeedKmh,
            int elevationGainMeters,
            int breakSeconds,
            int durationSeconds,
            String area,
            File sourceGpx,
            @Nullable String memo,
            @Nullable RecordWeather weather,
            @Nullable List<File> photos
    ) throws IOException {
        return core(context).save(name, distanceMeters, topSpeedKmh, averageSpeedKmh, elevationGainMeters,
                breakSeconds, durationSeconds, area, sourceGpx, memo, weather, photos);
    }

    public static synchronized ArrayList<RidingRecord> load(Context context) {
        return core(context).load();
    }

    public static boolean hasRecords(Context context) {
        return !load(context).isEmpty();
    }

    public static boolean isLocalGpxReference(@Nullable String reference) {
        return GuestRideStoreCore.isLocalGpxReference(reference);
    }

    public static InputStream openGpx(Context context, String reference) throws IOException {
        File file = core(context).gpxFile(reference);
        if (!file.isFile()) {
            throw new IOException("Local GPX file not found");
        }
        return new FileInputStream(file);
    }

    /** Removes a guest record with its GPX and photos. Server records are never touched. */
    public static synchronized boolean delete(Context context, int localId) throws IOException {
        return core(context).delete(localId);
    }

    @Nullable
    public static synchronized RidingRecord find(Context context, int localId) {
        return core(context).find(localId);
    }

    @Nullable
    public static synchronized RidingRecord updateMemo(Context context, int localId, @Nullable String memo) throws IOException {
        return core(context).updateMemo(localId, memo);
    }

    @Nullable
    public static synchronized RidingRecord addPhotos(Context context, int localId, List<File> photos) throws IOException {
        return core(context).addPhotos(localId, photos);
    }

    @Nullable
    public static synchronized RidingRecord removePhoto(Context context, int localId, String fileName) throws IOException {
        return core(context).removePhoto(localId, fileName);
    }

    /** File of a guest photo; it may be missing, callers must show a placeholder then. */
    public static File photoFile(Context context, String fileName) throws IOException {
        return core(context).photoFile(fileName);
    }

    private static int nextLocalId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int id = preferences.getInt(KEY_NEXT_ID, -1);
        if (id >= 0 || id == Integer.MIN_VALUE) id = -1;
        preferences.edit().putInt(KEY_NEXT_ID, id - 1).apply();
        return id;
    }
}
