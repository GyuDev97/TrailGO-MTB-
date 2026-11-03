package com.bike.webapp.weather;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses the 기상청 단기예보 조회서비스 2.0 <b>초단기실황</b> (getUltraSrtNcst) response and
 * classifies its result code. Only what the observation really contains is read:
 *
 * <ul>
 * <li>{@code T1H} 기온 (℃), {@code RN1} 1시간 강수량 (mm), {@code PTY} 강수형태</li>
 * <li>PTY: 0 없음, 1 비, 2 비/눈, 3 눈, 5 빗방울, 6 빗방울눈날림, 7 눈날림</li>
 * <li>-999 (and other 3-digit-negative sentinels) mean "missing"</li>
 * </ul>
 *
 * <p>The observation has no sky condition (맑음/흐림): none is derived here. Error responses may be
 * JSON ({@code response.header.resultCode}) or, for gateway-level errors such as a bad key, XML
 * ({@code returnReasonCode}); both are mapped to an {@link Outcome}.</p>
 *
 * <p>Raw bodies never leave this class: callers get normalised values and an outcome.</p>
 */
public final class KmaResponseParser {

	public enum Outcome {
		OK,
		/** 03 NODATA_ERROR: nothing published for that base time (yet / any more). */
		NO_DATA,
		/** 22 / 23: daily or per-second call limit exceeded. */
		LIMIT_EXCEEDED,
		/** 20, 21, 30, 31, 32, 33: key missing, disabled, not registered, expired, wrong IP... */
		KEY_REJECTED,
		/** 01, 02, 04, 05, 10, 11, 12, 99 and anything else the service reports as an error. */
		SERVICE_ERROR,
		/** Not a recognisable response at all (empty, HTML, truncated). */
		MALFORMED
	}

	/** Observation values at the base time. */
	public static final class Observation {
		public final LocalDateTime observedAt;
		public final double temperatureC;
		/** Normalised PTY: NONE, RAIN, RAIN_SNOW, SNOW, DRIZZLE, DRIZZLE_SNOW, SNOW_FLURRY (or SHOWER). */
		public final String precipitationCode;
		/** RN1 in mm, or null when missing/unreadable. */
		public final Double precipitationMm;

		Observation(LocalDateTime observedAt, double temperatureC, String precipitationCode, Double precipitationMm) {
			this.observedAt = observedAt;
			this.temperatureC = temperatureC;
			this.precipitationCode = precipitationCode;
			this.precipitationMm = precipitationMm;
		}
	}

	public static final class Parsed {
		public final Outcome outcome;
		/** The service's result code ("00", "22", ...) when one was found; never the key or the body. */
		public final String code;
		public final Observation observation;

		Parsed(Outcome outcome, String code, Observation observation) {
			this.outcome = outcome;
			this.code = code;
			this.observation = observation;
		}
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
	private static final Pattern XML_CODE = Pattern.compile("<(?:returnReasonCode|resultCode)>\\s*(\\d+)\\s*</");

	private KmaResponseParser() {
	}

	public static Parsed parseObservation(String body) {
		if (body == null || body.trim().isEmpty()) {
			return new Parsed(Outcome.MALFORMED, null, null);
		}
		String trimmed = body.trim();
		if (trimmed.startsWith("<")) {
			Matcher matcher = XML_CODE.matcher(trimmed);
			if (matcher.find()) {
				String code = matcher.group(1);
				// A successful XML body would be "00": only JSON is requested, so treat it as unusable.
				Outcome outcome = classify(code);
				return new Parsed(outcome == Outcome.OK ? Outcome.MALFORMED : outcome, code, null);
			}
			return new Parsed(Outcome.MALFORMED, null, null);
		}

		JsonNode response;
		try {
			response = MAPPER.readTree(trimmed).path("response");
		} catch (IOException | RuntimeException e) {
			return new Parsed(Outcome.MALFORMED, null, null);
		}
		String code = response.path("header").path("resultCode").asText("");
		if (code.isEmpty()) {
			return new Parsed(Outcome.MALFORMED, null, null);
		}
		Outcome outcome = classify(code);
		if (outcome != Outcome.OK) {
			return new Parsed(outcome, code, null);
		}

		JsonNode items = items(response);
		if (items == null) {
			return new Parsed(Outcome.NO_DATA, code, null);
		}
		Double temperature = null;
		String precipitation = null;
		Double rain = null;
		LocalDateTime observedAt = null;
		for (JsonNode item : items) {
			if (observedAt == null) {
				observedAt = stamp(item.path("baseDate").asText(""), item.path("baseTime").asText(""));
			}
			String category = item.path("category").asText("");
			String value = item.path("obsrValue").asText("");
			if ("T1H".equals(category)) {
				temperature = number(value);
			} else if ("PTY".equals(category)) {
				precipitation = precipitationFromCode(value);
			} else if ("RN1".equals(category)) {
				rain = rainMm(value);
			}
		}
		if (observedAt == null || temperature == null || precipitation == null) {
			return new Parsed(Outcome.MALFORMED, code, null);
		}
		return new Parsed(Outcome.OK, code, new Observation(observedAt, temperature, precipitation, rain));
	}

