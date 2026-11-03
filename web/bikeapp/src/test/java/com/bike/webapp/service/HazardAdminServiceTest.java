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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.bike.webapp.dto.AdminHazardRequest;
import com.bike.webapp.dto.HazardMergeRequest;
import com.bike.webapp.dto.HazardReview;
import com.bike.webapp.dto.HazardRow;
import com.bike.webapp.hazard.HazardException;
import com.bike.webapp.mapper.HazardAdminMapper;
import com.bike.webapp.mapper.HazardAppMapper;

class HazardAdminServiceTest {

	private HazardAdminMapper mapper;
	private HazardAppMapper appMapper;
	private HazardAdminService service;

	@BeforeEach
	void setUp() {
		mapper = mock(HazardAdminMapper.class);
		appMapper = mock(HazardAppMapper.class);
		service = new HazardAdminService(mapper, appMapper);
		when(appMapper.findVisibleInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
				.thenReturn(Collections.<HazardRow>emptyList());
		when(mapper.transitionStatus(anyInt(), anyString(), anyString(), anyString(), any())).thenReturn(1);
		when(mapper.mergeHazard(anyInt(), anyInt())).thenReturn(1);
	}

	private HazardReview zone(int id, String status, String type) {
		HazardReview zone = new HazardReview();
		zone.setId(id);
		zone.setStatus(status);
		zone.setHazardType(type);
		zone.setTitle("제목" + id);
		zone.setReporter("rider" + id);
		zone.setLatitude("37.5000000");
		zone.setLongitude("127.0000000");
		zone.setRadiusM(40);
		zone.setDescription("설명" + id);
		zone.setCreatedAt(LocalDateTime.of(2026, 11, 1, 10, 0));
		when(mapper.findById(id)).thenReturn(zone);
		return zone;
	}

	private void found(HazardReview... zones) {
		when(mapper.findByIds(any())).thenReturn(new ArrayList<HazardReview>(Arrays.asList(zones)));
	}

	private static HazardException refused(Runnable call) {
		return assertThrows(HazardException.class, call::run);
	}

	// ---- approve / reject / hide: the approve-button bug ---------------------------------------------------

	@Test
	void approvingAPendingZoneChangesStatusLegacyStatusAndHistory() {
		zone(1, "PENDING", "BAD_ROAD");
		service.approve(1, "root");
		verify(mapper).ensureReview(1);
		verify(mapper).transitionStatus(1, "PENDING", "APPROVED", "root", null);
		verify(mapper).updateLegacyStatus(1, "1");
		verify(mapper).insertHistory(1, "APPROVED", "PENDING", "APPROVED", "root", null);
	}

	@Test
	void approvingTwiceIsRefusedAndChangesNothing() {
		zone(1, "APPROVED", "BAD_ROAD");
		HazardException e = refused(() -> service.approve(1, "root"));
		assertEquals(HttpStatus.CONFLICT, e.getStatus());
		assertEquals("ALREADY_IN_STATUS", e.getCode());
		verify(mapper, never()).transitionStatus(anyInt(), anyString(), anyString(), anyString(), any());
		verify(mapper, never()).insertHistory(anyInt(), anyString(), any(), any(), any(), any());
	}

	@Test
	void aStalePageCannotOverwriteAChangeMadeElsewhere() {
		zone(1, "PENDING", "BAD_ROAD");
		when(mapper.transitionStatus(1, "PENDING", "APPROVED", "root", null)).thenReturn(0); // someone else was faster
		assertEquals("STALE", refused(() -> service.approve(1, "root")).getCode());
		verify(mapper, never()).updateLegacyStatus(anyInt(), anyString());
		verify(mapper, never()).insertHistory(anyInt(), anyString(), any(), any(), any(), any());
	}

	@Test
	void anApprovedZoneCannotBeRejectedOnlyHidden() {
		zone(1, "APPROVED", "OTHER");
		assertEquals("INVALID_TRANSITION", refused(() -> service.reject(1, "root", "x")).getCode());
		service.hide(1, "root", "중단");
		verify(mapper).transitionStatus(1, "APPROVED", "INACTIVE", "root", "중단");
		verify(mapper).updateLegacyStatus(1, "4");
		verify(mapper).insertHistory(1, "HIDDEN", "APPROVED", "INACTIVE", "root", "중단");
	}

	@Test
	void rejectingKeepsTheReasonAndRestoringReopensForReview() {
		zone(1, "PENDING", "OTHER");
		service.reject(1, "root", "  사실 아님 ");
		verify(mapper).transitionStatus(1, "PENDING", "REJECTED", "root", "사실 아님");
		verify(mapper).updateLegacyStatus(1, "2");

		zone(2, "INACTIVE", "OTHER");
		service.restore(2, "root");
		verify(mapper).transitionStatus(2, "INACTIVE", "PENDING", "root", null);
		verify(mapper).insertHistory(2, "RESTORED", "INACTIVE", "PENDING", "root", null);
	}

	@Test
	void aMissingZoneIsNotFound() {
		when(mapper.findById(9)).thenReturn(null);
		assertEquals(HttpStatus.NOT_FOUND, refused(() -> service.approve(9, "root")).getStatus());
	}

	@Test
	void aMergedZoneCannotBeChangedOrEdited() {
		HazardReview merged = zone(1, "MERGED", "OTHER");
		merged.setMergedInto(5);
		assertEquals("MERGED_LOCKED", refused(() -> service.approve(1, "root")).getCode());
		assertEquals("MERGED_LOCKED", refused(() -> service.hide(1, "root", null)).getCode());
		assertEquals("MERGED_LOCKED", refused(() -> service.update(1, new HazardReview(), "root")).getCode());
		verify(mapper, never()).updateDangerDetails(any());
	}

	// ---- editing --------------------------------------------------------------------------------------------

	@Test
	void anEditThatDoesNotMentionTheRadiusKeepsIt() {
		zone(1, "PENDING", "BAD_ROAD");
		HazardReview changes = new HazardReview();
		changes.setTitle("새 제목");
		service.update(1, changes, "root");
		ArgumentCaptor<HazardReview> saved = ArgumentCaptor.forClass(HazardReview.class);
		verify(mapper).updateDangerDetails(saved.capture());
		assertEquals(Integer.valueOf(40), saved.getValue().rawRadiusM());
		assertEquals("새 제목", saved.getValue().getTitle());
	}

	@Test
	void editValidationRejectsBadTypeRadiusAndCoordinates() {
		zone(1, "PENDING", "BAD_ROAD");
		HazardReview type = new HazardReview();
		type.setHazardType("VOLCANO");
		assertThrows(IllegalArgumentException.class, () -> service.update(1, type, "root"));
		HazardReview radius = new HazardReview();
		radius.setRadiusM(500);
		assertThrows(IllegalArgumentException.class, () -> service.update(1, radius, "root"));
		HazardReview coordinate = new HazardReview();
		coordinate.setLatitude("95");
		coordinate.setLongitude("127");
		assertThrows(IllegalArgumentException.class, () -> service.update(1, coordinate, "root"));
		verify(mapper, never()).updateDangerDetails(any());
	}

	@Test
	void aZoneWithAnOlderTypeCanBeSavedUnchangedButNotSwitchedToAnotherOldOne() {
		zone(1, "PENDING", "ROCKFALL");
		HazardReview same = new HazardReview();
		same.setHazardType("ROCKFALL");
		service.update(1, same, "root");
		HazardReview appType = new HazardReview();
		appType.setHazardType("SHARP_CURVE");
		service.update(1, appType, "root");
		verify(mapper, times(2)).updateReviewDetails(any());
	}

	@Test
	void aStatusInAnEditGoesThroughThePolicy() {
		zone(1, "APPROVED", "OTHER");
		HazardReview changes = new HazardReview();
		changes.setStatus("REJECTED");
		assertEquals("INVALID_TRANSITION", refused(() -> service.update(1, changes, "root")).getCode());
	}

	// ---- registering directly ------------------------------------------------------------------------------

	private static AdminHazardRequest createRequest() {
		AdminHazardRequest request = new AdminHazardRequest();
		request.setLatitude(37.5);
		request.setLongitude(127.0);
		request.setRadiusM(45);
		request.setType("BAD_ROAD");
		request.setTitle("  운영자 등록  ");
		request.setDescription("설명");
		request.setAddress("대구 수성구");
		return request;
	}

	private void insertAssigns(final int id) {
		doAnswer(invocation -> {
			((HazardRow) invocation.getArgument(0)).setId(id);
			return 1;
		}).when(mapper).insertAdminDanger(any(HazardRow.class), anyString(), anyString());
		zone(id, "APPROVED", "BAD_ROAD").setCreatedBy("root");
	}

	@Test
	void anAdminRegistrationIsActiveApprovedAttributedAndLogged() {
		insertAssigns(30);
		HazardReview created = service.createByAdmin(createRequest(), "root");
		assertEquals(30, created.getId().intValue());

		ArgumentCaptor<HazardRow> row = ArgumentCaptor.forClass(HazardRow.class);
		verify(mapper).insertAdminDanger(row.capture(), eq("37.5000000"), eq("127.0000000"));
		assertEquals("admin:root", row.getValue().getReporter());
		assertEquals(45, row.getValue().getRadiusM());
		assertEquals("대구 수성구", row.getValue().getAddress());
		verify(mapper).insertAdminReview(30, "운영자 등록", "BAD_ROAD", "root");
		verify(mapper).insertHistory(30, "ADMIN_CREATE", null, "APPROVED", "root", "운영자 직접 등록");
	}

	@Test
	void registrationValidatesLikeTheAppDoes() {
		AdminHazardRequest noPlace = createRequest();
		noPlace.setLatitude(0.0);
		noPlace.setLongitude(0.0);
		assertThrows(IllegalArgumentException.class, () -> service.createByAdmin(noPlace, "root"));
		AdminHazardRequest badType = createRequest();
		badType.setType("ROCKFALL"); // older admin-only types are not offered for new zones
		assertThrows(IllegalArgumentException.class, () -> service.createByAdmin(badType, "root"));
		AdminHazardRequest noTitle = createRequest();
		noTitle.setTitle(" ");
		assertThrows(IllegalArgumentException.class, () -> service.createByAdmin(noTitle, "root"));
		AdminHazardRequest small = createRequest();
		small.setRadiusM(9);
		assertThrows(IllegalArgumentException.class, () -> service.createByAdmin(small, "root"));
		AdminHazardRequest large = createRequest();
		large.setRadiusM(201);
		assertThrows(IllegalArgumentException.class, () -> service.createByAdmin(large, "root"));
		verify(mapper, never()).insertAdminDanger(any(HazardRow.class), anyString(), anyString());
	}

	@Test
	void registrationNearAnExistingZoneNeedsConfirmation() {
		insertAssigns(30);
		HazardRow existing = new HazardRow();
		existing.setId(4);
		existing.setLatitude(37.5 + 20 / 111_195.0);
		existing.setLongitude(127.0);
		existing.setRadiusM(30);
		existing.setHazardType("BAD_ROAD");
		existing.setTitle("근처 제보");
		existing.setStatus("PENDING");
		when(appMapper.findVisibleInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
				.thenReturn(Collections.singletonList(existing));

		HazardException e = refused(() -> service.createByAdmin(createRequest(), "root"));
		assertEquals(HttpStatus.CONFLICT, e.getStatus());
		assertEquals("DUPLICATE_CANDIDATES", e.getCode());
		assertTrue(e.body().containsKey("candidates"));
		verify(mapper, never()).insertAdminDanger(any(HazardRow.class), anyString(), anyString());

		AdminHazardRequest confirmed = createRequest();
		confirmed.setAcknowledgeDuplicates(true);
		service.createByAdmin(confirmed, "root");
		verify(mapper).insertAdminDanger(any(HazardRow.class), anyString(), anyString());
	}

	// ---- merging ---------------------------------------------------------------------------------------------

	private static HazardMergeRequest mergeRequest(int canonical, Integer... sources) {
		HazardMergeRequest request = new HazardMergeRequest();
		request.setCanonicalId(canonical);
		request.setSourceIds(new ArrayList<Integer>(Arrays.asList(sources)));
		return request;
	}

	@Test
	void mergingFoldsSourcesIntoTheRepresentativeAndKeepsTheOriginals() {
		HazardReview canonical = zone(1, "APPROVED", "BAD_ROAD");
		HazardReview second = zone(2, "PENDING", "BAD_ROAD");
		HazardReview third = zone(3, "REVIEWING", "BAD_ROAD");
		found(canonical, second, third);

		HazardReview result = service.merge(mergeRequest(1, 2, 3), "root");
		assertEquals(1, result.getId().intValue());

		verify(mapper).mergeHazard(2, 1);
		verify(mapper).mergeHazard(3, 1);
		verify(mapper).updateLegacyStatus(2, "4");
		verify(mapper).updateLegacyStatus(3, "4");
		verify(mapper).repointMerged(2, 1);
		verify(mapper).repointMerged(3, 1);
		verify(mapper).insertHistory(2, "MERGE", "PENDING", "MERGED", "root", "위험지역 #1로 병합");
		verify(mapper).insertHistory(3, "MERGE", "REVIEWING", "MERGED", "root", "위험지역 #1로 병합");
		verify(mapper).insertHistory(eq(1), eq("MERGE_RECEIVED"), any(), any(), eq("root"), anyString());

		ArgumentCaptor<String> group = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> before = ArgumentCaptor.forClass(String.class);
		verify(mapper, times(2)).insertMerge(group.capture(), eq(1), anyInt(), eq("root"), before.capture(), anyString(), anyString());
		assertEquals(group.getAllValues().get(0), group.getAllValues().get(1)); // one operation, one group
		// the original report and its reporter are preserved in the audit snapshot
		assertTrue(before.getAllValues().get(0).contains("\"reporter\":\"rider2\""));
		assertTrue(before.getAllValues().get(0).contains("\"title\":\"제목2\""));
	}

	@Test
	void anEditOfTheRepresentativeIsAppliedAndRecordedBeforeAfter() {
		HazardReview canonical = zone(1, "APPROVED", "BAD_ROAD");
		found(canonical, zone(2, "PENDING", "BAD_ROAD"));
		HazardMergeRequest request = mergeRequest(1, 2);
		request.setTitle("합쳐진 제목");
		request.setRadiusM(60);
		service.merge(request, "root");

		ArgumentCaptor<HazardReview> edits = ArgumentCaptor.forClass(HazardReview.class);
		verify(mapper).updateDangerDetails(edits.capture());
		assertEquals("합쳐진 제목", edits.getValue().getTitle());
		assertEquals(Integer.valueOf(60), edits.getValue().rawRadiusM());
		verify(mapper).insertMerge(anyString(), eq(1), eq(2), eq("root"), anyString(), anyString(), anyString());
	}

	@Test
	void withoutEditsTheRepresentativeIsLeftAsItIs() {
		found(zone(1, "APPROVED", "BAD_ROAD"), zone(2, "PENDING", "BAD_ROAD"));
		service.merge(mergeRequest(1, 2), "root");
		verify(mapper, never()).updateDangerDetails(any());
		verify(mapper, never()).updateReviewDetails(any());
	}

	@Test
	void zonesOfDifferentTypesNeedAnExplicitConfirmation() {
		found(zone(1, "APPROVED", "BAD_ROAD"), zone(2, "PENDING", "TRAFFIC"));
		HazardException e = refused(() -> service.merge(mergeRequest(1, 2), "root"));
		assertEquals("TYPE_MISMATCH", e.getCode());
		verify(mapper, never()).mergeHazard(anyInt(), anyInt());

		HazardMergeRequest confirmed = mergeRequest(1, 2);
		confirmed.setConfirmTypeMismatch(true);
		service.merge(confirmed, "root");
		verify(mapper).mergeHazard(2, 1);
	}

	@Test
	void theRepresentativeCannotBeASourceAndSelectionsMustBeValid() {
		zone(1, "APPROVED", "OTHER");
		assertThrows(IllegalArgumentException.class, () -> service.merge(mergeRequest(1, 1), "root"));
		assertThrows(IllegalArgumentException.class, () -> service.merge(mergeRequest(1), "root"));
		assertThrows(IllegalArgumentException.class, () -> service.merge(mergeRequest(1, 2, 2), "root"));
		HazardMergeRequest noCanonical = new HazardMergeRequest();
		noCanonical.setSourceIds(Collections.singletonList(2));
		assertThrows(IllegalArgumentException.class, () -> service.merge(noCanonical, "root"));
		Integer[] tooMany = new Integer[HazardAdminService.MAX_MERGE_SOURCES + 1];
		for (int i = 0; i < tooMany.length; i++) tooMany[i] = 100 + i;
		assertThrows(IllegalArgumentException.class, () -> service.merge(mergeRequest(1, tooMany), "root"));
		verify(mapper, never()).mergeHazard(anyInt(), anyInt());
	}

	@Test
	void anAlreadyMergedZoneCannotBeMergedAgainOrBecomeTheRepresentative() {
		HazardReview canonical = zone(1, "APPROVED", "OTHER");
		HazardReview merged = zone(2, "MERGED", "OTHER");
		merged.setMergedInto(9);
		found(canonical, merged);
		assertEquals("ALREADY_MERGED", refused(() -> service.merge(mergeRequest(1, 2), "root")).getCode());

		// a zone that was merged elsewhere must not be chosen as the representative either (no chains, no cycles)
		HazardReview child = zone(3, "MERGED", "OTHER");
		child.setMergedInto(1);
		HazardReview other = zone(4, "PENDING", "OTHER");
		found(child, other);
		assertEquals("INVALID_CANONICAL", refused(() -> service.merge(mergeRequest(3, 4), "root")).getCode());
		verify(mapper, never()).mergeHazard(anyInt(), anyInt());
	}

	@Test
	void aHiddenOrRejectedRepresentativeIsRefused() {
		found(zone(1, "INACTIVE", "OTHER"), zone(2, "PENDING", "OTHER"));
		assertEquals("INVALID_CANONICAL", refused(() -> service.merge(mergeRequest(1, 2), "root")).getCode());
	}

	@Test
	void anUnknownZoneIsNotFound() {
		zone(1, "APPROVED", "OTHER");
		found(zone(1, "APPROVED", "OTHER")); // 2 is missing from the result
		assertEquals(HttpStatus.NOT_FOUND, refused(() -> service.merge(mergeRequest(1, 2), "root")).getStatus());
	}

	@Test
	void aFailureHalfwayAbortsWithoutWritingTheAuditTrail() {
		found(zone(1, "APPROVED", "OTHER"), zone(2, "PENDING", "OTHER"), zone(3, "PENDING", "OTHER"));
		when(mapper.mergeHazard(3, 1)).thenReturn(0); // zone 3 was changed by someone else meanwhile
		HazardException e = refused(() -> service.merge(mergeRequest(1, 2, 3), "root"));
		assertEquals("STALE", e.getCode());
		// the exception propagates out of the @Transactional service, which rolls the whole merge back
		verify(mapper, never()).insertMerge(anyString(), anyInt(), anyInt(), anyString(), any(), any(), any());
		assertTrue(RuntimeException.class.isAssignableFrom(HazardException.class));
		assertTrue(HazardAdminService.class.isAnnotationPresent(Transactional.class));
	}

	@Test
	void aDatabaseErrorWhileRecordingPropagatesToo() {
		found(zone(1, "APPROVED", "OTHER"), zone(2, "PENDING", "OTHER"));
		doAnswer(invocation -> {
			throw new org.springframework.dao.DuplicateKeyException("uk_hazard_merge_merged");
		}).when(mapper).insertMerge(anyString(), anyInt(), anyInt(), anyString(), any(), any(), any());
		assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> service.merge(mergeRequest(1, 2), "root"));
	}

	@Test
	void theMergeEditsAreValidated() {
		found(zone(1, "APPROVED", "OTHER"), zone(2, "PENDING", "OTHER"));
		HazardMergeRequest badRadius = mergeRequest(1, 2);
		badRadius.setRadiusM(500);
		assertEquals("INVALID_RADIUS", refused(() -> service.merge(badRadius, "root")).getCode());
		HazardMergeRequest badType = mergeRequest(1, 2);
		badType.setHazardType("VOLCANO");
		assertEquals("INVALID_TYPE", refused(() -> service.merge(badType, "root")).getCode());
		HazardMergeRequest badPlace = mergeRequest(1, 2);
		badPlace.setLatitude("999");
		badPlace.setLongitude("1");
		assertEquals("INVALID_COORDINATE", refused(() -> service.merge(badPlace, "root")).getCode());
	}

	@Test
	void thePreviewExplainsWithoutChangingAnything() {
		found(zone(1, "PENDING", "BAD_ROAD"), zone(2, "APPROVED", "TRAFFIC"));
		Map<String, Object> preview = service.previewMerge(mergeRequest(1, 2));
		assertEquals(Boolean.TRUE, preview.get("canMerge"));
		assertEquals(Boolean.TRUE, preview.get("needsTypeConfirmation"));
		@SuppressWarnings("unchecked")
		List<Map<String, String>> warnings = (List<Map<String, String>>) preview.get("warnings");
		assertEquals(2, warnings.size()); // different types, and an approved zone folded into an unapproved one
		verify(mapper, never()).mergeHazard(anyInt(), anyInt());
		verify(mapper, never()).insertMerge(anyString(), anyInt(), anyInt(), anyString(), any(), any(), any());
	}

	@Test
	void thePreviewReportsBlockingProblemsAsErrors() {
		HazardReview merged = zone(2, "MERGED", "OTHER");
		merged.setMergedInto(9);
		found(zone(1, "APPROVED", "OTHER"), merged);
		Map<String, Object> preview = service.previewMerge(mergeRequest(1, 2));
		assertFalse((Boolean) preview.get("canMerge"));
		assertFalse(((List<?>) preview.get("errors")).isEmpty());
	}

	@Test
	void duplicatesOfAZoneNeverIncludeItself() {
		zone(1, "PENDING", "BAD_ROAD");
		HazardRow itself = new HazardRow();
		itself.setId(1);
		itself.setLatitude(37.5);
		itself.setLongitude(127.0);
		itself.setRadiusM(40);
		itself.setHazardType("BAD_ROAD");
		HazardRow neighbour = new HazardRow();
		neighbour.setId(2);
		neighbour.setLatitude(37.5 + 30 / 111_195.0);
		neighbour.setLongitude(127.0);
		neighbour.setRadiusM(30);
		neighbour.setHazardType("BAD_ROAD");
		neighbour.setTitle("이웃");
		neighbour.setStatus("PENDING");
		when(appMapper.findVisibleInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
				.thenReturn(Arrays.asList(itself, neighbour));
		List<Map<String, Object>> candidates = service.duplicatesOf(1);
		assertEquals(1, candidates.size());
		assertEquals(2, candidates.get(0).get("id"));
	}
}
