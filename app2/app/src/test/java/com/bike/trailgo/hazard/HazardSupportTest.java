package com.bike.trailgo.hazard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

/** Refresh timing, spoken/banner text, report validation and settings sanity. */
public class HazardSupportTest {

    private static final double LAT = 37.5;
    private static final double LON = 127.0;
    private static final double METERS_PER_DEGREE_LAT = 111_195.0;

    // ---- refresh policy -------------------------------------------------------------------------------

    @Test
    public void firstFixAsksTheServerOnce() {
        HazardRefreshPolicy policy = new HazardRefreshPolicy();
        assertTrue(policy.shouldRefresh(LAT, LON, 1_000L));
        policy.onAttempt(1_000L);
        assertFalse("a request in flight blocks the next one", policy.shouldRefresh(LAT, LON, 2_000L));
    }

    @Test
    public void locationUpdatesDoNotCauseRequestsWhileStayingPut() {
        HazardRefreshPolicy policy = new HazardRefreshPolicy();
        policy.onAttempt(0L);
        policy.onSuccess(LAT, LON, 100L);
        int requests = 0;
        for (long t = 1_000L; t < 14 * 60_000L; t += 1_000L) {
            if (policy.shouldRefresh(LAT + 100 / METERS_PER_DEGREE_LAT, LON, t)) requests++;
        }
        assertEquals(0, requests);
    }

    @Test
    public void refreshesAfterRealMovementOrEnoughTime() {
        HazardRefreshPolicy policy = new HazardRefreshPolicy();
        policy.onAttempt(0L);
        policy.onSuccess(LAT, LON, 100L);
        double moved = LAT + 900 / METERS_PER_DEGREE_LAT;
        assertFalse("not within the minimum interval", policy.shouldRefresh(moved, LON, 10_000L));
        assertTrue(policy.shouldRefresh(moved, LON, 40_000L));
        assertTrue("time alone refreshes after 15 minutes", policy.shouldRefresh(LAT, LON, 16 * 60_000L));
    }

    @Test
    public void failuresBackOffAndSuccessResetsThem() {
        HazardRefreshPolicy policy = new HazardRefreshPolicy();
        policy.onAttempt(0L);
        policy.onFailure();
        assertFalse(policy.shouldRefresh(LAT, LON, 30_000L));
        assertTrue(policy.shouldRefresh(LAT, LON, 61_000L));
        policy.onAttempt(61_000L);
        policy.onFailure();
        assertFalse("second failure waits longer", policy.shouldRefresh(LAT, LON, 61_000L + 100_000L));
        assertTrue(policy.shouldRefresh(LAT, LON, 61_000L + 121_000L));
        policy.onAttempt(200_000L);
        policy.onSuccess(LAT, LON, 201_000L);
        assertFalse(policy.shouldRefresh(LAT, LON, 240_000L));
    }

    @Test
    public void resetStartsOver() {
        HazardRefreshPolicy policy = new HazardRefreshPolicy();
        policy.onAttempt(0L);
        policy.onSuccess(LAT, LON, 1L);
        policy.reset();
        assertTrue(policy.shouldRefresh(LAT, LON, 2L));
    }

    // ---- words ------------------------------------------------------------------------------------------

    private static Hazard zone(HazardType type) {
        return new Hazard(1, LAT, LON, 30, type, "t");
    }

    @Test
    public void preWarningSpeechNamesTheTypeAndRoundedDistance() {
        String speech = HazardMessages.speech(Collections.singletonList(
                new HazardAlert(HazardAlert.Kind.PRE_WARN, zone(HazardType.BAD_ROAD), 97.0)));
        assertEquals("전방 약 100미터에 노면 불량 위험지역이 있습니다.", speech);
    }

    @Test
    public void entrySpeechNamesTheType() {
        String speech = HazardMessages.speech(Collections.singletonList(
                new HazardAlert(HazardAlert.Kind.ENTER, zone(HazardType.SHARP_CURVE), -5.0)));
        assertEquals("급커브 위험구역에 진입했습니다. 주의하세요.", speech);
    }

