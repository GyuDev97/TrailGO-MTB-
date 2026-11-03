package com.bike.webapp.dto;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The nullable {@code riding_record.rr_weather_*} snapshot columns, shared by the record DTOs.
 *
 * <p>The raw columns are MyBatis-only ({@link JsonIgnore}); the apps only see {@link #getWeather()}.
 * The sky/precipitation columns hold the normalised values (CLEAR, RAIN, ...), never KMA codes.</p>
 */
public abstract class WeatherColumns {

	private Timestamp rr_weather_observed_at;
	private Integer rr_weather_nx;
	private Integer rr_weather_ny;
	private Double rr_weather_temperature_c;
	private String rr_weather_sky_code;
	private String rr_weather_precipitation_code;
	private String rr_weather_label;
	private Double rr_weather_precipitation_mm;

	/** Weather at ride time; {@code {"available": false}} for old rides or when none was captured. */
	public RecordWeather getWeather() {
		return RecordWeather.from(rr_weather_observed_at, rr_weather_temperature_c,
				rr_weather_sky_code, rr_weather_precipitation_code, rr_weather_precipitation_mm, rr_weather_label);
	}

	@JsonIgnore
	public Timestamp getRr_weather_observed_at() {
		return rr_weather_observed_at;
	}

	public void setRr_weather_observed_at(Timestamp rr_weather_observed_at) {
		this.rr_weather_observed_at = rr_weather_observed_at;
	}

	@JsonIgnore
	public Integer getRr_weather_nx() {
		return rr_weather_nx;
	}

	public void setRr_weather_nx(Integer rr_weather_nx) {
		this.rr_weather_nx = rr_weather_nx;
	}

	@JsonIgnore
	public Integer getRr_weather_ny() {
		return rr_weather_ny;
	}

	public void setRr_weather_ny(Integer rr_weather_ny) {
		this.rr_weather_ny = rr_weather_ny;
	}

	@JsonIgnore
	public Double getRr_weather_temperature_c() {
		return rr_weather_temperature_c;
	}

	public void setRr_weather_temperature_c(Double rr_weather_temperature_c) {
		this.rr_weather_temperature_c = rr_weather_temperature_c;
	}

	@JsonIgnore
	public String getRr_weather_sky_code() {
		return rr_weather_sky_code;
	}

	public void setRr_weather_sky_code(String rr_weather_sky_code) {
		this.rr_weather_sky_code = rr_weather_sky_code;
	}

	@JsonIgnore
	public String getRr_weather_precipitation_code() {
		return rr_weather_precipitation_code;
	}

	public void setRr_weather_precipitation_code(String rr_weather_precipitation_code) {
		this.rr_weather_precipitation_code = rr_weather_precipitation_code;
	}

	@JsonIgnore
	public String getRr_weather_label() {
		return rr_weather_label;
	}

	public void setRr_weather_label(String rr_weather_label) {
		this.rr_weather_label = rr_weather_label;
	}

	@JsonIgnore
	public Double getRr_weather_precipitation_mm() {
		return rr_weather_precipitation_mm;
	}

	public void setRr_weather_precipitation_mm(Double rr_weather_precipitation_mm) {
		this.rr_weather_precipitation_mm = rr_weather_precipitation_mm;
	}
}
