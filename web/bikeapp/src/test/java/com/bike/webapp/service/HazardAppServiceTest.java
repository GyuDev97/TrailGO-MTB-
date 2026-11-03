package com.bike.webapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.bike.webapp.dto.HazardRequest;
import com.bike.webapp.dto.HazardRow;
import com.bike.webapp.dto.HazardView;
import com.bike.webapp.hazard.HazardException;
import com.bike.webapp.mapper.HazardAdminMapper;
import com.bike.webapp.mapper.HazardAppMapper;

class HazardAppServiceTest {

	private static final double LAT = 37.5;
	private static final double LON = 127.0;
	private static final double METERS_PER_DEGREE_LAT = 111_194.93;

	private HazardAppMapper mapper;
	private HazardAdminMapper adminMapper;
	private HazardAppService service;

	@BeforeEach
	void setUp() {
		mapper = mock(HazardAppMapper.class);
		adminMapper = mock(HazardAdminMapper.class);
		service = new HazardAppService(mapper, adminMapper);
	}

	private static HazardRow row(int id, double metersNorth, int radius, String type, String status, String reporter) {
		HazardRow row = new HazardRow();
		row.setId(id);
		row.setLatitude(LAT + metersNorth / METERS_PER_DEGREE_LAT);
		row.setLongitude(LON);
		row.setRadiusM(radius);
		row.setHazardType(type);
		row.setStatus(status);
		row.setTitle("제목" + id);
		row.setReporter(reporter);
		row.setCreatedAt(new Timestamp(1_000L));
		row.setUpdatedAt(new Timestamp(2_000L));
		return row;
	}

	private void visible(HazardRow... rows) {
		when(mapper.findVisibleInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
				.thenReturn(new ArrayList<HazardRow>(Arrays.asList(rows)));
	}

	private static HazardRequest request() {
		HazardRequest r = new HazardRequest();
		r.setLatitude(LAT);
		r.setLongitude(LON);
		r.setRadiusM(40);
		r.setType("BAD_ROAD");
		r.setTitle("움푹 파인 길");
		r.setDescription("자전거 바퀴가 빠질 정도");
		return r;
	}

	private static HazardException refused(Runnable call) {
		return assertThrows(HazardException.class, call::run);
	}

	// ---- nearby -----------------------------------------------------------------------------------

	@Test
	void nearbyJudgesByTheZoneEdgeNotTheCentre() {
		// centre 129 m away with a 30 m radius: the edge is 99 m away, inside a 100 m query
		visible(row(1, 129, 30, "BAD_ROAD", "PENDING", "a"), row(2, 135, 30, "BAD_ROAD", "PENDING", "a"));
		HazardAppService.Nearby result = service.nearby(LAT, LON, 100, null);
		assertEquals(1, result.getItems().size());
		assertEquals(1, result.getItems().get(0).getId());
		assertEquals(129.0, result.getItems().get(0).getDistanceM(), 0.5);
	}

	@Test
	void nearbyIsSortedByEdgeDistance() {
		visible(row(1, 400, 10, "OTHER", "PENDING", "a"), row(2, 300, 10, "OTHER", "PENDING", "a"),
				row(3, 330, 200, "OTHER", "PENDING", "a"));
		List<HazardView> items = service.nearby(LAT, LON, 1000, null).getItems();
		assertEquals(Arrays.asList(3, 2, 1), Arrays.asList(items.get(0).getId(), items.get(1).getId(), items.get(2).getId()));
	}

	@Test
	void nearbyMarksOnlyApprovedAsVerifiedAndOnlyTheCallersOwnAsMine() {
		visible(row(1, 10, 30, "SLIPPERY", "APPROVED", "me"), row(2, 20, 30, "SLIPPERY", "PENDING", "other"));
		List<HazardView> items = service.nearby(LAT, LON, 500, "me").getItems();
		assertTrue(items.get(0).isVerified());
		assertTrue(items.get(0).isMine());
		assertFalse(items.get(1).isVerified());
		assertFalse(items.get(1).isMine());
		assertEquals("미끄럼 위험", items.get(0).getTypeLabel());
	}

	@Test
	void nearbyDefaultsLegacyRowsToOtherAndTheDefaultRadius() {
		HazardRow legacy = row(1, 10, 0, null, "PENDING", "a");
		visible(legacy);
		HazardView view = service.nearby(LAT, LON, 500, null).getItems().get(0);
		assertEquals("OTHER", view.getType());
		assertEquals(HazardAppService.DEFAULT_RADIUS_M, view.getRadiusM());
	}

	@Test
	void nearbyCapsTheResultCountAndSaysSo() {
		HazardRow[] many = new HazardRow[HazardAppService.MAX_RESULTS + 5];
		for (int i = 0; i < many.length; i++) {
			many[i] = row(i + 1, 10 + i, 10, "OTHER", "PENDING", "a");
		}
		visible(many);
		HazardAppService.Nearby result = service.nearby(LAT, LON, 1000, null);
		assertEquals(HazardAppService.MAX_RESULTS, result.getItems().size());
		assertTrue(result.isTruncated());
		assertEquals(1, result.getItems().get(0).getId()); // nearest kept
	}

	@Test
	void nearbyRejectsBadInput() {
		assertEquals("INVALID_COORDINATE", refused(() -> service.nearby(null, LON, 100, null)).getCode());
		assertEquals("INVALID_COORDINATE", refused(() -> service.nearby(91.0, LON, 100, null)).getCode());
		assertEquals("INVALID_RADIUS", refused(() -> service.nearby(LAT, LON, 0, null)).getCode());
		assertEquals("INVALID_RADIUS", refused(() -> service.nearby(LAT, LON, HazardAppService.MAX_NEARBY_M + 1, null)).getCode());
		verify(mapper, never()).findVisibleInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt());
	}

