package com.bike.webapp.weather;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Looks up the observed weather near a position and time using the 기상청 초단기실황 only.
 *
 * <p><b>Which observation.</b> 초단기실황 is generated for every whole hour (base_time HH00) and
 * can be called from HH:40 onwards. For a ride that started at {@code at} the observation of the
 * hour at or just before the start is used, unless that one is not published yet, in which case
 * the newest published hour is used. The observation time is always reported as it is.</p>
 *
 * <p><b>Never throws.</b> No key, bad position, timeout, error codes, quota exceeded: all give
 * {@link Optional#empty()}. A "limit exceeded" or "key rejected" answer pauses upstream calls for a
 * while; an hourly call budget guards the key's quota; results are cached per grid cell and base
 * hour so riders in the same area share one call.</p>
 */
@Service
public class WeatherService {

	public static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/** The service publishes the observation of HH:00 and serves it from HH:40. */
	static final int PUBLISH_DELAY_MINUTES = 40;
	/** If the wanted hour has no data yet/any more, one earlier hour is tried when the ride is recent. */
	static final long FALLBACK_MAX_AGE_HOURS = 3;

	private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
	private static final long SUCCESS_TTL_MS = 2L * 60L * 60L * 1000L;
	private static final long NO_DATA_TTL_MS = 3L * 60L * 1000L;
	private static final long ERROR_TTL_MS = 60L * 1000L;
	private static final long LIMIT_PAUSE_MS = 10L * 60L * 1000L;
	private static final long KEY_PAUSE_MS = 30L * 60L * 1000L;
	private static final int MAX_CACHE_ENTRIES = 512;

	private static final class Cached {
		final WeatherSnapshot snapshot; // null = cached failure
		final long expiresAtMillis;

		Cached(WeatherSnapshot snapshot, long expiresAtMillis) {
			this.snapshot = snapshot;
			this.expiresAtMillis = expiresAtMillis;
		}
	}

	private final KmaWeatherClient client;
	private final boolean configured;
	private final Clock clock;
	private final CallBudget budget;
	private final Map<String, Cached> cache = new LinkedHashMap<String, Cached>(64, 0.75f, true) {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Cached> eldest) {
			return size() > MAX_CACHE_ENTRIES;
		}
	};

	@Autowired
	public WeatherService(KmaHttpClient client,
			@Value("${trailgo.weather.kma-service-key:}") String serviceKey,
			@Value("${trailgo.weather.max-upstream-calls-per-hour:300}") int maxCallsPerHour) {
		this(client, serviceKey != null && !serviceKey.trim().isEmpty(), Clock.system(KST),
				new CallBudget(maxCallsPerHour, Duration.ofHours(1).toMillis()));
	}

	WeatherService(KmaWeatherClient client, boolean configured, Clock clock, CallBudget budget) {
		this.client = client;
		this.configured = configured;
		this.clock = clock;
		this.budget = budget;
	}

	public boolean isConfigured() {
		return configured;
	}

	/**
	 * @param at the ride start (Asia/Seoul wall clock); a time in the future is treated as "now"
	 */
	public Optional<WeatherSnapshot> lookup(Double latitude, Double longitude, LocalDateTime at) {
		if (!configured) {
			log.debug("Weather lookup skipped: KMA_SERVICE_KEY is not configured");
			return Optional.empty();
		}
		KmaGridConverter.Grid grid = KmaGridConverter.toGrid(latitude, longitude);
		if (grid == null || at == null) {
			return Optional.empty();
		}
		try {
			LocalDateTime now = LocalDateTime.now(clock);
			LocalDateTime base = observationBase(at, now);
			WeatherSnapshot snapshot = lookupBase(grid, base, now, true);
			return Optional.ofNullable(snapshot);
		} catch (RuntimeException e) {
			log.warn("Weather lookup failed: {} (nx={}, ny={})", e.getClass().getSimpleName(), grid.nx, grid.ny);
			return Optional.empty();
		}
	}

	/**
	 * The observation hour for a ride starting at {@code at}: the hour at or before the start, but
	 * never an hour that is not served yet (HH:40 rule).
	 */
	static LocalDateTime observationBase(LocalDateTime at, LocalDateTime now) {
		LocalDateTime newestServed = now.minusMinutes(PUBLISH_DELAY_MINUTES).truncatedTo(ChronoUnit.HOURS);
		LocalDateTime wanted = (at.isAfter(now) ? now : at).truncatedTo(ChronoUnit.HOURS);
		return wanted.isBefore(newestServed) ? wanted : newestServed;
	}

	private WeatherSnapshot lookupBase(KmaGridConverter.Grid grid, LocalDateTime base, LocalDateTime now,
			boolean allowFallback) {
		String key = grid.nx + ":" + grid.ny + ":" + base;
		long nowMillis = clock.millis();

		synchronized (cache) {
			Cached hit = cache.get(key);
			if (hit != null && hit.expiresAtMillis > nowMillis) {
				return hit.snapshot;
			}
		}

		if (!budget.tryAcquire(nowMillis)) {
			log.warn("Weather lookup skipped: upstream call budget used up or calls paused (nx={}, ny={})", grid.nx, grid.ny);
			return null;
		}

		KmaWeatherClient.RawResponse raw = client.fetchObservation(grid.nx, grid.ny, base);
		long ttl;
		WeatherSnapshot snapshot = null;
		if (raw == null) {
			ttl = ERROR_TTL_MS; // timeout / I/O error
		} else {
			KmaResponseParser.Parsed parsed = KmaResponseParser.parseObservation(raw.body);
			KmaResponseParser.Outcome outcome = parsed.outcome;
			if (raw.status == 429 && outcome == KmaResponseParser.Outcome.MALFORMED) {
				outcome = KmaResponseParser.Outcome.LIMIT_EXCEEDED;
			}
			if ((raw.status == 401 || raw.status == 403) && outcome == KmaResponseParser.Outcome.MALFORMED) {
				// The gateway refuses the key (not activated yet, service not approved for it, blocked):
				// no body to read, but retrying every minute would not help.
				outcome = KmaResponseParser.Outcome.KEY_REJECTED;
			}
			switch (outcome) {
			case OK:
				snapshot = toSnapshot(parsed.observation, grid);
				ttl = snapshot == null ? ERROR_TTL_MS : SUCCESS_TTL_MS;
				break;
			case NO_DATA:
				ttl = NO_DATA_TTL_MS;
				if (allowFallback && Duration.between(base, now).toHours() < FALLBACK_MAX_AGE_HOURS) {
					// Nothing for that hour: use the hour before, and say so through observedAt.
					snapshot = lookupBase(grid, base.minusHours(1), now, false);
				}
				break;
			case LIMIT_EXCEEDED:
				budget.pauseFor(nowMillis, LIMIT_PAUSE_MS);
				log.warn("KMA call limit exceeded (code {}): pausing weather lookups for {} min",
						parsed.code, LIMIT_PAUSE_MS / 60000L);
				return null;
			case KEY_REJECTED:
				budget.pauseFor(nowMillis, KEY_PAUSE_MS);
				log.warn("KMA service key was rejected (code {}): pausing weather lookups for {} min",
						parsed.code, KEY_PAUSE_MS / 60000L);
				return null;
			default:
				log.warn("KMA returned an unusable response (outcome {}, code {}, HTTP {})",
						outcome, parsed.code, raw.status);
				ttl = ERROR_TTL_MS;
				break;
			}
		}
		synchronized (cache) {
			cache.put(key, new Cached(snapshot, nowMillis + ttl));
		}
		return snapshot;
	}

	private static WeatherSnapshot toSnapshot(KmaResponseParser.Observation observation, KmaGridConverter.Grid grid) {
		String label = KmaResponseParser.labelFor(observation.precipitationCode);
		if (label == null) {
			return null;
		}
		return new WeatherSnapshot(observation.observedAt, grid.nx, grid.ny, observation.temperatureC,
				observation.precipitationCode, observation.precipitationMm, label);
	}
}
