package com.bike.webapp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;

class WeatherMigrationTest {

	private static final String[] COLUMNS = {
			"rr_weather_observed_at DATETIME NULL",
			"rr_weather_nx INT NULL",
			"rr_weather_ny INT NULL",
			"rr_weather_temperature_c DECIMAL(4,1) NULL",
			"rr_weather_sky_code VARCHAR(16) NULL",
			"rr_weather_precipitation_code VARCHAR(16) NULL",
			"rr_weather_label VARCHAR(40) NULL",
	};

	private static String resource(String path) throws IOException {
		try (InputStream in = WeatherMigrationTest.class.getResourceAsStream(path)) {
			assertTrue(in != null, path + " must exist");
			return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
		}
	}

	@Test
	void migrationAddsOnlyNullableWeatherColumnsToRidingRecord() throws IOException {
		String sql = resource("/db/migration/V20261013_1__riding_record_weather_snapshot.sql");
		assertTrue(sql.contains("ALTER TABLE riding_record"));
		for (String column : COLUMNS) {
			assertTrue(sql.contains(column), "missing column definition: " + column);
		}
		StringBuilder statements = new StringBuilder();
		for (String line : sql.split("\\r?\\n")) {
			if (!line.trim().startsWith("--")) {
				statements.append(line).append('\n');
			}
		}
		String code = statements.toString();
		assertFalse(code.toUpperCase().contains("NOT NULL"), "existing rides must stay valid: columns must be nullable");
		assertFalse(code.toUpperCase().contains("DROP "), "migration must be purely additive");
		assertFalse(code.toLowerCase().contains("rr_high"), "weather must not touch rr_high");
		assertFalse(code.toLowerCase().contains("rr_area"), "weather must not touch rr_area");
	}

	@Test
	void schemaSqlAppliedAtStartupCarriesTheSameColumns() throws IOException {
		String schema = resource("/schema.sql");
		for (String column : COLUMNS) {
			assertTrue(schema.contains(column), "schema.sql lacks: " + column);
		}
	}

	@Test
	void serviceKeyIsOnlyReadFromTheEnvironmentAndNeverCommitted() throws IOException {
		String properties = resource("/application.properties");
		assertTrue(properties.contains("trailgo.weather.kma-service-key=${KMA_SERVICE_KEY:}"));
	}

	@Test
	void precipitationAmountMigrationIsAdditiveAndAlsoRunsAtStartup() throws IOException {
		String sql = resource("/db/migration/V20261015_1__riding_record_weather_precipitation_mm.sql");
		assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS rr_weather_precipitation_mm DECIMAL(5,1) NULL"));
		assertFalse(sql.toUpperCase().contains("NOT NULL"));
		assertFalse(sql.toUpperCase().contains("DROP "));
		assertTrue(resource("/schema.sql").contains("rr_weather_precipitation_mm DECIMAL(5,1) NULL"));
	}
}
