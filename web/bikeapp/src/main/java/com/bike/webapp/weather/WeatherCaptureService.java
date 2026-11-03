package com.bike.webapp.weather;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bike.webapp.service.AndroidService;

/**
 * Attaches a weather snapshot to a ride that was just saved.
 *
 * <p>It runs after the record is committed and off the request thread, so a missing key, a slow or
 * failing KMA API, or a full queue can only ever leave the weather columns NULL; saving and reading
 * rides is never affected. The snapshot is written once ("only if still NULL") and is never
 * refreshed afterwards.</p>
 */
@Service
public class WeatherCaptureService {

	private static final Logger log = LoggerFactory.getLogger(WeatherCaptureService.class);

	private final WeatherService weatherService;
	private final AndroidService androidService;
	private final WeatherSnapshotTokens tokens;
	private final Executor executor;
	private final ExecutorService ownedExecutor;

	@Autowired
	public WeatherCaptureService(WeatherService weatherService, AndroidService androidService,
			WeatherSnapshotTokens tokens) {
		this.weatherService = weatherService;
		this.androidService = androidService;
		this.tokens = tokens;
		RejectedExecutionHandler dropAndLog = (task, pool) -> log.warn("Weather capture queue is full; skipping snapshot");
		ThreadFactory daemon = runnable -> {
			Thread thread = new Thread(runnable, "weather-capture");
			thread.setDaemon(true);
			return thread;
		};
		this.ownedExecutor = new ThreadPoolExecutor(1, 2, 60L, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(100), daemon, dropAndLog);
		this.executor = ownedExecutor;
	}

	/** For tests: lets the capture run synchronously. */
	WeatherCaptureService(WeatherService weatherService, AndroidService androidService,
			WeatherSnapshotTokens tokens, Executor executor) {
		this.weatherService = weatherService;
		this.androidService = androidService;
		this.tokens = tokens;
		this.executor = executor;
		this.ownedExecutor = null;
	}

	/** Schedules the snapshot by position and start time (no token). */
	public void captureAsync(final int rrNum, final Double latitude, final Double longitude,
			final LocalDateTime startedAtKst) {
		captureAsync(rrNum, null, latitude, longitude, startedAtKst);
	}

	/**
	 * Schedules the snapshot. Never throws and never blocks on the network.
	 *
	 * <p>If the app sent the token it got when the ride started, the snapshot looked up <em>then</em>
	 * is stored. Otherwise (no token, or the token expired / the server restarted) the weather is
	 * looked up now for the start position and time. Values from the client are never stored.</p>
	 */
	public void captureAsync(final int rrNum, final String token, final Double latitude, final Double longitude,
			final LocalDateTime startedAtKst) {
		boolean hasToken = token != null && !token.isEmpty();
		if (!hasToken && (latitude == null || longitude == null)) {
			return;
		}
		try {
			executor.execute(() -> capture(rrNum, token, latitude, longitude, startedAtKst));
		} catch (RuntimeException e) {
			log.warn("Weather capture could not be scheduled: {}", e.getClass().getSimpleName());
		}
	}

	void capture(int rrNum, String token, Double latitude, Double longitude, LocalDateTime startedAtKst) {
		try {
			WeatherSnapshot snapshot = tokens == null ? null : tokens.get(token);
			if (snapshot == null && latitude != null && longitude != null) {
				Optional<WeatherSnapshot> looked = weatherService.lookup(latitude, longitude, startedAtKst);
				snapshot = looked.isPresent() ? looked.get() : null;
			}
			if (snapshot == null) {
				return;
			}
			int updated = androidService.saveWeatherSnapshot(rrNum, snapshot);
			log.debug("Weather snapshot stored for record {} (rows={})", rrNum, updated);
		} catch (RuntimeException e) {
			log.warn("Weather snapshot for record {} was not stored: {}", rrNum, e.getClass().getSimpleName());
		}
	}
	@PreDestroy
	void shutdown() {
		if (ownedExecutor != null) {
			ownedExecutor.shutdown();
		}
	}
}
