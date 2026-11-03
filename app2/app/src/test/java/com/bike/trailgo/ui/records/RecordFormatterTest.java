package com.bike.trailgo.ui.records;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecordFormatterTest {
    @Test
    public void distanceUsesMetersBelowOneKilometer() {
        assertEquals("50 m", RecordFormatter.formatDistance(50));
        assertEquals("999 m", RecordFormatter.formatDistance(999));
        assertEquals("1.00 km", RecordFormatter.formatDistance(1000));
        assertEquals("12.45 km", RecordFormatter.formatDistance(12450));
        assertEquals("-", RecordFormatter.formatDistance(0));
    }

    @Test
    public void durationFromSeconds() {
        assertEquals("-", RecordFormatter.formatDuration(0));
        assertEquals("1분 미만", RecordFormatter.formatDuration(45));
        assertEquals("5분", RecordFormatter.formatDuration(300));
        assertEquals("68시간 19분", RecordFormatter.formatDuration(68 * 3600 + 19 * 60 + 5));
    }

    @Test
    public void speedIsDashWithoutMeasurementAndBelowOneWhenTruncatedToZero() {
        assertEquals("-", RecordFormatter.formatSpeed(0, 0, 600));
        assertEquals("-", RecordFormatter.formatSpeed(5, 1000, 0));
        assertEquals("1 km/h 미만", RecordFormatter.formatSpeed(0, 50, 600));
        assertEquals("21 km/h", RecordFormatter.formatSpeed(21, 12000, 2000));
    }

    @Test
    public void caloriesAreAnEstimateFromDistanceOnly() {
        assertEquals(0, RecordFormatter.estimateCalories(0));
        assertEquals("-", RecordFormatter.formatEstimatedCalories(0));
        assertEquals("약 50 kcal", RecordFormatter.formatEstimatedCalories(1000));
        assertEquals("약 3 kcal", RecordFormatter.formatEstimatedCalories(50));
    }

    @Test
    public void parsesWhatItFormats() {
        assertEquals(50, RecordFormatter.parseDistanceMeters(RecordFormatter.formatDistance(50)));
        assertEquals(12450, RecordFormatter.parseDistanceMeters(RecordFormatter.formatDistance(12450)));
        assertEquals(-1, RecordFormatter.parseDistanceMeters("-"));
        assertEquals(-1, RecordFormatter.parseDistanceMeters(null));
        assertEquals(3600 + 5 * 60, RecordFormatter.parseDurationSeconds("1시간 5분"));
        assertEquals(12 * 60, RecordFormatter.parseDurationSeconds("12분"));
        assertEquals(-1, RecordFormatter.parseDurationSeconds("-"));
    }

    @Test
    public void screenshotRecordIsFlaggedAsSuspicious() {
        // 0.05 km in 68h19m: the combination seen in the reported capture.
        assertTrue(RecordFormatter.isSuspiciousDuration(50, 68 * 3600 + 19 * 60));
        // Ordinary 12.45 km ride in 35 minutes.
        assertFalse(RecordFormatter.isSuspiciousDuration(12450, 35 * 60));
        // Long but plausible: 90 km in 5 hours.
        assertFalse(RecordFormatter.isSuspiciousDuration(90000, 5 * 3600));
        // Over an hour at under 1 km/h on average.
        assertTrue(RecordFormatter.isSuspiciousDuration(500, 2 * 3600));
        // No time at all is "missing", not "suspicious".
        assertFalse(RecordFormatter.isSuspiciousDuration(0, 0));
    }

    @Test
    public void weatherIsShownOnlyWhenTheStoredSnapshotIsComplete() {
        assertEquals("맑음 · 21°C", RecordFormatter.formatWeather(true, "맑음", 21.0));
        assertEquals("흐림 · 12.5°C", RecordFormatter.formatWeather(true, " 흐림 ", 12.5));
        assertEquals("눈 · -3°C", RecordFormatter.formatWeather(true, "눈", -3.0));
        assertNull(RecordFormatter.formatWeather(false, "맑음", 21.0));
        assertNull(RecordFormatter.formatWeather(true, null, 21.0));
        assertNull(RecordFormatter.formatWeather(true, "  ", 21.0));
        assertNull(RecordFormatter.formatWeather(true, "맑음", null));
        assertNull(RecordFormatter.formatWeather(true, "맑음", Double.NaN));
    }

    @Test
    public void measurementNeedsBothDistanceAndTime() {
        assertFalse(RecordFormatter.hasMeasurement(0, 600));
        assertFalse(RecordFormatter.hasMeasurement(100, 0));
        assertTrue(RecordFormatter.hasMeasurement(100, 60));
    }

    @Test
    public void observedTimeIsReadFromTheIsoTimestamp() {
        assertEquals("18:00 관측", RecordFormatter.formatObservedTime("2026-10-10T18:00:00+09:00"));
        assertEquals("07:05 관측", RecordFormatter.formatObservedTime("2026-10-10T07:05:00+09:00"));
        assertNull(RecordFormatter.formatObservedTime(null));
        assertNull(RecordFormatter.formatObservedTime("garbage"));
        assertNull(RecordFormatter.formatObservedTime("2026-10-10T99:99:00+09:00"));
    }
    @Test
    public void precipitationAmountIsOnlyShownWhenThereWasSome() {
        assertNull(RecordFormatter.formatPrecipitationAmount(null));
        assertNull(RecordFormatter.formatPrecipitationAmount(0.0));
        assertNull(RecordFormatter.formatPrecipitationAmount(Double.NaN));
        assertEquals("1.5mm", RecordFormatter.formatPrecipitationAmount(1.5));
        assertEquals("2mm", RecordFormatter.formatPrecipitationAmount(2.0));
        assertEquals("0.3mm", RecordFormatter.formatPrecipitationAmount(0.3));
    }

    @Test
    public void weatherNoteSaysWhereTheValueComesFrom() {
        assertEquals("시작 시각 기준 · 19:00 관측",
                RecordFormatter.formatWeatherNote("2026-10-10T19:00:00+09:00", null));
        assertEquals("시작 시각 기준 · 19:00 관측 · 1시간 강수 1.5mm",
                RecordFormatter.formatWeatherNote("2026-10-10T19:00:00+09:00", 1.5));
        assertEquals("시작 시각 기준", RecordFormatter.formatWeatherNote(null, 0.0));
    }}
