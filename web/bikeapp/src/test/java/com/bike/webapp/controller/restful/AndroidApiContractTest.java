package com.bike.webapp.controller.restful;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.bike.webapp.dto.App_RidingRecord;
import com.bike.webapp.dto.RidingRecord;
import com.bike.webapp.service.AndroidService;
import com.bike.webapp.weather.WeatherCaptureService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Pins the contract between the Android app (Retrofit {@code ServerApi}, {@code EndActivity})
 * and this controller: URL, HTTP method and field names. When the Android sources are checked out
 * next to this project they are read directly, so a change on either side fails here.
 */
class AndroidApiContractTest {

	/** HTTP method + path (relative to the server base URL) the Android app calls for ride records. */
	private static final String[][] ANDROID_RECORD_CALLS = {
			{"GET", "app/get/{loginId}"},
			{"POST", "app/riding/upload"},
			{"POST", "app/recordset/open"},
			{"GET", "app/riding/records/{riderId}/{rrNum}"},
	};

	/** Fields the app reads from the record list / detail JSON ({@code RidingRecord} in Android). */
	private static final List<String> RECORD_FIELDS = Arrays.asList(
			"rr_num", "rr_rider", "rr_date", "rr_distance", "rr_topspeed", "rr_avgspeed", "rr_high",
			"rr_gpx", "rr_open", "rr_breaktime", "rr_time", "rr_area", "rr_name", "rr_memo", "weather");

	private static final Path ANDROID_ROOT = Paths.get("..", "..", "app2", "app", "src", "main", "java",
			"com", "bike", "trailgo");

	private AndroidService androidService;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		androidService = mock(AndroidService.class);
		AndroidController controller = new AndroidController();
		ReflectionTestUtils.setField(controller, "androidService", androidService);
		ReflectionTestUtils.setField(controller, "weatherCaptureService", mock(WeatherCaptureService.class));
		ReflectionTestUtils.setField(controller, "recordPhotoService", mock(com.bike.webapp.service.RecordPhotoService.class));
		ReflectionTestUtils.setField(controller, "storageRoot", "build/test-storage");
		mvc = MockMvcBuilders.standaloneSetup(controller)
				.defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
						.requestAttr(com.bike.webapp.auth.AppIdentity.ATTRIBUTE, "rider1"))
				.build();
	}

	private static String read(String relative) throws IOException {
		Path file = ANDROID_ROOT.resolve(relative);
		assumeTrue(Files.isRegularFile(file), "Android sources not checked out next to the web project");
		return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
	}

	@Test
	void serverRoutesExistForEveryRecordCallTheAppMakes() throws Exception {
		when(androidService.readRecord(any())).thenReturn(Collections.<App_RidingRecord>emptyList());
		RidingRecord record = new RidingRecord();
		record.setRr_date(Timestamp.valueOf("2026-10-10 19:49:00"));
		when(androidService.getOwnRecord(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(record);

		for (String[] call : ANDROID_RECORD_CALLS) {
			String path = "/" + call[1]
					.replace("{loginId}", "rider1").replace("{riderId}", "rider1").replace("{rrNum}", "1");
			int status = mvc.perform(call[0].equals("GET")
					? org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
					: org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
							.param("rr_num", "1").param("rr_open", "1").param("rr_rider", "rider1"))
					.andReturn().getResponse().getStatus();
			assertTrue(status != 404 && status != 405, call[0] + " " + path + " -> HTTP " + status);
		}
	}

	@Test
	void androidServerApiDeclaresExactlyTheseRecordCalls() throws Exception {
		String source = read("net/ServerApi.java");
		for (String[] call : ANDROID_RECORD_CALLS) {
			Pattern declared = Pattern.compile("@" + call[0] + "\\(\"" + Pattern.quote(androidPath(call[1])) + "\"\\)");
			assertTrue(declared.matcher(source).find(),
					"ServerApi.java must declare @" + call[0] + "(\"" + androidPath(call[1]) + "\")");
		}
	}

	/** The placeholder names are part of the contract: ServerApi uses the same ones. */
	private static String androidPath(String path) {
		return path;
	}

	@Test
	void everyFormFieldTheAppSendsWhenSavingIsBoundByTheServer() throws Exception {
		String source = read("ui/riding/EndActivity.java");
		Matcher matcher = Pattern.compile("insert\\.put\\(\"(\\w+)\"").matcher(source);
		List<String> sent = new ArrayList<>();
		while (matcher.find()) {
			sent.add(matcher.group(1));
		}
		assertTrue(sent.containsAll(Arrays.asList("rr_rider", "rr_distance", "rr_topspeed", "rr_avgspeed",
				"rr_high", "rr_gpx", "rr_open", "rr_breaktime", "rr_time", "rr_area", "rr_name")), sent.toString());

		BeanWrapperImpl server = new BeanWrapperImpl(new RidingRecord());
		for (String field : sent) {
			assertTrue(server.isWritableProperty(field), "server RidingRecord cannot bind form field " + field);
		}
	}

	@Test
	void recordJsonContainsEveryFieldTheAppReads() throws Exception {
		RidingRecord detail = new RidingRecord();
		detail.setRr_date(Timestamp.valueOf("2026-10-10 19:49:00"));
		when(androidService.getOwnRecord("rider1", 1)).thenReturn(detail);
		App_RidingRecord listItem = new App_RidingRecord();
		when(androidService.readRecord("rider1")).thenReturn(Collections.singletonList(listItem));

		MvcResult detailResult = mvc.perform(
				org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/app/riding/records/rider1/1")).andReturn();
		MvcResult listResult = mvc.perform(
				org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/app/get/rider1")).andReturn();

		ObjectMapper mapper = new ObjectMapper();
		JsonNode detailJson = mapper.readTree(detailResult.getResponse().getContentAsString());
		JsonNode listJson = mapper.readTree(listResult.getResponse().getContentAsString()).get(0);
		for (String field : RECORD_FIELDS) {
			assertTrue(detailJson.has(field), "detail JSON lacks " + field);
			assertTrue(listJson.has(field), "list JSON lacks " + field);
		}
		assertEquals(false, detailJson.get("weather").get("available").asBoolean());
	}

	@Test
	void androidRecordModelDeclaresTheFieldsTheServerSends() throws Exception {
		String source = read("dto/RidingRecord.java");
		for (String field : RECORD_FIELDS) {
			assertTrue(Pattern.compile("private\\s+[\\w<>]+\\s+" + field + "\\s*;").matcher(source).find(),
					"Android RidingRecord must declare field " + field);
		}
	}
}
