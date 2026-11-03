package com.bike.webapp.controller.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.bike.webapp.dto.HazardMergeRecord;
import com.bike.webapp.dto.HazardReview;
import com.bike.webapp.dto.HazardStats;

/**
 * Renders the real admin templates with sample data, so a typo in a template or an expression is caught
 * without a running server, and the button rules (no approve button on an approved zone) are checked on the HTML.
 */
class AdminHazardTemplatesTest {

	private SpringTemplateEngine engine;

	@BeforeEach
	void setUp() {
		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		resolver.setPrefix("templates/");
		resolver.setSuffix(".html");
		resolver.setCharacterEncoding("UTF-8");
		engine = new SpringTemplateEngine();
		engine.setTemplateResolver(resolver);
		engine.addDialect(new Java8TimeDialect());
	}

	private String render(String template, Map<String, Object> variables) {
		MockServletContext servletContext = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		MockHttpServletResponse response = new MockHttpServletResponse();
		Map<String, Object> model = new HashMap<String, Object>(variables);
		Map<String, String> csrf = new HashMap<String, String>();
		csrf.put("token", "t");
		csrf.put("headerName", "X-CSRF-TOKEN");
		model.put("_csrf", csrf);
		model.putIfAbsent("kakaoMapAppKey", "");
		model.putIfAbsent("activeMenu", "hazards");
		return engine.process(template, new WebContext(request, response, servletContext, Locale.KOREA, model));
	}

	private static HazardReview zone(int id, String status, String type, Integer mergedInto) {
		HazardReview zone = new HazardReview();
		zone.setId(id);
		zone.setStatus(status);
		zone.setHazardType(type);
		zone.setTitle("제목" + id);
		zone.setReporter("rider" + id);
		zone.setLatitude("37.5000000");
		zone.setLongitude("127.0000000");
		zone.setRadiusM(40);
		zone.setAddress("대구");
		zone.setDescription("설명" + id);
		zone.setMergedInto(mergedInto);
		zone.setCreatedAt(LocalDateTime.of(2026, 11, 1, 10, 0));
		zone.setConfirmCount(0);
		zone.setReportCount(0);
		return zone;
	}

	private Map<String, Object> detailModel(HazardReview zone) {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("hazard", zone);
		model.put("mergedChildren", Collections.<HazardReview>emptyList());
		return model;
	}

	// ---- the approve button -------------------------------------------------------------------------------

	@Test
	void aPendingZoneShowsApproveAndReject() {
		String html = render("admin/hazards/detail", detailModel(zone(1, "PENDING", "BAD_ROAD", null)));
		assertTrue(html.contains("data-action=\"approve\""));
		assertTrue(html.contains("data-action=\"reject\""));
		assertTrue(html.contains("data-action=\"hide\""));
		assertTrue(html.contains("승인 대기"));
		assertTrue(html.contains("미검증"));
	}

	@Test
	void anApprovedZoneShowsApprovedAndNoApproveOrRejectButton() {
		String html = render("admin/hazards/detail", detailModel(zone(1, "APPROVED", "BAD_ROAD", null)));
		assertFalse(html.contains("data-action=\"approve\""));
		assertFalse(html.contains("data-action=\"reject\""));
		assertTrue(html.contains("승인됨"));
		assertTrue(html.contains("data-action=\"hide\""));
	}

	@Test
	void aRejectedZoneShowsRejectedAndOnlyReopenAndHide() {
		String html = render("admin/hazards/detail", detailModel(zone(1, "REJECTED", "BAD_ROAD", null)));
		assertFalse(html.contains("data-action=\"approve\""));
		assertFalse(html.contains("data-action=\"reject\""));
		assertTrue(html.contains("거절됨"));
		assertTrue(html.contains("data-action=\"restore\""));
	}

	@Test
	void aHiddenZoneShowsHiddenAndOnlyRestore() {
		String html = render("admin/hazards/detail", detailModel(zone(1, "INACTIVE", "BAD_ROAD", null)));
		assertFalse(html.contains("data-action=\"approve\""));
		assertFalse(html.contains("data-action=\"hide\""));
		assertTrue(html.contains("숨김"));
		assertTrue(html.contains("data-action=\"restore\""));
	}

	@Test
	void aMergedZoneHasNoActionsAndPointsToTheRepresentative() {
		String html = render("admin/hazards/detail", detailModel(zone(2, "MERGED", "BAD_ROAD", 1)));
		for (String action : new String[] { "approve", "reject", "hide", "restore", "save", "duplicates" }) {
			assertFalse(html.contains("data-action=\"" + action + "\""), action);
		}
		assertTrue(html.contains("병합됨"));
		assertTrue(html.contains("/admin/hazards/1"));
	}

