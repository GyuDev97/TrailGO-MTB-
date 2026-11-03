package com.bike.webapp.weather;

/**
 * Protects the daily/hourly quota of the 기상청 key (the open-data portal allows a limited number of
 * calls per day and answers 22 / 23 beyond it).
 *
 * <ul>
 * <li>At most {@code maxCallsPerWindow} upstream calls per rolling window (default 1 hour).</li>
 * <li>{@link #pauseFor(long, long)} suspends all calls, used when the service says the limit is
 * exceeded or the key is rejected, so a bad state is not hammered.</li>
 * </ul>
 * Thread-safe; time is passed in so it can be tested without sleeping.
 */
public final class CallBudget {

	private final int maxCallsPerWindow;
	private final long windowMillis;
	private long windowStart = Long.MIN_VALUE;
	private int used;
	private long pausedUntil = Long.MIN_VALUE;

	public CallBudget(int maxCallsPerWindow, long windowMillis) {
		this.maxCallsPerWindow = maxCallsPerWindow;
		this.windowMillis = windowMillis;
	}

	/** @return true when one more upstream call may be made now (and counts it) */
	public synchronized boolean tryAcquire(long nowMillis) {
		if (nowMillis < pausedUntil) {
			return false;
		}
		if (windowStart == Long.MIN_VALUE || nowMillis - windowStart >= windowMillis) {
			windowStart = nowMillis;
			used = 0;
		}
		if (used >= maxCallsPerWindow) {
			return false;
		}
		used++;
		return true;
	}

	public synchronized void pauseFor(long nowMillis, long durationMillis) {
		pausedUntil = Math.max(pausedUntil, nowMillis + durationMillis);
	}

	public synchronized boolean isPaused(long nowMillis) {
		return nowMillis < pausedUntil;
	}
}
