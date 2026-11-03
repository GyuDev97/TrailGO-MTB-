package com.bike.webapp.controller.restful;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.bike.webapp.dto.App_RidingRecord;
import com.bike.webapp.dto.RidingRecord;
import com.bike.webapp.service.AndroidService;
import com.bike.webapp.weather.WeatherCaptureService;

/** Behaviour of the ride-record endpoints the Android app uses, including the weather snapshot. */
class AndroidRecordApiTest {

	private AndroidService androidService;
	private WeatherCaptureService weatherCaptureService;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		androidService = mock(AndroidService.class);
		weatherCaptureService = mock(WeatherCaptureService.class);
		AndroidController controller = new AndroidController();
		ReflectionTestUtils.setField(controller, "androidService", androidService);
		ReflectionTestUtils.setField(controller, "weatherCaptureService", weatherCaptureService);
		ReflectionTestUtils.setField(controller, "recordPhotoService", mock(com.bike.webapp.service.RecordPhotoService.class));
		ReflectionTestUtils.setField(controller, "storageRoot", "build/test-storage");
		mvc = MockMvcBuilders.standaloneSetup(controller)
				.defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
						.requestAttr(com.bike.webapp.auth.AppIdentity.ATTRIBUTE, "rider1"))
				.build();
	}

	private static RidingRecord ownRecord(int num, String rider) {
		RidingRecord record = new RidingRecord();
		record.setRr_num(num);
		record.setRr_rider(rider);
		record.setRr_name("주말 라이딩");
		record.setRr_distance(50);
		record.setRr_time(245939);
		record.setRr_high(36);
		record.setRr_gpx("gpx_x.gpx");
		record.setRr_date(Timestamp.valueOf("2026-10-10 19:49:00"));
		return record;
	}

	private static App_RidingRecord listRecord(int num) {
		App_RidingRecord record = new App_RidingRecord();
		record.setRr_num(num);
		record.setRr_rider("rider1");
		record.setRr_distance(1200);
		return record;
	}

	// ---- saving -------------------------------------------------------------------------------

	@Test
	void savingARecordWithCoordinatesSchedulesAWeatherSnapshot() throws Exception {
		doThrowIdOnInsert(77);
		mvc.perform(post("/app/riding/upload")
				.param("rr_rider", "rider1").param("rr_distance", "5000")
				.param("rr_start_lat", "35.8329").param("rr_start_lon", "128.565"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saved").value("1"));

		ArgumentCaptor<Double> lat = ArgumentCaptor.forClass(Double.class);
		ArgumentCaptor<Double> lon = ArgumentCaptor.forClass(Double.class);
		verify(weatherCaptureService).captureAsync(eq(77), isNull(), lat.capture(), lon.capture(), any(LocalDateTime.class));
		assertEquals(35.8329, lat.getValue(), 0.0);
		assertEquals(128.565, lon.getValue(), 0.0);
	}

	@Test
	void savingWithoutCoordinatesStillSucceedsAndPassesNullCoordinates() throws Exception {
		doThrowIdOnInsert(78);
		mvc.perform(post("/app/riding/upload").param("rr_rider", "rider1").param("rr_distance", "5000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saved").value("1"));
		verify(weatherCaptureService).captureAsync(eq(78), isNull(), isNull(), isNull(), any(LocalDateTime.class));
	}

	@Test
	void weatherSchedulingFailureNeverFailsTheSave() throws Exception {
		doThrowIdOnInsert(79);
		doThrow(new IllegalStateException("boom")).when(weatherCaptureService)
				.captureAsync(anyInt(), any(), any(), any(), any());
		mvc.perform(post("/app/riding/upload").param("rr_rider", "rider1")
				.param("rr_start_lat", "35.8").param("rr_start_lon", "128.5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saved").value("1"));
	}

	@Test
	void aFailedSaveStaysAFailureAndNoWeatherIsCaptured() throws Exception {
		doThrow(new IllegalStateException("db")).when(androidService).insertRecord(any(RidingRecord.class));
		mvc.perform(post("/app/riding/upload").param("rr_rider", "rider1")
				.param("rr_start_lat", "35.8").param("rr_start_lon", "128.5"))
				.andExpect(status().isInternalServerError());
		verify(weatherCaptureService, never()).captureAsync(anyInt(), any(), any(), any(), any());
	}

	private void doThrowIdOnInsert(int id) throws Exception {
		org.mockito.Mockito.doAnswer(invocation -> {
			((RidingRecord) invocation.getArgument(0)).setRr_num(id);
			return null;
		}).when(androidService).insertRecord(any(RidingRecord.class));
	}

	// ---- reading ------------------------------------------------------------------------------

	@Test
	void legacyListPathUsesTheRealLoginIdInsteadOfAlwaysAdmin() throws Exception {
		when(androidService.readRecord("rider1")).thenReturn(Collections.singletonList(listRecord(1)));
		mvc.perform(get("/app/get/rider1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].rr_num").value(1))
				.andExpect(jsonPath("$[0].weather.available").value(false));
		verify(androidService).readRecord("rider1");
	}

	@Test
	void explicitListPathReturnsTheSameRecords() throws Exception {
		when(androidService.readRecord("rider1")).thenReturn(Collections.singletonList(listRecord(1)));
		mvc.perform(get("/app/riding/records/rider1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].rr_distance").value(1200))
				.andExpect(jsonPath("$[0].weather.available").value(false));
	}

	@Test
	void ownDetailIncludesTheStoredWeatherSnapshot() throws Exception {
		RidingRecord record = ownRecord(123, "rider1");
		record.setRr_weather_observed_at(Timestamp.valueOf(LocalDateTime.of(2026, 10, 10, 19, 49)));
		record.setRr_weather_temperature_c(21.0);
		record.setRr_weather_sky_code("CLEAR");
		record.setRr_weather_precipitation_code("NONE");
		record.setRr_weather_label("맑음");
		when(androidService.getOwnRecord("rider1", 123)).thenReturn(record);

		mvc.perform(get("/app/riding/records/rider1/123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rr_num").value(123))
				.andExpect(jsonPath("$.rr_high").value(36))
				.andExpect(jsonPath("$.weather.available").value(true))
				.andExpect(jsonPath("$.weather.observedAt").value("2026-10-10T19:49:00+09:00"))
				.andExpect(jsonPath("$.weather.temperatureC").value(21.0))
				.andExpect(jsonPath("$.weather.sky").value("CLEAR"))
				.andExpect(jsonPath("$.weather.precipitation").value("NONE"))
				.andExpect(jsonPath("$.weather.label").value("맑음"));
	}

	@Test
	void detailIsTheSameOnEveryReadBecauseItIsAStoredSnapshot() throws Exception {
		RidingRecord record = ownRecord(123, "rider1");
		record.setRr_weather_observed_at(Timestamp.valueOf(LocalDateTime.of(2026, 10, 10, 19, 49)));
		record.setRr_weather_temperature_c(21.0);
		record.setRr_weather_label("맑음");
		when(androidService.getOwnRecord("rider1", 123)).thenReturn(record);

		String first = mvc.perform(get("/app/riding/records/rider1/123")).andReturn().getResponse().getContentAsString();
		String second = mvc.perform(get("/app/riding/records/rider1/123")).andReturn().getResponse().getContentAsString();
		assertEquals(first, second);
		// reading never triggers a weather lookup
		verify(weatherCaptureService, never()).captureAsync(anyInt(), any(), any(), any(), any());
	}

	@Test
	void oldRecordWithoutSnapshotReturnsAnUnavailableWeatherObject() throws Exception {
		when(androidService.getOwnRecord("rider1", 5)).thenReturn(ownRecord(5, "rider1"));
		mvc.perform(get("/app/riding/records/rider1/5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.weather.available").value(false))
				.andExpect(jsonPath("$.weather.label").doesNotExist());
	}

	@Test
	void anotherRidersRecordIsNotFound() throws Exception {
		// the service only returns a row when rider AND number match
		when(androidService.getOwnRecord("rider1", 999)).thenReturn(null);
		mvc.perform(get("/app/riding/records/rider1/999")).andExpect(status().isNotFound());
		verify(androidService).getOwnRecord("rider1", 999);
	}

	@Test
	void legacyDetailPathIsPublicRecordsOnly() throws Exception {
		RidingRecord open = ownRecord(7, "rider2");
		open.setRr_open(1);
		when(androidService.getPublicRecord(7)).thenReturn(open);
		when(androidService.getPublicRecord(8)).thenReturn(null);

		mvc.perform(get("/app/get/detail/7")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].rr_num").value(7))
				.andExpect(jsonPath("$[0].weather.available").value(false));
		mvc.perform(get("/app/get/detail/8")).andExpect(status().isNotFound());
		verify(androidService, never()).getRidingRecordDetail(any(), any());
	}

	@Test
	void visibilityEndpointIsUnchanged() throws Exception {
		when(androidService.RidingOpenSet(any(RidingRecord.class))).thenReturn(1);
		mvc.perform(post("/app/recordset/open").param("rr_num", "123").param("rr_open", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.updated").value("1"));
		ArgumentCaptor<RidingRecord> captured = ArgumentCaptor.forClass(RidingRecord.class);
		verify(androidService).RidingOpenSet(captured.capture());
		assertEquals(123, captured.getValue().getRr_num());
		assertEquals(1, captured.getValue().getRr_open());
	}
}