	@Test
	void nearbyNamesTheZonesThePhoneMustDrop() {
		visible(row(2, 120, 30, "BAD_ROAD", "APPROVED", "a"));
		when(mapper.findHiddenIdsInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
				.thenReturn(Arrays.asList(1, 5));
		HazardAppService.Nearby result = service.nearby(LAT, LON, 500, null);
		assertEquals(Arrays.asList(1, 5), result.getRemovedIds());
		assertEquals(1, result.getItems().size());
	}

	@Test
	void aMergedZoneIsNeitherListedNorEditableByItsReporter() {
		HazardRow merged = row(7, 10, 30, "BAD_ROAD", "MERGED", "rider1");
		when(mapper.findById(7)).thenReturn(merged);
		assertEquals(HttpStatus.NOT_FOUND, refused(() -> service.get(7, "rider1")).getStatus());
		assertEquals(HttpStatus.NOT_FOUND, refused(() -> service.update("rider1", 7, request())).getStatus());
		assertEquals(HttpStatus.NOT_FOUND, refused(() -> service.delete("rider1", 7)).getStatus());
		verify(mapper, never()).updateDanger(anyInt(), anyString(), anyInt());
		verify(mapper, never()).deactivateReview(anyInt());
	}

	@Test
	void nearbyWithNothingAroundReturnsAnEmptyList() {
		visible();
		assertTrue(service.nearby(LAT, LON, null, null).getItems().isEmpty());
	}

	// ---- get ----------------------------------------------------------------------------------------

	@Test
	void hiddenRejectedAndMissingHazardsAreNotFound() {
		when(mapper.findById(1)).thenReturn(row(1, 0, 30, "OTHER", "INACTIVE", "a"));
		when(mapper.findById(2)).thenReturn(row(2, 0, 30, "OTHER", "REJECTED", "a"));
		when(mapper.findById(3)).thenReturn(null);
		for (int id = 1; id <= 3; id++) {
			final int hazard = id;
			assertEquals(HttpStatus.NOT_FOUND, refused(() -> service.get(hazard, null)).getStatus());
		}
	}

	// ---- report -------------------------------------------------------------------------------------

	private void insertWillAssign(final int id) {
		org.mockito.Mockito.doAnswer(invocation -> {
			((HazardRow) invocation.getArgument(0)).setId(id);
			return 1;
		}).when(mapper).insertDanger(any(HazardRow.class), anyString(), anyString());
		when(mapper.findById(id)).thenReturn(row(id, 0, 40, "BAD_ROAD", "PENDING", "rider1"));
		visible();
	}

	@Test
	void reportStoresAnUnverifiedHazardOwnedByTheSignedInRider() {
		insertWillAssign(9);
		HazardView view = service.report("rider1", request());

		org.mockito.ArgumentCaptor<HazardRow> saved = org.mockito.ArgumentCaptor.forClass(HazardRow.class);
		verify(mapper).insertDanger(saved.capture(), eq("37.5000000"), eq("127.0000000"));
		assertEquals("rider1", saved.getValue().getReporter());
		assertEquals(40, saved.getValue().getRadiusM());
		verify(mapper).insertReview(9, "움푹 파인 길", "BAD_ROAD");
		verify(adminMapper).insertHistory(9, "REPORTED", null, "PENDING", "rider:rider1", null);
		assertFalse(view.isVerified());
		assertTrue(view.isMine());
	}

	@Test
	void reportUsesTheDefaultRadiusAndTrimsText() {
		insertWillAssign(9);
		HazardRequest r = request();
		r.setRadiusM(null);
		r.setTitle("  제목  ");
		r.setDescription(null);
		service.report("rider1", r);
		org.mockito.ArgumentCaptor<HazardRow> saved = org.mockito.ArgumentCaptor.forClass(HazardRow.class);
		verify(mapper).insertDanger(saved.capture(), anyString(), anyString());
		assertEquals(30, saved.getValue().getRadiusM());
		assertEquals("", saved.getValue().getDescription());
		verify(mapper).insertReview(9, "제목", "BAD_ROAD");
	}

	@Test
	void reportNeedsALoggedInRider() {
		assertEquals(HttpStatus.UNAUTHORIZED, refused(() -> service.report(null, request())).getStatus());
		assertEquals(HttpStatus.UNAUTHORIZED, refused(() -> service.report("  ", request())).getStatus());
	}

	@Test
	void reportRejectsInvalidInputWithoutTouchingTheDatabase() {
		HazardRequest r = request();
		r.setLatitude(95.0);
		assertEquals("INVALID_COORDINATE", refused(() -> service.report("rider1", r)).getCode());

		HazardRequest zero = request();
		zero.setLatitude(0.0);
		zero.setLongitude(0.0);
		assertEquals("INVALID_COORDINATE", refused(() -> service.report("rider1", zero)).getCode());

		HazardRequest noCoordinates = request();
		noCoordinates.setLongitude(null);
		assertEquals("INVALID_COORDINATE", refused(() -> service.report("rider1", noCoordinates)).getCode());

		HazardRequest badType = request();
		badType.setType("VOLCANO");
		assertEquals("INVALID_TYPE", refused(() -> service.report("rider1", badType)).getCode());

		HazardRequest noType = request();
		noType.setType(null);
		assertEquals("INVALID_TYPE", refused(() -> service.report("rider1", noType)).getCode());

		HazardRequest blankTitle = request();
		blankTitle.setTitle("   ");
		assertEquals("INVALID_TITLE", refused(() -> service.report("rider1", blankTitle)).getCode());

		HazardRequest longTitle = request();
		longTitle.setTitle(repeat('가', HazardAppService.MAX_TITLE_LENGTH + 1));
		assertEquals("INVALID_TITLE", refused(() -> service.report("rider1", longTitle)).getCode());

		HazardRequest longDescription = request();
		longDescription.setDescription(repeat('a', HazardAppService.MAX_DESCRIPTION_LENGTH + 1));
		assertEquals("INVALID_DESCRIPTION", refused(() -> service.report("rider1", longDescription)).getCode());

		HazardRequest small = request();
		small.setRadiusM(9);
		assertEquals("INVALID_RADIUS", refused(() -> service.report("rider1", small)).getCode());
		HazardRequest large = request();
		large.setRadiusM(201);
		assertEquals("INVALID_RADIUS", refused(() -> service.report("rider1", large)).getCode());

		verify(mapper, never()).insertDanger(any(HazardRow.class), anyString(), anyString());
	}

	@Test
	void radiusLimitsAreInclusive() {
		insertWillAssign(9);
		HazardRequest min = request();
		min.setRadiusM(10);
		service.report("rider1", min);
		HazardRequest max = request();
		max.setRadiusM(200);
		service.report("rider1", max);
		verify(mapper, org.mockito.Mockito.times(2)).insertDanger(any(HazardRow.class), anyString(), anyString());
	}

	@Test
	void reportRefusesADuplicateOfTheSameTypeNearby() {
		insertWillAssign(9);
		visible(row(4, 10, 30, "BAD_ROAD", "PENDING", "someone"));
		HazardException e = refused(() -> service.report("rider1", request()));
		assertEquals(HttpStatus.CONFLICT, e.getStatus());
		assertEquals("DUPLICATE_HAZARD", e.getCode());
		assertEquals(4, e.body().get("hazardId"));
		verify(mapper, never()).insertDanger(any(HazardRow.class), anyString(), anyString());
	}

	@Test
	void aDifferentTypeOrAFarawayHazardIsNotADuplicate() {
		insertWillAssign(9);
		visible(row(4, 10, 30, "TRAFFIC", "PENDING", "x"), row(5, 40, 30, "BAD_ROAD", "PENDING", "x"));
		service.report("rider1", request());
		verify(mapper).insertDanger(any(HazardRow.class), anyString(), anyString());
	}

	// ---- update / delete -----------------------------------------------------------------------------

	@Test
	void ownerCanEditAPendingReport() {
		when(mapper.findById(5)).thenReturn(row(5, 0, 30, "OTHER", "PENDING", "rider1"));
		HazardRequest r = request();
		r.setLatitude(10.0); // ignored: a hazard does not move
		service.update("rider1", 5, r);
		verify(adminMapper).ensureReview(5);
		verify(mapper).updateDanger(5, "자전거 바퀴가 빠질 정도", 40);
		verify(mapper).updateReview(5, "움푹 파인 길", "BAD_ROAD");
	}

	@Test
	void nobodyElseCanEditOrWithdraw() {
		when(mapper.findById(5)).thenReturn(row(5, 0, 30, "OTHER", "PENDING", "owner"));
		assertEquals(HttpStatus.FORBIDDEN, refused(() -> service.update("intruder", 5, request())).getStatus());
		assertEquals(HttpStatus.FORBIDDEN, refused(() -> service.delete("intruder", 5)).getStatus());
		assertEquals(HttpStatus.UNAUTHORIZED, refused(() -> service.delete(null, 5)).getStatus());
		verify(mapper, never()).updateDanger(anyInt(), anyString(), anyInt());
		verify(mapper, never()).deactivateReview(anyInt());
	}

	@Test
	void approvedReportsCannotBeEditedByTheRider() {
		when(mapper.findById(5)).thenReturn(row(5, 0, 30, "OTHER", "APPROVED", "rider1"));
		assertEquals("LOCKED", refused(() -> service.update("rider1", 5, request())).getCode());
	}

	@Test
	void withdrawingHidesTheHazardAndKeepsAHistoryEntry() {
		when(mapper.findById(5)).thenReturn(row(5, 0, 30, "OTHER", "APPROVED", "rider1"));
		service.delete("rider1", 5);
		verify(mapper).deactivateReview(5);
		verify(mapper).deactivateLegacy(5);
		verify(adminMapper).insertHistory(5, "WITHDRAWN", "APPROVED", "INACTIVE", "rider:rider1", null);
	}

	@Test
	void withdrawingAnAlreadyHiddenHazardIsNotFound() {
		when(mapper.findById(5)).thenReturn(row(5, 0, 30, "OTHER", "INACTIVE", "rider1"));
		assertEquals(HttpStatus.NOT_FOUND, refused(() -> service.delete("rider1", 5)).getStatus());
		when(mapper.findById(6)).thenReturn(null);
		assertEquals(HttpStatus.NOT_FOUND, refused(() -> service.update("rider1", 6, request())).getStatus());
	}

	private static String repeat(char c, int n) {
		char[] chars = new char[n];
		Arrays.fill(chars, c);
		return new String(chars);
	}
}
