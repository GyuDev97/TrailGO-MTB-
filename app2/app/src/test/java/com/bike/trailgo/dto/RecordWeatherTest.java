package com.bike.trailgo.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

public class RecordWeatherTest {
    private static RecordWeather parse(String json) {
        return new Gson().fromJson(json, RecordWeather.class);
    }

    @Test
    public void readsTheServersObservationJson() {
        RecordWeather w = parse("{\"available\":true,\"observedAt\":\"2026-10-10T19:00:00+09:00\","
                + "\"temperatureC\":18.5,\"precipitation\":\"RAIN\",\"precipitationMm\":1.5,\"label\":\"비\","
                + "\"token\":\"0123456789abcdef0123456789abcdef\"}");
        assertTrue(w.isAvailable());
        assertEquals(18.5, w.getTemperatureC(), 0.0);
        assertEquals(1.5, w.getPrecipitationMm(), 0.0);
        assertEquals("비", w.getLabel());
        assertNull(w.getSky());
        assertEquals("0123456789abcdef0123456789abcdef", w.getToken());
        assertTrue(w.isPrecipitating());
    }

    @Test
    public void unavailableMeansUnavailable() {
        RecordWeather w = parse("{\"available\":false}");
        assertFalse(w.isAvailable());
        assertNull(w.getLabel());
        assertNull(w.getToken());
    }

    @Test
    public void theIconMatchesWhatWasObservedNeverASunWithoutASkyReading() {
        assertEquals(RecordWeather.Icon.CLOUD, parse("{\"available\":true,\"precipitation\":\"SNOW\"}").iconKind());
        assertEquals(RecordWeather.Icon.CLOUD, parse("{\"available\":true,\"precipitation\":\"DRIZZLE\"}").iconKind());
        // no precipitation and no sky reading: neutral, not sunny
        assertEquals(RecordWeather.Icon.NEUTRAL, parse("{\"available\":true,\"precipitation\":\"NONE\"}").iconKind());
        // snapshots stored by an older server with a real sky value keep their sun
        assertEquals(RecordWeather.Icon.SUN, parse("{\"available\":true,\"sky\":\"CLEAR\",\"precipitation\":\"NONE\"}").iconKind());
        assertEquals(RecordWeather.Icon.CLOUD, parse("{\"available\":true,\"sky\":\"CLEAR\",\"precipitation\":\"RAIN\"}").iconKind());
    }

    @Test
    public void aGuestCopyDropsTheServerToken() {
        RecordWeather w = parse("{\"available\":true,\"temperatureC\":10.0,\"precipitation\":\"NONE\","
                + "\"label\":\"강수 없음\",\"token\":\"0123456789abcdef0123456789abcdef\"}");
        RecordWeather copy = w.withoutToken();
        assertNull(copy.getToken());
        assertEquals(10.0, copy.getTemperatureC(), 0.0);
        assertEquals("강수 없음", copy.getLabel());
        assertFalse(new Gson().toJson(copy).contains("0123456789abcdef"));
    }
}
