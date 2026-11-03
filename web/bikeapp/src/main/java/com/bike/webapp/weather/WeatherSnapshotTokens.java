package com.bike.webapp.weather;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Remembers the weather the server looked up at the start of a ride under a random token.
 *
 * <p>The app keeps the token with the ride session and sends it back when the ride is saved; the
 * server then stores <em>its own</em> snapshot, not values supplied by the client. If the token is
 * unknown (expired, or the server restarted) the caller falls back to a fresh lookup by position
 * and start time. In-memory and bounded; nothing here is personal data.</p>
 */
@Component
public class WeatherSnapshotTokens {

	private static final long TTL_MILLIS = 36L * 60L * 60L * 1000L;
	private static final int MAX_ENTRIES = 5_000;

	private static final class Entry {
		final WeatherSnapshot snapshot;
		final long expiresAt;

		Entry(WeatherSnapshot snapshot, long expiresAt) {
			this.snapshot = snapshot;
			this.expiresAt = expiresAt;
		}
	}

	private final SecureRandom random = new SecureRandom();
	private final Clock clock;
	private final Map<String, Entry> entries = new LinkedHashMap<String, Entry>(64, 0.75f, true) {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
			return size() > MAX_ENTRIES;
		}
	};

	public WeatherSnapshotTokens() {
		this(Clock.systemUTC());
	}

	WeatherSnapshotTokens(Clock clock) {
		this.clock = clock;
	}

	public synchronized String put(WeatherSnapshot snapshot) {
		byte[] bytes = new byte[16];
		random.nextBytes(bytes);
		StringBuilder token = new StringBuilder(32);
		for (byte b : bytes) {
			token.append(String.format("%02x", b));
		}
		entries.put(token.toString(), new Entry(snapshot, clock.millis() + TTL_MILLIS));
		return token.toString();
	}

	/** @return the snapshot, or null when the token is unknown or expired */
	public synchronized WeatherSnapshot get(String token) {
		if (token == null || !token.matches("[0-9a-f]{32}")) {
			return null;
		}
		Entry entry = entries.get(token);
		if (entry == null) {
			return null;
		}
		if (entry.expiresAt <= clock.millis()) {
			entries.remove(token);
			return null;
		}
		return entry.snapshot;
	}
}
