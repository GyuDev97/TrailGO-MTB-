package com.bike.webapp.controller.restful;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.bike.webapp.dto.RecordPhoto;
import com.bike.webapp.dto.RidingRecord;
import com.bike.webapp.service.AndroidService;
import com.bike.webapp.service.RecordPhotoService;
import com.bike.webapp.weather.WeatherCaptureService;
import com.bike.webapp.weather.WeatherService;
import com.bike.webapp.weather.WeatherSnapshotTokens;
import com.bike.webapp.weather.WeatherSnapshot;

/** Memo, photo and weather-preview endpoints used by the save and detail screens. */
class AndroidMemoPhotoWeatherApiTest {

	private AndroidService androidService;
	private RecordPhotoService photoService;
	private WeatherService weatherService;
	private WeatherCaptureService captureService;
	private MockMvc mvc;
	private AndroidController controller;
	private WeatherSnapshotTokens tokens;

	@BeforeEach
	void setUp() {
		androidService = mock(AndroidService.class);
		photoService = mock(RecordPhotoService.class);
		weatherService = mock(WeatherService.class);
		captureService = mock(WeatherCaptureService.class);
		controller = new AndroidController();
		tokens = new WeatherSnapshotTokens();
		ReflectionTestUtils.setField(controller, "weatherSnapshotTokens", tokens);
		ReflectionTestUtils.setField(controller, "androidService", androidService);
		ReflectionTestUtils.setField(controller, "recordPhotoService", photoService);
		ReflectionTestUtils.setField(controller, "weatherService", weatherService);
		ReflectionTestUtils.setField(controller, "weatherCaptureService", captureService);
		ReflectionTestUtils.setField(controller, "storageRoot", "build/test-storage");
		mvc = MockMvcBuilders.standaloneSetup(controller)
				.defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
						.requestAttr(com.bike.webapp.auth.AppIdentity.ATTRIBUTE, "rider1"))
				.build();
	}

	/** The signed-in rider of the call (null: a guest without a session). */
	private static org.springframework.test.web.servlet.request.RequestPostProcessor asRider(String rider) {
		return request -> {
			request.setAttribute(com.bike.webapp.auth.AppIdentity.ATTRIBUTE, rider);
			return request;
		};
	}

	private static String hex(char c) {
		char[] chars = new char[32];
		java.util.Arrays.fill(chars, c);
		return new String(chars);
	}

	private static String repeat(int n) {
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < n; i++) {
			text.append('가');
		}
		return text.toString();
	}

	// ---- saving with memo / start time -------------------------------------------------------

	@Test
	void savingReturnsTheNewRecordNumberAndStoresTheMemo() throws Exception {
		doAnswer(invocation -> {
			((RidingRecord) invocation.getArgument(0)).setRr_num(321);
			return null;
		}).when(androidService).insertRecord(any(RidingRecord.class));

		mvc.perform(post("/app/riding/upload").param("rr_rider", "rider1")
				.param("rr_memo", "하산 구간 조심").param("rr_name", "관악산"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saved").value("1"))
				.andExpect(jsonPath("$.rr_num").value("321"));

		ArgumentCaptor<RidingRecord> saved = ArgumentCaptor.forClass(RidingRecord.class);
		verify(androidService).insertRecord(saved.capture());
		assertEquals("하산 구간 조심", saved.getValue().getRr_memo());
	}

	@Test
	void blankMemoIsStoredAsNullAndLongMemoIsCutToFiveHundred() {
		assertNull(AndroidController.normalizeMemo(null));
		assertNull(AndroidController.normalizeMemo("   \n "));
		assertEquals("a", AndroidController.normalizeMemo("a"));
		assertEquals(500, AndroidController.normalizeMemo(repeat(500)).length());
		assertEquals(500, AndroidController.normalizeMemo(repeat(900)).length());
	}

	@Test
	void weatherTimeUsesTheRideStartWhenItIsSaneAndNowOtherwise() {
		LocalDateTime now = LocalDateTime.now(WeatherService.KST);
		assertEquals(LocalDateTime.of(2026, 10, 10, 18, 5, 30), AndroidController.weatherTime("2026-10-10 18:05:30"));
		assertTrue(java.time.Duration.between(AndroidController.weatherTime(null), now).abs().getSeconds() < 5);
		assertTrue(java.time.Duration.between(AndroidController.weatherTime("garbage"), now).abs().getSeconds() < 5);
		assertTrue(java.time.Duration.between(AndroidController.weatherTime("2999-01-01 00:00:00"), now).abs().getSeconds() < 5);
	}

	@Test
	void theCapturedWeatherUsesTheRideStartTime() throws Exception {
		doAnswer(invocation -> {
			((RidingRecord) invocation.getArgument(0)).setRr_num(9);
			return null;
		}).when(androidService).insertRecord(any(RidingRecord.class));
		mvc.perform(post("/app/riding/upload").param("rr_rider", "rider1")
				.param("rr_start_lat", "35.8").param("rr_start_lon", "128.5")
				.param("rr_started_at", "2026-10-10 18:05:30")).andExpect(status().isOk());
		verify(captureService).captureAsync(eq(9), isNull(), eq(35.8), eq(128.5), eq(LocalDateTime.of(2026, 10, 10, 18, 5, 30)));
	}

	// ---- memo ---------------------------------------------------------------------------------

	@Test
	void ownerCanEditTheMemoAndEmptyClearsIt() throws Exception {
		when(androidService.updateMemo("rider1", 7, "새 메모")).thenReturn(1);
		when(androidService.updateMemo("rider1", 7, null)).thenReturn(1);

		mvc.perform(post("/app/riding/records/rider1/7/memo").param("memo", "새 메모"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.updated").value("1"));
		mvc.perform(post("/app/riding/records/rider1/7/memo").param("memo", ""))
				.andExpect(status().isOk());
		verify(androidService).updateMemo("rider1", 7, null);
	}

	@Test
	void memoOfSomeoneElsesRecordIsNotFoundAndOverlongMemoIsRejected() throws Exception {
		when(androidService.updateMemo("intruder", 7, "x")).thenReturn(0);
		mvc.perform(post("/app/riding/records/intruder/7/memo").param("memo", "x")).andExpect(status().isForbidden());
		mvc.perform(post("/app/riding/records/rider1/7/memo").param("memo", repeat(501)))
				.andExpect(status().isBadRequest());
		verify(androidService, never()).updateMemo(eq("rider1"), anyInt(), anyString());
	}

	// ---- photos -------------------------------------------------------------------------------

	private static RecordPhoto photo(String id, int order) {
		RecordPhoto photo = new RecordPhoto();
		photo.setId(id);
		photo.setSortOrder(order);
		return photo;
	}

	@Test
	void detailListsThePhotosOfTheRecord() throws Exception {
		RidingRecord record = new RidingRecord();
		record.setRr_num(7);
		record.setRr_date(Timestamp.valueOf("2026-10-10 19:49:00"));
		record.setRr_memo("메모");
		when(androidService.getOwnRecord("rider1", 7)).thenReturn(record);
		when(photoService.list(7)).thenReturn(Arrays.asList(photo(hex('a'), 0), photo(hex('b'), 1)));

		mvc.perform(get("/app/riding/records/rider1/7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rr_memo").value("메모"))
				.andExpect(jsonPath("$.photos.length()").value(2))
				.andExpect(jsonPath("$.photos[0].id").value(hex('a')))
				.andExpect(jsonPath("$.photos[1].sortOrder").value(1));
	}

	@Test
	void uploadMapsServiceResultsToHttpStatuses() throws Exception {
		MockMultipartFile file = new MockMultipartFile("photo", "p.jpg", "image/jpeg", new byte[] {1});
		when(photoService.add(eq("rider1"), eq(7), any())).thenReturn(
				new RecordPhotoService.AddResult(RecordPhotoService.Status.OK, photo(hex('c'), 2)));
		mvc.perform(multipart("/app/riding/records/rider1/7/photos").file(file))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(hex('c')));

		RecordPhotoService.Status[] statuses = {RecordPhotoService.Status.NOT_FOUND, RecordPhotoService.Status.TOO_MANY,
				RecordPhotoService.Status.TOO_LARGE, RecordPhotoService.Status.INVALID, RecordPhotoService.Status.FAILED};
		int[] codes = {404, 409, 413, 400, 500};
		for (int i = 0; i < statuses.length; i++) {
			when(photoService.add(eq("rider1"), eq(7), any()))
					.thenReturn(new RecordPhotoService.AddResult(statuses[i], null));
			mvc.perform(multipart("/app/riding/records/rider1/7/photos").file(file))
					.andExpect(status().is(codes[i]));
		}
	}

	@Test
	void deleteIsOwnerOnly() throws Exception {
		String id = hex('d');
		when(photoService.delete("rider1", 7, id)).thenReturn(true);
		mvc.perform(delete("/app/riding/records/rider1/7/photos/" + id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.deleted").value("1"));
		mvc.perform(delete("/app/riding/records/intruder/7/photos/" + id)).andExpect(status().isForbidden());
	}

	@Test
	void photoBytesAreServedOnlyWhenTheServiceAllowsIt(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("x.jpg");
		Files.write(file, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2});
		String id = hex('e');
		when(photoService.open(id, "rider1")).thenReturn(new RecordPhotoService.PhotoFile(file, "image/jpeg", false));

		mvc.perform(get("/app/riding/photos/" + id))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "image/jpeg"))
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")))
				.andExpect(content().bytes(Files.readAllBytes(file)));
		mvc.perform(get("/app/riding/photos/" + id).with(asRider(null))).andExpect(status().isNotFound());
		mvc.perform(get("/app/riding/photos/" + id).with(asRider("intruder"))).andExpect(status().isNotFound());
	}

	// ---- weather lookup -----------------------------------------------------------------------

	private WeatherSnapshot snapshot() {
		return new WeatherSnapshot(LocalDateTime.of(2026, 10, 10, 19, 0), 88, 90, 21.0, "RAIN", 1.5, "비");
	}

	@Test
	void weatherLookupReturnsTheObservationAndATokenForTheSave() throws Exception {
		when(weatherService.lookup(eq(35.8329), eq(128.565), any(LocalDateTime.class))).thenReturn(Optional.of(snapshot()));

		mvc.perform(get("/app/weather").param("lat", "35.8329").param("lon", "128.565")
				.param("at", "2026-10-10 19:20:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.available").value(true))
				.andExpect(jsonPath("$.observedAt").value("2026-10-10T19:00:00+09:00"))
				.andExpect(jsonPath("$.temperatureC").value(21.0))
				.andExpect(jsonPath("$.precipitation").value("RAIN"))
				.andExpect(jsonPath("$.precipitationMm").value(1.5))
				.andExpect(jsonPath("$.label").value("비"))
				.andExpect(jsonPath("$.sky").doesNotExist())
				.andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f]{32}")));
	}

	@Test
	void theTokenHandsTheSameSnapshotBackAtSaveTime() throws Exception {
		when(weatherService.lookup(any(), any(), any(LocalDateTime.class))).thenReturn(Optional.of(snapshot()));
		String body = mvc.perform(get("/app/weather").param("lat", "35.8329").param("lon", "128.565"))
				.andReturn().getResponse().getContentAsString();
		String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("token").asText();
		assertEquals(21.0, tokens.get(token).getTemperatureC(), 0.0);
	}

	@Test
	void weatherLookupWithoutDataOrOnFailureIsExactlyUnavailable() throws Exception {
		when(weatherService.lookup(any(), any(), any())).thenReturn(Optional.empty());
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5"))
				.andExpect(status().isOk()).andExpect(content().json("{\"available\":false}", true));

		when(weatherService.lookup(any(), any(), any())).thenThrow(new IllegalStateException("boom"));
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5"))
				.andExpect(status().isOk()).andExpect(content().json("{\"available\":false}", true));
	}

	@Test
	void weatherLookupNeedsCoordinates() throws Exception {
		mvc.perform(get("/app/weather")).andExpect(status().isBadRequest());
	}

	@Test
	void aMemberIdThatDoesNotExistIsRefused() throws Exception {
		when(androidService.sameCheck("ghost")).thenReturn(0);
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5").param("riderId", "ghost"))
				.andExpect(status().isForbidden());
		verify(weatherService, never()).lookup(any(), any(), any());
	}

	@Test
	void aKnownMemberIsServed() throws Exception {
		when(androidService.sameCheck("rider1")).thenReturn(1);
		when(weatherService.lookup(any(), any(), any())).thenReturn(Optional.of(snapshot()));
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5").param("riderId", "rider1"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true));
	}

	@Test
	void anonymousGuestsAreLimitedMoreStrictlyThanMembers() throws Exception {
		when(weatherService.lookup(any(), any(), any())).thenReturn(Optional.empty());
		ReflectionTestUtils.setField(controller, "weatherAnonymousLimiter",
				new com.bike.webapp.weather.RequestRateLimiter(2, 600_000L));
		for (int i = 0; i < 2; i++) {
			mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5")).andExpect(status().isOk());
		}
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "600"));
	}

	@Test
	void aMemberHasHisOwnLimitAndTheDeviceHasAnother() throws Exception {
		when(androidService.sameCheck(anyString())).thenReturn(1);
		when(weatherService.lookup(any(), any(), any())).thenReturn(Optional.empty());
		ReflectionTestUtils.setField(controller, "weatherRiderLimiter",
				new com.bike.webapp.weather.RequestRateLimiter(1, 600_000L));
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5").param("riderId", "a").with(asRider("a"))).andExpect(status().isOk());
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5").param("riderId", "a").with(asRider("a"))).andExpect(status().isTooManyRequests());
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5").param("riderId", "b").with(asRider("b"))).andExpect(status().isOk());

		ReflectionTestUtils.setField(controller, "weatherIpLimiter",
				new com.bike.webapp.weather.RequestRateLimiter(1, 600_000L));
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5").param("riderId", "c").with(asRider("c"))).andExpect(status().isOk());
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5").param("riderId", "d").with(asRider("d"))).andExpect(status().isTooManyRequests());
	}

	@Test
	void theWeatherTokenSentWithTheRideIsPassedToTheCapture() throws Exception {
		doAnswer(invocation -> {
			((RidingRecord) invocation.getArgument(0)).setRr_num(11);
			return null;
		}).when(androidService).insertRecord(any(RidingRecord.class));
		String token = "0123456789abcdef0123456789abcdef";
		mvc.perform(post("/app/riding/upload").param("rr_rider", "rider1").param("rr_weather_token", token)
				.param("rr_start_lat", "35.8").param("rr_start_lon", "128.5")
				.param("rr_started_at", "2026-10-10 19:20:00")).andExpect(status().isOk());
		verify(captureService).captureAsync(eq(11), eq(token), eq(35.8), eq(128.5), eq(LocalDateTime.of(2026, 10, 10, 19, 20)));
	}
}