package com.bike.trailgo.ui.riding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.bike.trailgo.dto.RecordWeather;
import com.google.gson.Gson;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;

public class RideWeatherFileTest {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private static RecordWeather weather(boolean available) {
        return new Gson().fromJson(available
                ? "{\"available\":true,\"observedAt\":\"2026-10-10T19:00:00+09:00\",\"temperatureC\":18.5,"
                + "\"precipitation\":\"NONE\",\"label\":\"강수 없음\",\"token\":\"0123456789abcdef0123456789abcdef\"}"
                : "{\"available\":false}", RecordWeather.class);
    }

    @Test
    public void theStartWeatherSurvivesAProcessRestart() throws Exception {
        File dir = temp.getRoot();
        RideWeatherFile.save(dir, weather(true));
        RecordWeather loaded = RideWeatherFile.load(dir);
        assertNotNull(loaded);
        assertEquals(18.5, loaded.getTemperatureC(), 0.0);
        assertEquals("0123456789abcdef0123456789abcdef", loaded.getToken());
    }

    @Test
    public void nothingIsStoredForAnUnavailableWeather() throws Exception {
        RideWeatherFile.save(temp.getRoot(), weather(false));
        RideWeatherFile.save(temp.getRoot(), null);
        assertNull(RideWeatherFile.load(temp.getRoot()));
        assertFalse(new File(temp.getRoot(), RideWeatherFile.NAME).exists());
    }

    @Test
    public void aMissingOrCorruptFileReadsAsNoWeatherInsteadOfCrashing() throws Exception {
        assertNull(RideWeatherFile.load(temp.getRoot()));
        Files.write(new File(temp.getRoot(), RideWeatherFile.NAME).toPath(), "{not json".getBytes("UTF-8"));
        assertNull(RideWeatherFile.load(temp.getRoot()));
    }

    @Test
    public void clearingRemovesIt() throws Exception {
        RideWeatherFile.save(temp.getRoot(), weather(true));
        RideWeatherFile.clear(temp.getRoot());
        assertNull(RideWeatherFile.load(temp.getRoot()));
        RideWeatherFile.clear(temp.getRoot()); // clearing twice is fine
    }

    @Test
    public void aNewerWeatherReplacesTheOldOne() throws Exception {
        RideWeatherFile.save(temp.getRoot(), weather(true));
        RecordWeather other = new Gson().fromJson("{\"available\":true,\"temperatureC\":3.0,\"precipitation\":\"SNOW\",\"label\":\"눈\"}", RecordWeather.class);
        RideWeatherFile.save(temp.getRoot(), other);
        assertEquals(3.0, RideWeatherFile.load(temp.getRoot()).getTemperatureC(), 0.0);
    }
}