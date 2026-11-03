package com.bike.webapp.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/** Call budget, request limiter and the token store. */
class WeatherGuardsTest {

	// ---- CallBudget --------------------------------------------------------------------------

	@Test
	void budgetAllowsNCallsPerWindowThenResets() {
		CallBudget budget = new CallBudget(3, 1000);
		assertTrue(budget.tryAcquire(0));
		assertTrue(budget.tryAcquire(10));
		assertTrue(budget.tryAcquire(20));
		assertFalse(budget.tryAcquire(30));
		assertFalse(budget.tryAcquire(999));
		assertTrue(budget.tryAcquire(1000)); // new window
	}

	@Test
	void pausingSuspendsCallsUntilItEnds() {
		CallBudget budget = new CallBudget(100, 1000);
		assertTrue(budget.tryAcquire(0));
		budget.pauseFor(0, 5000);
		assertTrue(budget.isPaused(4999));
		assertFalse(budget.tryAcquire(4999));
		assertFalse(budget.isPaused(5000));
		assertTrue(budget.tryAcquire(5000));
	}

	// ---- RequestRateLimiter ------------------------------------------------------------------

	@Test
	void eachCallerHasItsOwnLimit() {
		RequestRateLimiter limiter = new RequestRateLimiter(2, 600_000);
		assertTrue(limiter.tryAcquire("rider:a", 0));
		assertTrue(limiter.tryAcquire("rider:a", 1));
		assertFalse(limiter.tryAcquire("rider:a", 2));
		assertTrue(limiter.tryAcquire("rider:b", 2));
		assertTrue(limiter.tryAcquire("rider:a", 600_000)); // window over
	}

	@Test
	void aFloodOfDistinctCallersCannotGrowMemoryWithoutBound() {
		RequestRateLimiter limiter = new RequestRateLimiter(1, 600_000);
		for (int i = 0; i < 25_000; i++) {
			assertTrue(limiter.tryAcquire("ip:" + i, i));
		}
		// still works afterwards
		assertTrue(limiter.tryAcquire("ip:new", 30_000));
	}

	// ---- WeatherSnapshotTokens ---------------------------------------------------------------

	private static WeatherSnapshot snapshot() {
		return new WeatherSnapshot(LocalDateTime.of(2026, 10, 10, 19, 0), 88, 90, 21.0, "NONE", 0.0, "강수 없음");
	}

	@Test
	void aTokenGivesBackTheSnapshotTheServerLookedUp() {
		WeatherSnapshotTokens tokens = new WeatherSnapshotTokens();
		String token = tokens.put(snapshot());
		assertTrue(token.matches("[0-9a-f]{32}"));
		assertEquals(21.0, tokens.get(token).getTemperatureC(), 0.0);
		assertNotEquals(token, tokens.put(snapshot()));
	}

	@Test
	void unknownMalformedOrMissingTokensGiveNothing() {
		WeatherSnapshotTokens tokens = new WeatherSnapshotTokens();
		assertNull(tokens.get(null));
		assertNull(tokens.get(""));
		assertNull(tokens.get("../../x"));
		assertNull(tokens.get("0123456789abcdef0123456789abcdef"));
	}

	@Test
	void tokensExpire() {
		final long[] now = {0L};
		Clock clock = new Clock() {
			@Override
			public java.time.ZoneId getZone() {
				return ZoneOffset.UTC;
			}

			@Override
			public Clock withZone(java.time.ZoneId zone) {
				return this;
			}

			@Override
			public Instant instant() {
				return Instant.ofEpochMilli(now[0]);
			}
		};
		WeatherSnapshotTokens tokens = new WeatherSnapshotTokens(clock);
		String token = tokens.put(snapshot());
		now[0] = 35L * 3_600_000L;
		assertNotNull(tokens.get(token));
		now[0] = 37L * 3_600_000L;
		assertNull(tokens.get(token));
	}
}
