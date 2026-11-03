package com.bike.trailgo.hazard;

import com.bike.trailgo.gps.GeoMath;

/**
 * When to ask the server for hazards again. A location update never triggers a request by itself: a new
 * request needs real movement or enough time, no request in flight, and respects a back-off after failures.
 */
public final class HazardRefreshPolicy {
    public static final int QUERY_RADIUS_M = 2000;
    public static final double REFRESH_DISTANCE_M = 800.0;
    public static final long REFRESH_INTERVAL_MS = 15L * 60L * 1000L;
    public static final long MIN_ATTEMPT_INTERVAL_MS = 30_000L;
    public static final long MAX_BACKOFF_MS = 5L * 60L * 1000L;

    private final double refreshDistanceM;
    private final long refreshIntervalMs;

    private boolean inFlight;
    private long lastAttemptMs = Long.MIN_VALUE;
    private long lastSuccessMs = Long.MIN_VALUE;
    private double centerLat = Double.NaN;
    private double centerLon = Double.NaN;
    private int failures;

    public HazardRefreshPolicy() {
        this(REFRESH_DISTANCE_M, REFRESH_INTERVAL_MS);
    }

    public HazardRefreshPolicy(double refreshDistanceM, long refreshIntervalMs) {
        this.refreshDistanceM = refreshDistanceM;
        this.refreshIntervalMs = refreshIntervalMs;
    }

    public synchronized boolean shouldRefresh(double lat, double lon, long nowMs) {
        if (inFlight) return false;
        if (lastAttemptMs != Long.MIN_VALUE && nowMs - lastAttemptMs < waitAfterAttempt()) return false;
        if (lastSuccessMs == Long.MIN_VALUE) return true;
        if (nowMs - lastSuccessMs >= refreshIntervalMs) return true;
        return GeoMath.distanceMeters(centerLat, centerLon, lat, lon) >= refreshDistanceM;
    }

    /** Call when a request is sent. */
    public synchronized void onAttempt(long nowMs) {
        inFlight = true;
        lastAttemptMs = nowMs;
    }

    public synchronized void onSuccess(double lat, double lon, long nowMs) {
        inFlight = false;
        failures = 0;
        lastSuccessMs = nowMs;
        centerLat = lat;
        centerLon = lon;
    }

    public synchronized void onFailure() {
        inFlight = false;
        failures++;
    }

    /** Forgets everything: the next fix of a new ride refreshes at once. */
    public synchronized void reset() {
        inFlight = false;
        lastAttemptMs = Long.MIN_VALUE;
        lastSuccessMs = Long.MIN_VALUE;
        centerLat = Double.NaN;
        centerLon = Double.NaN;
        failures = 0;
    }

    private long waitAfterAttempt() {
        if (failures == 0) return MIN_ATTEMPT_INTERVAL_MS;
        long backoff = 60_000L << Math.min(failures - 1, 10);
        return Math.min(MAX_BACKOFF_MS, backoff);
    }
}
