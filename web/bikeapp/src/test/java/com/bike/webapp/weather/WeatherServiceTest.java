package com.bike.webapp.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class WeatherServiceTest {

	// Daegu 앞산 -> grid (88, 90)
	private static final double LAT = 35.8329;
	private static final double LON = 128.5650;

	/** Answers per base hour; records every upstream call. */
	static class FakeClient implements KmaWeatherClient {
		String body = KmaResponseParserTest.ncst("21.0", "0", "0");
		int status = 200;
		boolean timeout;
		boolean throwUnexpected;
		/** base hours that answer NODATA (03) */
		final List<LocalDateTime> noDataFor = new ArrayList<>();
		final List<LocalDateTime> calls = new ArrayList<>();

		@Override
		public RawResponse fetchObservation(int nx, int ny, LocalDateTime base) {
			calls.add(base);
			if (throwUnexpected) {
				throw new IllegalStateException("boom");
			}
			if (timeout) {
				return null;
			}
			if (noDataFor.contains(base)) {
				return new RawResponse(200, KmaResponseParserTest.error("03", "NODATA_ERROR"));
			}
			return new RawResponse(status, body);
		}
	}

	/** A service whose clock stands at the given Asia/Seoul wall time. */
	private static WeatherService service(FakeClient client, boolean configured, String nowKst, CallBudget budget) {
		Instant instant = LocalDateTime.parse(nowKst).atZone(WeatherService.KST).toInstant();
		return new WeatherService(client, configured, Clock.fixed(instant, WeatherService.KST), budget);
	}

	private static WeatherService service(FakeClient client, String nowKst) {
		return service(client, true, nowKst, new CallBudget(100, 3_600_000L));
	}

	private static LocalDateTime t(String iso) {
		return LocalDateTime.parse(iso);
	}

	// ---- which observation hour --------------------------------------------------------------

	@Test
	void anObservationIsServedFromFortyMinutesPastTheHour() {
		// now 19:39 -> 19:00 is not served yet, 18:00 is the newest
		assertEquals(t("2026-10-10T18:00:00"), WeatherService.observationBase(t("2026-10-10T19:39:00"), t("2026-10-10T19:39:00")));
		// now 19:40 -> 19:00 is served
		assertEquals(t("2026-10-10T19:00:00"), WeatherService.observationBase(t("2026-10-10T19:40:00"), t("2026-10-10T19:40:00")));
	}

	@Test
	void aRideUsesTheObservationAtOrJustBeforeItsStart() {
		LocalDateTime now = t("2026-10-10T21:15:00");
		// started 19:20 -> the 19:00 observation (not 20:00, which is after the start)
		assertEquals(t("2026-10-10T19:00:00"), WeatherService.observationBase(t("2026-10-10T19:20:00"), now));
		// started exactly at 19:00
		assertEquals(t("2026-10-10T19:00:00"), WeatherService.observationBase(t("2026-10-10T19:00:00"), now));
	}

	@Test
	void aFreshlystartedRideFallsBackToTheNewestServedHour() {
		// started 19:20, asked at 19:25: the 19:00 observation is not served until 19:40 -> 18:00
		assertEquals(t("2026-10-10T18:00:00"), WeatherService.observationBase(t("2026-10-10T19:20:00"), t("2026-10-10T19:25:00")));
	}

	@Test
	void aStartInTheFutureIsTreatedAsNow() {
		assertEquals(t("2026-10-10T18:00:00"), WeatherService.observationBase(t("2026-10-10T23:00:00"), t("2026-10-10T19:25:00")));
	}

	@Test
	void theHourBeforeMidnightRollsOverTheDate() {
		assertEquals(t("2026-10-09T23:00:00"), WeatherService.observationBase(t("2026-10-10T00:10:00"), t("2026-10-10T00:10:00")));
		assertEquals(t("2026-10-10T00:00:00"), WeatherService.observationBase(t("2026-10-10T00:45:00"), t("2026-10-10T00:45:00")));
	}

	// ---- results -----------------------------------------------------------------------------

	@Test
	void returnsTheNormalisedObservationWithoutAnySkyCondition() {
		FakeClient client = new FakeClient();
		client.body = KmaResponseParserTest.ncst("21.0", "1", "2.5");
		WeatherSnapshot snapshot = service(client, "2026-10-10T19:49:00").lookup(LAT, LON, t("2026-10-10T19:20:00")).get();

		assertEquals(21.0, snapshot.getTemperatureC(), 0.0);
		assertEquals("RAIN", snapshot.getPrecipitationCode());
		assertEquals(2.5, snapshot.getPrecipitationMm(), 0.0);
		assertEquals("비", snapshot.getLabel());
		assertEquals(null, snapshot.getSkyCode());
		assertEquals(88, snapshot.getNx());
		assertEquals(90, snapshot.getNy());
		assertEquals(1, client.calls.size());
		assertEquals(t("2026-10-10T19:00:00"), client.calls.get(0));
	}

	@Test
	void noPrecipitationIsSaidAsSuchNotAsSunshine() {
		FakeClient client = new FakeClient();
		WeatherSnapshot snapshot = service(client, "2026-10-10T19:49:00").lookup(LAT, LON, t("2026-10-10T19:20:00")).get();
		assertEquals("강수 없음", snapshot.getLabel());
		assertEquals("NONE", snapshot.getPrecipitationCode());
	}

	@Test
	void withoutServiceKeyNothingIsCalled() {
		FakeClient client = new FakeClient();
		assertFalse(service(client, false, "2026-10-10T19:49:00", new CallBudget(10, 1000)).lookup(LAT, LON, t("2026-10-10T19:20:00")).isPresent());
		assertTrue(client.calls.isEmpty());
	}

	@Test
	void badOrMissingPositionNeverCallsTheService() {
		FakeClient client = new FakeClient();
		WeatherService service = service(client, "2026-10-10T19:49:00");
		assertFalse(service.lookup(null, null, t("2026-10-10T19:20:00")).isPresent());
		assertFalse(service.lookup(0.0, 0.0, t("2026-10-10T19:20:00")).isPresent());
		assertFalse(service.lookup(LAT, LON, null).isPresent());
		assertTrue(client.calls.isEmpty());
	}

	@Test
	void timeoutsAndUnexpectedErrorsGiveNothingAndDoNotThrow() {
		FakeClient timeout = new FakeClient();
		timeout.timeout = true;
		assertFalse(service(timeout, "2026-10-10T19:49:00").lookup(LAT, LON, t("2026-10-10T19:20:00")).isPresent());

		FakeClient broken = new FakeClient();
		broken.throwUnexpected = true;
		assertFalse(service(broken, "2026-10-10T19:49:00").lookup(LAT, LON, t("2026-10-10T19:20:00")).isPresent());

		FakeClient serverError = new FakeClient();
		serverError.status = 500;
		serverError.body = "<html>500</html>";
		assertFalse(service(serverError, "2026-10-10T19:49:00").lookup(LAT, LON, t("2026-10-10T19:20:00")).isPresent());
	}

	@Test
	void noDataFallsBackToTheHourBeforeForARecentRideAndSaysWhichHour() {
		FakeClient client = new FakeClient();
		client.noDataFor.add(t("2026-10-10T19:00:00"));
		WeatherSnapshot snapshot = service(client, "2026-10-10T19:49:00").lookup(LAT, LON, t("2026-10-10T19:20:00")).get();
		assertEquals(2, client.calls.size());
		assertEquals(t("2026-10-10T18:00:00"), client.calls.get(1));
		// the body of the fake always says 19:00; the point is the second call, and only one extra
	}

	@Test
	void noDataForAnOldRideIsNotRetriedEndlessly() {
		FakeClient client = new FakeClient();
		client.noDataFor.add(t("2026-10-10T08:00:00"));
		assertFalse(service(client, "2026-10-10T19:49:00").lookup(LAT, LON, t("2026-10-10T08:20:00")).isPresent());
		assertEquals(1, client.calls.size());
	}

	// ---- protecting the key's quota ----------------------------------------------------------

	@Test
	void sameCellAndHourIsServedFromTheCache() {
		FakeClient client = new FakeClient();
		WeatherService service = service(client, "2026-10-10T19:49:00");
		service.lookup(LAT, LON, t("2026-10-10T19:20:00"));
		service.lookup(LAT + 0.0001, LON + 0.0001, t("2026-10-10T19:35:00"));
		assertEquals(1, client.calls.size());
		service.lookup(LAT, LON, t("2026-10-10T18:20:00")); // another hour
		assertEquals(2, client.calls.size());
	}

	@Test
	void failuresAreRememberedBrieflySoTheServiceIsNotHammered() {
		FakeClient client = new FakeClient();
		client.timeout = true;
		WeatherService service = service(client, "2026-10-10T19:49:00");
		service.lookup(LAT, LON, t("2026-10-10T19:20:00"));
		service.lookup(LAT, LON, t("2026-10-10T19:20:00"));
		assertEquals(1, client.calls.size());
	}

	@Test
	void limitExceededPausesAllUpstreamCalls() {
		FakeClient client = new FakeClient();
		client.body = KmaResponseParserTest.error("22", "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR");
		CallBudget budget = new CallBudget(100, 3_600_000L);
		WeatherService service = service(client, true, "2026-10-10T19:49:00", budget);

		assertFalse(service.lookup(LAT, LON, t("2026-10-10T19:20:00")).isPresent());
		assertEquals(1, client.calls.size());
		// another cell and hour while paused: no new call
		assertFalse(service.lookup(37.5665, 126.9780, t("2026-10-10T17:20:00")).isPresent());
		assertEquals(1, client.calls.size());
	}

	@Test
	void aRejectedKeyPausesCallsToo() {
		FakeClient client = new FakeClient();
		client.body = KmaResponseParserTest.error("30", "SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
		WeatherService service = service(client, "2026-10-10T19:49:00");
		assertFalse(service.lookup(LAT, LON, t("2026-10-10T19:20:00")).isPresent());
		assertFalse(service.lookup(37.5665, 126.9780, t("2026-10-10T19:20:00")).isPresent());
		assertEquals(1, client.calls.size());
	}

	@Test
	void anHttp429WithoutABodyCountsAsLimitExceeded() {
		FakeClient client = new FakeClient();
		client.status = 429;
		client.body = "";
		WeatherService service = service(client, "2026-10-10T19:49:00");
		assertFalse(service.lookup(LAT, LON, t("2026-10-10T19:20:00")).isPresent());
		assertFalse(service.lookup(37.5665, 126.9780, t("2026-10-10T19:20:00")).isPresent());
		assertEquals(1, client.calls.size());
	}

	@Test
	void theHourlyCallBudgetIsEnforced() {
		FakeClient client = new FakeClient();
		WeatherService service = service(client, true, "2026-10-10T19:49:00", new CallBudget(2, 3_600_000L));
		assertTrue(service.lookup(LAT, LON, t("2026-10-10T19:20:00")).isPresent());
		assertTrue(service.lookup(LAT, LON, t("2026-10-10T18:20:00")).isPresent());
		assertFalse(service.lookup(LAT, LON, t("2026-10-10T17:20:00")).isPresent());
		assertEquals(2, client.calls.size());
	}

	@Test
	void exposesWhetherAKeyIsConfigured() {
		assertTrue(service(new FakeClient(), "2026-10-10T19:49:00").isConfigured());
		assertFalse(service(new FakeClient(), false, "2026-10-10T19:49:00", new CallBudget(1, 1)).isConfigured());
		assertEquals(Optional.empty(), service(new FakeClient(), false, "2026-10-10T19:49:00", new CallBudget(1, 1)).lookup(LAT, LON, t("2026-10-10T19:00:00")));
	}

	@Test
	void anHttp403FromTheGatewayPausesCallsLikeARejectedKey() {
		FakeClient client = new FakeClient();
		client.status = 403;
		client.body = "";
		WeatherService service = service(client, "2026-10-10T19:49:00");
		assertFalse(service.lookup(LAT, LON, t("2026-10-10T19:20:00")).isPresent());
		assertFalse(service.lookup(37.5665, 126.9780, t("2026-10-10T19:20:00")).isPresent());
		assertEquals(1, client.calls.size());
	}}
