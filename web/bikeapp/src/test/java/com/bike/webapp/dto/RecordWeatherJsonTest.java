package com.bike.webapp.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class RecordWeatherJsonTest {

	private final ObjectMapper mapper = new ObjectMapper();

	private RidingRecord record() {
		RidingRecord record = new RidingRecord();
		record.setRr_num(123);
		record.setRr_name("주말 라이딩");
		record.setRr_distance(50);
		record.setRr_time(245939);
		record.setRr_high(36);
		record.setRr_gpx("gpx_x.gpx");
		record.setRr_date(Timestamp.valueOf("2026-10-10 19:49:00"));
		return record;
	}

	@Test
	void recordWithoutSnapshotAlwaysHasAnUnavailableWeatherObject() throws Exception {
		JsonNode json = mapper.readTree(mapper.writeValueAsString(record()));
		assertTrue(json.has("weather"));
		assertEquals("{\"available\":false}", json.get("weather").toString());
	}

	@Test
	void detailJsonMatchesTheDocumentedShape() throws Exception {
		RidingRecord record = record();
		record.setRr_weather_observed_at(Timestamp.valueOf(LocalDateTime.of(2026, 10, 10, 19, 49)));
		record.setRr_weather_temperature_c(21.0);
		record.setRr_weather_precipitation_mm(1.5);
		record.setRr_weather_precipitation_code("RAIN");
		record.setRr_weather_label("비");
		record.setRr_weather_nx(89);
		record.setRr_weather_ny(90);

		JsonNode json = mapper.readTree(mapper.writeValueAsString(record));
		JsonNode weather = json.get("weather");
		assertTrue(weather.get("available").asBoolean());
		assertEquals("2026-10-10T19:49:00+09:00", weather.get("observedAt").asText());
		assertEquals(21.0, weather.get("temperatureC").asDouble(), 0.0);
		assertFalse(weather.has("sky")); // an observation has no sky condition
		assertEquals(1.5, weather.get("precipitationMm").asDouble(), 0.0);
		assertFalse(weather.has("token"));
		assertEquals("RAIN", weather.get("precipitation").asText());
		assertEquals("비", weather.get("label").asText());
	}

	@Test
	void incompleteSnapshotIsReportedAsUnavailableNotHalfFilled() throws Exception {
		RidingRecord record = record();
		record.setRr_weather_temperature_c(21.0); // no observedAt / label
		assertEquals("{\"available\":false}", mapper.readTree(mapper.writeValueAsString(record)).get("weather").toString());
	}

	@Test
	void rawColumnsAndStartCoordinatesNeverLeakIntoJson() throws Exception {
		RidingRecord record = record();
		record.setRr_weather_observed_at(Timestamp.valueOf(LocalDateTime.of(2026, 10, 10, 19, 49)));
		record.setRr_weather_temperature_c(21.0);
		record.setRr_weather_label("맑음");
		record.setRr_start_lat(35.8329);
		record.setRr_start_lon(128.5650);

		JsonNode json = mapper.readTree(mapper.writeValueAsString(record));
		json.fieldNames().forEachRemaining(name -> {
			assertFalse(name.startsWith("rr_weather_"), name);
			assertFalse(name.startsWith("rr_start_l"), name);
		});
	}

	@Test
	void listDtoHasTheSameWeatherContract() throws Exception {
		App_RidingRecord record = new App_RidingRecord();
		record.setRr_num(5);
		JsonNode json = mapper.readTree(mapper.writeValueAsString(record));
		assertEquals("{\"available\":false}", json.get("weather").toString());
	}
}