	/** Maps the service's result code to what the caller should do about it. */
	static Outcome classify(String code) {
		switch (code.trim()) {
		case "00":
			return Outcome.OK;
		case "03":
			return Outcome.NO_DATA;
		case "22":
		case "23":
			return Outcome.LIMIT_EXCEEDED;
		case "20":
		case "21":
		case "30":
		case "31":
		case "32":
		case "33":
			return Outcome.KEY_REJECTED;
		default:
			return Outcome.SERVICE_ERROR;
		}
	}

	/** PTY (초단기실황): 0 없음, 1 비, 2 비/눈, 3 눈, 5 빗방울, 6 빗방울눈날림, 7 눈날림. */
	static String precipitationFromCode(String code) {
		switch (code.trim()) {
		case "0":
			return WeatherSnapshot.PRECIP_NONE;
		case "1":
			return WeatherSnapshot.PRECIP_RAIN;
		case "2":
			return WeatherSnapshot.PRECIP_RAIN_SNOW;
		case "3":
			return WeatherSnapshot.PRECIP_SNOW;
		case "4":
			return WeatherSnapshot.PRECIP_SHOWER;
		case "5":
			return WeatherSnapshot.PRECIP_DRIZZLE;
		case "6":
			return WeatherSnapshot.PRECIP_DRIZZLE_SNOW;
		case "7":
			return WeatherSnapshot.PRECIP_SNOW_FLURRY;
		default:
			return null;
		}
	}

	/** Korean label for the precipitation type; "강수 없음" only when the observation says PTY 0. */
	public static String labelFor(String precipitationCode) {
		if (precipitationCode == null) {
			return null;
		}
		switch (precipitationCode) {
		case WeatherSnapshot.PRECIP_NONE:
			return "강수 없음";
		case WeatherSnapshot.PRECIP_RAIN:
			return "비";
		case WeatherSnapshot.PRECIP_RAIN_SNOW:
			return "비/눈";
		case WeatherSnapshot.PRECIP_SNOW:
			return "눈";
		case WeatherSnapshot.PRECIP_SHOWER:
			return "소나기";
		case WeatherSnapshot.PRECIP_DRIZZLE:
			return "빗방울";
		case WeatherSnapshot.PRECIP_DRIZZLE_SNOW:
			return "빗방울/눈날림";
		case WeatherSnapshot.PRECIP_SNOW_FLURRY:
			return "눈날림";
		default:
			return null;
		}
	}

	private static JsonNode items(JsonNode response) {
		JsonNode item = response.path("body").path("items").path("item");
		if (item.isArray() && item.size() > 0) {
			return item;
		}
		if (item.isObject()) {
			return MAPPER.createArrayNode().add(item);
		}
		return null;
	}

	/** A finite value; -999 style sentinels and anything non-numeric are "missing". */
	private static Double number(String text) {
		try {
			double value = Double.parseDouble(text.trim());
			if (Double.isNaN(value) || Double.isInfinite(value) || value <= -900.0 || value >= 900.0) {
				return null;
			}
			return value;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** RN1 is a number of mm; the service may also spell "no rain" as 강수없음. Ranges stay unknown. */
	private static Double rainMm(String text) {
		String value = text.trim();
		if ("강수없음".equals(value)) {
			return 0.0;
		}
		Double parsed = number(value);
		return parsed == null || parsed < 0 ? null : parsed;
	}

	private static LocalDateTime stamp(String date, String time) {
		if (date.length() != 8 || time.length() != 4) {
			return null;
		}
		try {
			return LocalDateTime.parse(date + time, STAMP);
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
