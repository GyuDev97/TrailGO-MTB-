package com.bike.trailgo.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bike.trailgo.dto.RidingRecord;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/** Month totals, date reading and the texts of the home screen. */
public class HomeLogicTest {
    private static final TimeZone SEOUL = RecordDates.SEOUL;
    private static final int OCT_2026 = 202610;

    private static RidingRecord ride(String date, int meters, int gain, int seconds) {
        RidingRecord record = new RidingRecord();
        record.setRr_date(date);
        record.setRr_distance(meters);
        record.setRr_high(gain);
        record.setRr_time(seconds);
        return record;
    }

    // ---- dates -------------------------------------------------------------------------------

    @Test
    public void readsWallClockAndDateOnlyText() {
        RecordDates.Parts parts = RecordDates.parse("2026-10-13 22:59:08", SEOUL);
        assertEquals(2026, parts.year);
        assertEquals(10, parts.month);
        assertEquals(13, parts.day);
        assertEquals(22, parts.hour);
        assertEquals(59, parts.minute);
        RecordDates.Parts dateOnly = RecordDates.parse("2026-10-13", SEOUL);
        assertFalse(dateOnly.hasTime());
    }

    @Test
    public void anIsoTimestampWithAnOffsetIsReadInKoreaTime() {
        // 16:30 UTC on Sep 30 is 01:30 on Oct 1 in Korea
        RecordDates.Parts parts = RecordDates.parse("2026-09-30T16:30:00.000+00:00", SEOUL);
        assertEquals(202610, RecordDates.monthKey(parts));
        assertEquals(1, parts.day);
        assertEquals(1, parts.hour);
        assertEquals(30, parts.minute);
        assertEquals(202610, RecordDates.monthKey(RecordDates.parse("2026-09-30T16:30:00Z", SEOUL)));
        assertEquals(202609, RecordDates.monthKey(RecordDates.parse("2026-09-30T10:00:00+09:00", SEOUL)));
    }

    @Test
    public void unreadableDatesGiveNothing() {
        assertNull(RecordDates.parse(null, SEOUL));
        assertNull(RecordDates.parse("", SEOUL));
        assertNull(RecordDates.parse("yesterday", SEOUL));
        assertNull(RecordDates.parse("2026-13-40 10:00:00", SEOUL));
        assertNull(RecordDates.parse("2026-10-10 25:00:00", SEOUL));
    }

    @Test
    public void currentMonthIsDecidedInTheGivenZone() {
        // 2026-09-30 16:00 UTC: still September in UTC, already October in Seoul
        long instant = java.time.Instant.parse("2026-09-30T16:00:00Z").toEpochMilli();
        assertEquals(202610, RecordDates.currentMonthKey(SEOUL, instant));
        assertEquals(202609, RecordDates.currentMonthKey(TimeZone.getTimeZone("UTC"), instant));
    }

    // ---- monthly totals ----------------------------------------------------------------------

    @Test
    public void onlyRidesOfTheMonthAreCountedAndSummed() {
        List<RidingRecord> records = Arrays.asList(
                ride("2026-10-13 22:59:00", 1200, 18, 480),
                ride("2026-10-10 19:49:00", 48000, 36, 3600),
                ride("2026-09-28 08:00:00", 90000, 500, 7200),   // last month
                ride("2025-10-05 08:00:00", 10000, 100, 1000));  // same month, other year
        MonthlySummary summary = MonthlySummary.of(records, OCT_2026, SEOUL);
        assertEquals(2, summary.rideCount);
        assertEquals(49200L, summary.distanceMeters);
        assertEquals(54L, summary.elevationGainMeters);
        assertEquals(4080L, summary.ridingSeconds);
        assertEquals(0, summary.suspiciousCount);
        assertEquals(2026, summary.year());
        assertEquals(10, summary.month());
    }

    @Test
    public void monthBoundariesAreExact() {
        List<RidingRecord> records = Arrays.asList(
                ride("2026-09-30 23:59:59", 1000, 1, 100),
                ride("2026-10-01 00:00:00", 2000, 2, 200),
                ride("2026-10-31 23:59:59", 4000, 4, 400),
                ride("2026-11-01 00:00:00", 8000, 8, 800));
        MonthlySummary summary = MonthlySummary.of(records, OCT_2026, SEOUL);
        assertEquals(2, summary.rideCount);
        assertEquals(6000L, summary.distanceMeters);
    }

    @Test
    public void anIsoRecordNearMidnightLandsInTheRightKoreanMonth() {
        // saved 01:30 on Oct 1 Korea time, delivered as UTC of Sep 30
        MonthlySummary summary = MonthlySummary.of(
                Collections.singletonList(ride("2026-09-30T16:30:00.000+00:00", 500, 1, 60)), OCT_2026, SEOUL);
        assertEquals(1, summary.rideCount);
    }

    @Test
    public void noRidesMeansZeroNotAnError() {
        MonthlySummary empty = MonthlySummary.of(new ArrayList<RidingRecord>(), OCT_2026, SEOUL);
        assertEquals(0, empty.rideCount);
        assertEquals(0L, empty.distanceMeters);
        MonthlySummary none = MonthlySummary.of(null, OCT_2026, SEOUL);
        assertEquals(0, none.rideCount);
    }

