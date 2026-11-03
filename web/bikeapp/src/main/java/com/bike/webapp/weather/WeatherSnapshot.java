package com.bike.webapp.weather;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Observed weather near the start of a ride, already normalised for the apps: no KMA codes leak
 * out. {@code observedAt} is the Asia/Seoul base time of the 초단기실황 observation.
 *
 * <p>There is deliberately no sky condition: the observation does not contain one.</p>
 */
public final class WeatherSnapshot {

	public static final String PRECIP_NONE = "NONE";
	public static final String PRECIP_RAIN = "RAIN";
	public static final String PRECIP_RAIN_SNOW = "RAIN_SNOW";
	public static final String PRECIP_SNOW = "SNOW";
	public static final String PRECIP_SHOWER = "SHOWER";
	public static final String PRECIP_DRIZZLE = "DRIZZLE";
	public static final String PRECIP_DRIZZLE_SNOW = "DRIZZLE_SNOW";
	public static final String PRECIP_SNOW_FLURRY = "SNOW_FLURRY";

	private final LocalDateTime observedAt;
	private final int nx;
	private final int ny;
	private final double temperatureC;
	private final String precipitationCode;
	private final Double precipitationMm;
	private final String label;

	public WeatherSnapshot(LocalDateTime observedAt, int nx, int ny, double temperatureC,
			String precipitationCode, Double precipitationMm, String label) {
		this.observedAt = observedAt;
		this.nx = nx;
		this.ny = ny;
		this.temperatureC = temperatureC;
		this.precipitationCode = precipitationCode;
		this.precipitationMm = precipitationMm;
		this.label = label;
	}

	public LocalDateTime getObservedAt() {
		return observedAt;
	}

	/** Wall-clock value for the DATETIME column. */
	public Timestamp getObservedAtTimestamp() {
		return Timestamp.valueOf(observedAt);
	}

	public int getNx() {
		return nx;
	}

	public int getNy() {
		return ny;
	}

	public double getTemperatureC() {
		return temperatureC;
	}

	/** Always null: an observation has no sky condition. Kept for the (nullable) sky column. */
	public String getSkyCode() {
		return null;
	}

	public String getPrecipitationCode() {
		return precipitationCode;
	}

	/** 1-hour precipitation in mm, null when the observation did not carry a usable value. */
	public Double getPrecipitationMm() {
		return precipitationMm;
	}

	public String getLabel() {
		return label;
	}
}
