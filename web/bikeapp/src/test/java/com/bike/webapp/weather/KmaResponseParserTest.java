package com.bike.webapp.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.bike.webapp.weather.KmaResponseParser.Outcome;
import com.bike.webapp.weather.KmaResponseParser.Parsed;

class KmaResponseParserTest {

	/** A getUltraSrtNcst answer as the service returns it (categories in the documented set). */
	static String ncst(String t1h, String pty, String rn1) {
		return "{\"response\":{\"header\":{\"resultCode\":\"00\",\"resultMsg\":\"NORMAL_SERVICE\"},"
				+ "\"body\":{\"dataType\":\"JSON\",\"items\":{\"item\":["
				+ item("PTY", pty) + "," + item("REH", "40") + "," + item("RN1", rn1) + ","
				+ item("T1H", t1h) + "," + item("UUU", "1.2") + "," + item("VEC", "270") + ","
				+ item("VVV", "-0.3") + "," + item("WSD", "1.5")
				+ "]},\"pageNo\":1,\"numOfRows\":100,\"totalCount\":8}}}";
	}

	private static String item(String category, String value) {
		return "{\"baseDate\":\"20261010\",\"baseTime\":\"1900\",\"category\":\"" + category
				+ "\",\"nx\":88,\"ny\":90,\"obsrValue\":\"" + value + "\"}";
	}

	static String error(String code, String message) {
		return "{\"response\":{\"header\":{\"resultCode\":\"" + code + "\",\"resultMsg\":\"" + message + "\"}}}";
	}

	@Test
	void parsesTemperatureRainTypeRainAmountAndObservationTime() {
		Parsed parsed = KmaResponseParser.parseObservation(ncst("21.0", "0", "0"));
		assertEquals(Outcome.OK, parsed.outcome);
		assertEquals("00", parsed.code);
		KmaResponseParser.Observation o = parsed.observation;
		assertEquals(21.0, o.temperatureC, 0.0);
		assertEquals("NONE", o.precipitationCode);
		assertEquals(0.0, o.precipitationMm, 0.0);
		assertEquals(LocalDateTime.of(2026, 10, 10, 19, 0), o.observedAt);
	}

	@Test
	void mapsEveryDocumentedPrecipitationTypeToItsOwnValueAndLabel() {
		String[][] expected = {
				{"0", "NONE", "강수 없음"}, {"1", "RAIN", "비"}, {"2", "RAIN_SNOW", "비/눈"}, {"3", "SNOW", "눈"},
				{"5", "DRIZZLE", "빗방울"}, {"6", "DRIZZLE_SNOW", "빗방울/눈날림"}, {"7", "SNOW_FLURRY", "눈날림"}};
		for (String[] row : expected) {
			KmaResponseParser.Observation o = KmaResponseParser.parseObservation(ncst("10", row[0], "0.5")).observation;
			assertEquals(row[1], o.precipitationCode, "PTY " + row[0]);
			assertEquals(row[2], KmaResponseParser.labelFor(o.precipitationCode));
		}
	}

	@Test
	void unknownPrecipitationCodeMakesTheObservationUnusable() {
		assertEquals(Outcome.MALFORMED, KmaResponseParser.parseObservation(ncst("10", "9", "0")).outcome);
	}

	@Test
	void thereIsNoSkyConditionInAnObservation() {
		// no SKY category is read, and no label ever talks about clouds
		for (String code : new String[] {"NONE", "RAIN", "SNOW", "DRIZZLE"}) {
			String label = KmaResponseParser.labelFor(code);
			assertNotNull(label);
			for (String sky : new String[] {"맑음", "흐림", "구름"}) {
				assertEquals(false, label.contains(sky), label);
			}
		}
		assertNull(KmaResponseParser.labelFor(null));
		assertNull(KmaResponseParser.labelFor("CLEAR"));
	}