    @Test
    public void aSuspiciousTimeStaysInTheSumsAndIsOnlyFlagged() {
        // the reported record: 50 m in 68 h 19 min
        List<RidingRecord> records = Arrays.asList(
                ride("2026-10-13 22:59:00", 50, 18, 68 * 3600 + 19 * 60),
                ride("2026-10-10 19:49:00", 48000, 36, 3600));
        MonthlySummary summary = MonthlySummary.of(records, OCT_2026, SEOUL);
        assertEquals(2, summary.rideCount);
        assertEquals(68L * 3600 + 19 * 60 + 3600, summary.ridingSeconds); // stored value, not corrected
        assertEquals(1, summary.suspiciousCount);
    }

    @Test
    public void damagedRecordsNeverMakeTotalsNegativeOrOverflow() {
        List<RidingRecord> records = Arrays.asList(
                ride("2026-10-01 10:00:00", -5, -3, -10),
                ride("2026-10-02 10:00:00", Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE),
                ride("2026-10-03 10:00:00", Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        MonthlySummary summary = MonthlySummary.of(records, OCT_2026, SEOUL);
        assertEquals(3, summary.rideCount);
        assertEquals(2L * Integer.MAX_VALUE, summary.distanceMeters);
        assertTrue(summary.elevationGainMeters >= 0);
        assertTrue(summary.ridingSeconds >= 0);
    }

    @Test
    public void recordsWithoutAReadableDateBelongToNoMonth() {
        List<RidingRecord> records = Arrays.asList(
                ride(null, 1000, 1, 100), ride("garbage", 1000, 1, 100), ride("2026-10-05 10:00:00", 500, 1, 50));
        MonthlySummary summary = MonthlySummary.of(records, OCT_2026, SEOUL);
        assertEquals(1, summary.rideCount);
        assertEquals(2, summary.undatedCount);
    }

    // ---- texts -------------------------------------------------------------------------------

    @Test
    public void valuesAreSplitIntoNumberAndUnit() {
        assertEquals("2", HomeFormat.count(2).number);
        assertEquals("회", HomeFormat.count(2).unit);
        assertEquals("49 m", HomeFormat.distance(49).joined());
        assertEquals("0 m", HomeFormat.distance(0).joined());
        assertEquals("1.0 km", HomeFormat.distance(1000).joined());
        assertEquals("12.5 km", HomeFormat.distance(12450).joined());
        assertEquals("125 km", HomeFormat.distance(125_000).joined());
        assertEquals("54 m", HomeFormat.elevation(54).joined());
        assertEquals("1,250 m", HomeFormat.elevation(1250).joined());
    }

    @Test
    public void monthlyTimeIsHoursAndMinutesOrMinutes() {
        assertEquals("68:20 시간", HomeFormat.duration(68 * 3600 + 20 * 60 + 30).joined());
        assertEquals("1:00 시간", HomeFormat.duration(3600).joined());
        assertEquals("45 분", HomeFormat.duration(45 * 60).joined());
        assertEquals("<1 분", HomeFormat.duration(8).joined());
        assertEquals("0 분", HomeFormat.duration(0).joined());
    }

    @Test
    public void titlesFallBackWithoutInventingNames() {
        RidingRecord named = new RidingRecord();
        named.setRr_name("  북한산 주말 라이딩  ");
        assertEquals("북한산 주말 라이딩", HomeFormat.title(named));
        RidingRecord areaOnly = new RidingRecord();
        areaOnly.setRr_area("대구");
        assertEquals("대구 라이딩", HomeFormat.title(areaOnly));
        assertEquals("라이딩 기록", HomeFormat.title(new RidingRecord()));
        RidingRecord url = new RidingRecord();
        url.setRr_name("https://play.google.com/store/");
        assertEquals("https://play.google.com/store/", HomeFormat.title(url)); // shown as is, ellipsized by the view
    }

    @Test
    public void dateAndTimeAreShownInKoreanTwelveHourStyle() {
        assertEquals("2026년 10월 13일 · 오후 10:59", HomeFormat.dateAndTime("2026-10-13 22:59:08", SEOUL));
        assertEquals("2026년 10월 10일 · 오후 7:49", HomeFormat.dateAndTime("2026-10-10 19:49:00", SEOUL));
        assertEquals("2026년 10월 1일 · 오전 12:05", HomeFormat.dateAndTime("2026-10-01 00:05:00", SEOUL));
        assertEquals("2026년 10월 1일 · 오후 12:30", HomeFormat.dateAndTime("2026-10-01 12:30:00", SEOUL));
        assertEquals("2026년 10월 1일", HomeFormat.dateAndTime("2026-10-01", SEOUL));
        assertEquals("-", HomeFormat.dateAndTime(null, SEOUL));
        assertEquals("-", HomeFormat.dateAndTime("nonsense", SEOUL));
    }

    @Test
    public void rowTextsUseTheSameRulesAsTheRecordScreens() {
        RidingRecord record = ride("2026-10-13 22:59:00", 1, 18, 8);
        assertEquals("1 m", HomeFormat.rowDistance(record));
        assertEquals("18 m", HomeFormat.rowElevation(record));
        assertEquals("1분 미만", HomeFormat.rowDuration(record));
        RidingRecord empty = ride("2026-10-13 22:59:00", 0, 0, 0);
        assertEquals("-", HomeFormat.rowDistance(empty));
        assertEquals("-", HomeFormat.rowDuration(empty));
        assertNotNull(HomeFormat.monthLabel(OCT_2026));
        assertEquals("2026년 10월", HomeFormat.monthLabel(OCT_2026));
    }
}