    @Test
    public void everyTypeHasWordsAndAnUnknownCodeFallsBackToOther() {
        for (HazardType type : HazardType.values()) {
            assertNotNull(HazardMessages.speech(Collections.singletonList(
                    new HazardAlert(HazardAlert.Kind.PRE_WARN, zone(type), 50.0))));
            assertEquals(type, HazardType.fromCode(type.code()));
        }
        assertEquals(6, HazardType.values().length);
        assertEquals(HazardType.OTHER, HazardType.fromCode("VOLCANO"));
        assertEquals(HazardType.OTHER, HazardType.fromCode(null));
        assertNull(HazardMessages.speech(Collections.<HazardAlert>emptyList()));
        assertNull(HazardMessages.speech(null));
    }

    @Test
    public void distancesAreRoundedToTenMetersAndNeverZero() {
        assertEquals(10, HazardMessages.roundedDistance(0.0));
        assertEquals(10, HazardMessages.roundedDistance(4.0));
        assertEquals(90, HazardMessages.roundedDistance(88.0));
        assertEquals(100, HazardMessages.roundedDistance(99.9));
    }

    @Test
    public void bannerTextMarksUnverifiedReportsAndExtraZones() {
        Hazard unverified = zone(HazardType.BAD_ROAD);
        HazardStatus approaching = new HazardStatus(unverified, false, 100.0, 0);
        assertEquals("🕳 노면 불량 위험지역", HazardMessages.bannerTitle(approaching));
        assertEquals("전방 약 100m · 미검증 제보", HazardMessages.bannerDetail(approaching));

        Hazard verified = zone(HazardType.TRAFFIC);
        verified.verified = true;
        HazardStatus inside = new HazardStatus(verified, true, 0.0, 2);
        assertEquals("🚗 위험구역 진입", HazardMessages.bannerTitle(inside));
        assertEquals("차량 주의 구간입니다. 주의하세요. · 외 2곳", HazardMessages.bannerDetail(inside));
    }

    // ---- report validation ------------------------------------------------------------------------------

    private static String check(Double lat, Double lon, HazardType type, String title, String description, int radius) {
        return HazardReport.validate(lat, lon, type, title, description, radius);
    }

    @Test
    public void aCompleteReportIsAccepted() {
        assertNull(check(LAT, LON, HazardType.BAD_ROAD, "움푹 파인 길", "", 30));
        assertNull(check(LAT, LON, HazardType.OTHER, "a", null, HazardReport.MIN_RADIUS_M));
        assertNull(check(LAT, LON, HazardType.OTHER, "a", "d", HazardReport.MAX_RADIUS_M));
    }

    @Test
    public void incompleteOrOutOfRangeReportsAreRefusedBeforeSending() {
        assertNotNull(check(null, LON, HazardType.OTHER, "a", "", 30));
        assertNotNull(check(95.0, LON, HazardType.OTHER, "a", "", 30));
        assertNotNull(check(0.0, 0.0, HazardType.OTHER, "a", "", 30));
        assertNotNull(check(Double.NaN, LON, HazardType.OTHER, "a", "", 30));
        assertNotNull(check(LAT, LON, null, "a", "", 30));
        assertNotNull(check(LAT, LON, HazardType.OTHER, "   ", "", 30));
        assertNotNull(check(LAT, LON, HazardType.OTHER, null, "", 30));
        assertNotNull(check(LAT, LON, HazardType.OTHER, repeat(HazardReport.MAX_TITLE_LENGTH + 1), "", 30));
        assertNotNull(check(LAT, LON, HazardType.OTHER, "a", repeat(HazardReport.MAX_DESCRIPTION_LENGTH + 1), 30));
        assertNotNull(check(LAT, LON, HazardType.OTHER, "a", "", HazardReport.MIN_RADIUS_M - 1));
        assertNotNull(check(LAT, LON, HazardType.OTHER, "a", "", HazardReport.MAX_RADIUS_M + 1));
    }

    // ---- settings -----------------------------------------------------------------------------------------

    @Test
    public void warnDistanceSnapsToASupportedChoice() {
        assertEquals(100, HazardAlertConfig.sanitizeWarnDistance(100));
        assertEquals(50, HazardAlertConfig.sanitizeWarnDistance(50));
        assertEquals(100, HazardAlertConfig.sanitizeWarnDistance(0));
        assertEquals(100, HazardAlertConfig.sanitizeWarnDistance(99999));
        assertEquals(100, HazardAlertConfig.defaults().warnDistanceM, 0.0);
    }

    private static String repeat(int n) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < n; i++) text.append('가');
        return text.toString();
    }
}
