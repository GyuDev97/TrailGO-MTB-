package com.bike.webapp.weather;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.bike.webapp.service.AndroidService;

class WeatherCaptureServiceTest {

	private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 10, 10, 19, 20);
	private final Executor direct = Runnable::run;

	private WeatherService weather(WeatherServiceTest.FakeClient client, boolean configured) {
		return new WeatherService(client, configured,
				Clock.fixed(Instant.parse("2026-10-10T10:49:00Z"), WeatherService.KST), new CallBudget(100, 3_600_000L));
	}

	private WeatherCaptureService capture(WeatherServiceTest.FakeClient client, boolean configured,
			AndroidService androidService, WeatherSnapshotTokens tokens) {
		return new WeatherCaptureService(weather(client, configured), androidService, tokens, direct);
	}

	private static WeatherSnapshot startSnapshot() {
		return new WeatherSnapshot(LocalDateTime.of(2026, 10, 10, 19, 0), 88, 90, 18.5, "RAIN", 1.0, "비");
	}

	@Test
	void theWeatherLookedUpAtTheStartIsTheOneStoredNotANewLookup() {
		AndroidService androidService = mock(AndroidService.class);
		WeatherSnapshotTokens tokens = new WeatherSnapshotTokens();
		String token = tokens.put(startSnapshot());
		WeatherServiceTest.FakeClient client = new WeatherServiceTest.FakeClient();

		capture(client, true, androidService, tokens).captureAsync(123, token, 35.8329, 128.565, STARTED_AT);

		ArgumentCaptor<WeatherSnapshot> stored = ArgumentCaptor.forClass(WeatherSnapshot.class);
		verify(androidService).saveWeatherSnapshot(eq(123), stored.capture());
		assertEquals("비", stored.getValue().getLabel());
		assertEquals(18.5, stored.getValue().getTemperatureC(), 0.0);
		assertEquals(0, client.calls.size()); // no upstream call needed
	}

	@Test
	void anUnknownTokenFallsBackToALookupByPositionAndStartTime() {
		AndroidService androidService = mock(AndroidService.class);
		WeatherServiceTest.FakeClient client = new WeatherServiceTest.FakeClient();

		capture(client, true, androidService, new WeatherSnapshotTokens())
				.captureAsync(123, "0123456789abcdef0123456789abcdef", 35.8329, 128.565, STARTED_AT);

		verify(androidService).saveWeatherSnapshot(eq(123), any(WeatherSnapshot.class));
		assertEquals(1, client.calls.size());
	}

	@Test
	void noTokenAndNoPositionStoresNothing() {
		AndroidService androidService = mock(AndroidService.class);
		WeatherServiceTest.FakeClient client = new WeatherServiceTest.FakeClient();
		WeatherCaptureService service = capture(client, true, androidService, new WeatherSnapshotTokens());

		service.captureAsync(123, null, null, null, STARTED_AT);
		service.captureAsync(123, "", 35.8, null, STARTED_AT);
		verify(androidService, never()).saveWeatherSnapshot(anyInt(), any(WeatherSnapshot.class));
		assertEquals(0, client.calls.size());
	}

	@Test
	void noKeyAndNoTokenLeavesTheWeatherNullAndTheSaveUntouched() {
		AndroidService androidService = mock(AndroidService.class);
		WeatherServiceTest.FakeClient client = new WeatherServiceTest.FakeClient();

		assertDoesNotThrow(() -> capture(client, false, androidService, new WeatherSnapshotTokens())
				.captureAsync(123, null, 35.8329, 128.565, STARTED_AT));
		verify(androidService, never()).saveWeatherSnapshot(anyInt(), any(WeatherSnapshot.class));
	}

	@Test
	void upstreamTimeoutNeverPropagates() {
		AndroidService androidService = mock(AndroidService.class);
		WeatherServiceTest.FakeClient client = new WeatherServiceTest.FakeClient();
		client.timeout = true;

		assertDoesNotThrow(() -> capture(client, true, androidService, new WeatherSnapshotTokens())
				.captureAsync(123, null, 35.8329, 128.565, STARTED_AT));
		verify(androidService, never()).saveWeatherSnapshot(anyInt(), any(WeatherSnapshot.class));
	}

	@Test
	void databaseFailureWhileStoringNeverPropagates() {
		AndroidService androidService = mock(AndroidService.class);
		when(androidService.saveWeatherSnapshot(anyInt(), any(WeatherSnapshot.class)))
				.thenThrow(new IllegalStateException("db down"));
		WeatherSnapshotTokens tokens = new WeatherSnapshotTokens();
		String token = tokens.put(startSnapshot());

		assertDoesNotThrow(() -> capture(new WeatherServiceTest.FakeClient(), true, androidService, tokens)
				.captureAsync(123, token, null, null, STARTED_AT));
	}

	@Test
	void rejectedExecutorNeverPropagates() {
		AndroidService androidService = mock(AndroidService.class);
		Executor rejecting = task -> {
			throw new java.util.concurrent.RejectedExecutionException("full");
		};
		WeatherCaptureService service = new WeatherCaptureService(
				weather(new WeatherServiceTest.FakeClient(), true), androidService, new WeatherSnapshotTokens(), rejecting);

		assertDoesNotThrow(() -> service.captureAsync(123, null, 35.8329, 128.565, STARTED_AT));
	}
}
