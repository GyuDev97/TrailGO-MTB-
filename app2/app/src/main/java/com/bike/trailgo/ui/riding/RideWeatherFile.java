package com.bike.trailgo.ui.riding;

import androidx.annotation.Nullable;

import com.bike.trailgo.data.AtomicFiles;
import com.bike.trailgo.dto.RecordWeather;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * The weather looked up when the current ride started, kept next to the ride's recovery checkpoint
 * so it survives a process restart and reaches the save screen. It belongs to exactly one ride:
 * it is removed whenever the ride is saved, discarded, or a new ride begins. Pure java.io.
 */
public final class RideWeatherFile {
    static final String NAME = "ride-weather.json";
    private static final Gson GSON = new Gson();
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private RideWeatherFile() {
    }

    public static void save(File directory, RecordWeather weather) throws IOException {
        if (weather == null || !weather.isAvailable()) {
            return;
        }
        AtomicFiles.write(new File(directory, NAME), GSON.toJson(weather).getBytes(UTF8));
    }

    /** @return the stored weather, or null when there is none, it is unreadable or not available */
    @Nullable
    public static RecordWeather load(File directory) {
        File file = new File(directory, NAME);
        if (!file.isFile()) {
            return null;
        }
        try {
            RecordWeather weather = GSON.fromJson(new String(AtomicFiles.read(file), UTF8), RecordWeather.class);
            return weather != null && weather.isAvailable() ? weather : null;
        } catch (IOException | RuntimeException error) {
            return null;
        }
    }

    public static void clear(File directory) {
        //noinspection ResultOfMethodCallIgnored
        new File(directory, NAME).delete();
        //noinspection ResultOfMethodCallIgnored
        new File(directory, NAME + ".tmp").delete();
    }
}