	@Test
	void rainAmountIsReadAsNumberAndMissingValuesStayUnknown() {
		assertEquals(1.5, KmaResponseParser.parseObservation(ncst("10", "1", "1.5")).observation.precipitationMm, 0.0);
		assertEquals(0.0, KmaResponseParser.parseObservation(ncst("10", "0", "강수없음")).observation.precipitationMm, 0.0);
		assertNull(KmaResponseParser.parseObservation(ncst("10", "0", "-999")).observation.precipitationMm);
		assertNull(KmaResponseParser.parseObservation(ncst("10", "1", "1mm 미만")).observation.precipitationMm);
	}

	@Test
	void missingTemperatureMeansNoObservation() {
		assertEquals(Outcome.MALFORMED, KmaResponseParser.parseObservation(ncst("-999", "0", "0")).outcome);
		assertEquals(Outcome.MALFORMED, KmaResponseParser.parseObservation(ncst("abc", "0", "0")).outcome);
	}

	@Test
	void freezingTemperaturesAreValid() {
		assertEquals(-12.4, KmaResponseParser.parseObservation(ncst("-12.4", "3", "0.0")).observation.temperatureC, 0.0);
	}

	@Test
	void classifiesTheServiceResultCodes() {
		assertEquals(Outcome.NO_DATA, KmaResponseParser.parseObservation(error("03", "NODATA_ERROR")).outcome);
		assertEquals(Outcome.LIMIT_EXCEEDED, KmaResponseParser.parseObservation(error("22", "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR")).outcome);
		assertEquals(Outcome.LIMIT_EXCEEDED, KmaResponseParser.parseObservation(error("23", "x")).outcome);
		for (String code : new String[] {"20", "21", "30", "31", "32", "33"}) {
			assertEquals(Outcome.KEY_REJECTED, KmaResponseParser.parseObservation(error(code, "x")).outcome, code);
		}
		for (String code : new String[] {"01", "02", "04", "05", "10", "11", "12", "99"}) {
			assertEquals(Outcome.SERVICE_ERROR, KmaResponseParser.parseObservation(error(code, "x")).outcome, code);
		}
		assertEquals("22", KmaResponseParser.parseObservation(error("22", "x")).code);
	}

	@Test
	void gatewayErrorsAnsweredAsXmlAreClassifiedToo() {
		String keyError = "<OpenAPI_ServiceResponse><cmmMsgHeader><errMsg>SERVICE ERROR</errMsg>"
				+ "<returnAuthMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</returnAuthMsg>"
				+ "<returnReasonCode>30</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>";
		assertEquals(Outcome.KEY_REJECTED, KmaResponseParser.parseObservation(keyError).outcome);
		String limit = "<OpenAPI_ServiceResponse><cmmMsgHeader><returnReasonCode>22</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>";
		assertEquals(Outcome.LIMIT_EXCEEDED, KmaResponseParser.parseObservation(limit).outcome);
		assertEquals(Outcome.MALFORMED, KmaResponseParser.parseObservation("<html>502 Bad Gateway</html>").outcome);
	}

	@Test
	void emptyOrBrokenBodiesAreMalformedNotCrashes() {
		assertEquals(Outcome.MALFORMED, KmaResponseParser.parseObservation(null).outcome);
		assertEquals(Outcome.MALFORMED, KmaResponseParser.parseObservation("").outcome);
		assertEquals(Outcome.MALFORMED, KmaResponseParser.parseObservation("{\"response\":").outcome);
		assertEquals(Outcome.MALFORMED, KmaResponseParser.parseObservation("{}").outcome);
	}

	@Test
	void okWithoutItemsIsNoData() {
		String empty = "{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":\"\",\"totalCount\":0}}}";
		assertEquals(Outcome.NO_DATA, KmaResponseParser.parseObservation(empty).outcome);
	}

	@Test
	void aSingleItemObjectIsAccepted() {
		String body = "{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{\"item\":"
				+ item("T1H", "5") + "}}}}";
		// only T1H: no precipitation type, so it cannot be a complete observation
		assertEquals(Outcome.MALFORMED, KmaResponseParser.parseObservation(body).outcome);
	}
}
