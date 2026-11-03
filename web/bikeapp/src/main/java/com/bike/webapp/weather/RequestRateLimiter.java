package com.bike.webapp.weather;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Fixed-window request limiter for the public weather endpoint, keyed by caller (rider id or
 * IP). Memory stays bounded: expired windows are dropped and the table is capped.
 */
public final class RequestRateLimiter {

	private static final int MAX_KEYS = 10_000;

	private static final class Window {
		long start;
		int count;
	}

	private final int maxRequests;
	private final long windowMillis;
	private final Map<String, Window> windows = new HashMap<String, Window>();

	public RequestRateLimiter(int maxRequests, long windowMillis) {
		this.maxRequests = maxRequests;
		this.windowMillis = windowMillis;
	}

	/** @return true when the caller may proceed (and the request is counted) */
	public synchronized boolean tryAcquire(String key, long nowMillis) {
		if (windows.size() >= MAX_KEYS) {
			evictExpired(nowMillis);
			if (windows.size() >= MAX_KEYS) {
				windows.clear(); // a flood of distinct keys must not grow memory without bound
			}
		}
		Window window = windows.get(key);
		if (window == null || nowMillis - window.start >= windowMillis) {
			window = new Window();
			window.start = nowMillis;
			windows.put(key, window);
		}
		if (window.count >= maxRequests) {
			return false;
		}
		window.count++;
		return true;
	}

	private void evictExpired(long nowMillis) {
		Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
		while (iterator.hasNext()) {
			if (nowMillis - iterator.next().getValue().start >= windowMillis) {
				iterator.remove();
			}
		}
	}
}