	@Test
	void anAdminRegisteredZoneIsMarkedAsOperatorConfirmed() {
		HazardReview zone = zone(3, "APPROVED", "SLIPPERY", null);
		zone.setSource("ADMIN");
		String html = render("admin/hazards/detail", detailModel(zone));
		assertTrue(html.contains("운영자 등록"));
		assertTrue(html.contains("운영자가 직접 등록해 확인한"));
	}

	@Test
	void theTypeListOffersTheSixAppTypesAndKeepsAnOlderOne() {
		String html = render("admin/hazards/detail", detailModel(zone(1, "PENDING", "ROCKFALL", null)));
		for (String code : new String[] { "STEEP_SLOPE", "BAD_ROAD", "TRAFFIC", "SLIPPERY", "SHARP_CURVE", "OTHER" }) {
			assertTrue(html.contains("value=\"" + code + "\""), code);
		}
		assertTrue(html.contains("낙석 (이전 유형)"));
	}

	@Test
	void theDetailPageListsTheOriginalsMergedIntoIt() {
		Map<String, Object> model = detailModel(zone(1, "APPROVED", "BAD_ROAD", null));
		model.put("mergedChildren", Arrays.asList(zone(2, "MERGED", "BAD_ROAD", 1), zone(3, "MERGED", "BAD_ROAD", 1)));
		String html = render("admin/hazards/detail", model);
		assertTrue(html.contains("병합된 원본 2건"));
		assertTrue(html.contains("rider2"));
	}

	// ---- the other pages -------------------------------------------------------------------------------------

	@Test
	void theListHasFiltersSelectionAndTheRegisterButton() {
		Map<String, Object> model = new HashMap<String, Object>();
		HazardReview merged = zone(2, "MERGED", "BAD_ROAD", 1);
		model.put("hazards", Arrays.asList(zone(1, "APPROVED", "BAD_ROAD", null), merged));
		HazardStats stats = new HazardStats();
		model.put("stats", stats);
		model.put("sort", "newest");
		String html = render("admin/hazards/list", model);
		assertTrue(html.contains("/admin/hazards/new"));
		assertTrue(html.contains("name=\"source\""));
		assertTrue(html.contains("value=\"MERGED\""));
		assertTrue(html.contains("id=\"merge-selected\""));
		assertTrue(html.contains("대표 #1"));
		// a merged zone cannot be ticked for another merge
		assertTrue(html.matches("(?s).*value=\"2\"[^>]*disabled.*") || html.matches("(?s).*disabled[^>]*value=\"2\".*"));
	}

	@Test
	void theRegistrationPageRenders() {
		String html = render("admin/hazards/new", new HashMap<String, Object>());
		assertTrue(html.contains("hazard-create-form"));
		assertTrue(html.contains("data-action=\"create\""));
		assertTrue(html.contains("data-action=\"check-duplicates\""));
		assertTrue(html.contains("value=\"SHARP_CURVE\""));
		assertTrue(html.contains("max=\"200\""));
	}

	@Test
	void theMergePageShowsEachZoneWithARepresentativeChoice() {
		Map<String, Object> model = new HashMap<String, Object>();
		HazardReview merged = zone(3, "MERGED", "BAD_ROAD", 1);
		model.put("zones", Arrays.asList(zone(1, "APPROVED", "BAD_ROAD", null), zone(2, "PENDING", "BAD_ROAD", null), merged));
		String html = render("admin/hazards/merge", model);
		assertTrue(html.contains("canonical-radio"));
		assertTrue(html.contains("data-id=\"1\""));
		assertTrue(html.contains("이미 병합됨"));
		assertTrue(html.contains("data-action=\"merge-confirm\""));
		assertTrue(html.contains("병합 결과 미리보기"));
	}

	@Test
	void theMergePageAsksForMoreZonesWhenThereIsOnlyOne() {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("zones", Collections.singletonList(zone(1, "APPROVED", "BAD_ROAD", null)));
		assertTrue(render("admin/hazards/merge", model).contains("2건 이상"));
	}

	@Test
	void theMergeHistoryPageRenders() {
		HazardMergeRecord record = new HazardMergeRecord();
		record.setId(1L);
		record.setCanonicalId(1);
		record.setMergedId(2);
		record.setAdminId("root");
		record.setMergedAt(LocalDateTime.of(2026, 11, 2, 9, 30));
		record.setCanonicalTitle("대표");
		record.setMergedTitle("원본");
		record.setMergedBefore("{\"reporter\":\"rider2\"}");
		List<HazardMergeRecord> merges = new ArrayList<HazardMergeRecord>(Collections.singletonList(record));
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("merges", merges);
		String html = render("admin/hazards/merges", model);
		assertTrue(html.contains("2026-11-02 09:30"));
		assertTrue(html.contains("#2 원본"));
		assertTrue(html.contains("rider2"));
	}
}
