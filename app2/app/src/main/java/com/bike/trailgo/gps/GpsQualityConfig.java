package com.bike.trailgo.gps;

/**
 * Every tunable of the GPS quality pipeline in one place. {@link #DEFAULT} is what rides use;
 * tests and future settings can build variants with {@link Builder}.
 *
 * <p>The defaults are deliberately conservative: a fix is only dropped when it is provably stale,
 * out of order, unusable (accuracy worse than {@link #hardMaxHorizontalAccuracyMeters}) or
 * physically impossible to reach. Everything else is either recorded as measured or simply not
 * counted as movement; no coordinate or altitude is ever rewritten.</p>
 */
public final class GpsQualityConfig {

    // ---- position -----------------------------------------------------------------------------
    /** Fixes older than this are cached/stale (e.g. FusedLocation's last known location). */
    public final long maxFixAgeMillis;
    /** Beyond this the fix is not a usable GPS fix (network/cell level) and is rejected outright. */
    public final double hardMaxHorizontalAccuracyMeters;
    /** Worse than this is "poor": deferred while better fixes keep arriving, see patience. */
    public final double poorHorizontalAccuracyMeters;
    /**
     * Poor fixes are deferred for at most this long since the last recorded point (or since the
     * first poor fix when nothing was recorded yet). After that they are recorded anyway so a tunnel
     * or canopy never leaves a hole in the track.
     */
    public final long poorAccuracyPatienceMillis;
    /** Accuracy assumed when the device reports none. */
    public final double unknownAccuracyMeters;
    /** Movement inside this radius around the last recorded point is jitter, not movement. */
    public final double minNoiseRadiusMeters;
    public final double maxNoiseRadiusMeters;
    /**\n     * Noise radius = combined accuracy (anchor and new fix) * factor, clamped to [min, max]. Reported\n     * accuracy is a ~68% radius, so\n     * a factor of 2 keeps ~98% of stationary jitter inside it.\n     */
    public final double noiseRadiusFactor;
    /** Doppler speed under this means "not moving" (only trusted with good speed accuracy). */
    public final double stationarySpeedMps;
    /** While Doppler says stationary, displacement up to radius*factor is still treated as jitter. */
    public final double stationaryDisplacementFactor;
    /** Reported speed accuracy worse than this makes the Doppler speed untrusted. */
    public final double maxTrustedSpeedAccuracyMps;
    /** Reaching a point faster than this is a position jump (default 100 km/h, as before). */
    public final double maxSpeedMps;
    /** After this many mutually consistent "jumps" the new position is accepted as the truth. */
    public final int resyncAfterJumps;

    // ---- elevation ----------------------------------------------------------------------------
    public final double minPlausibleAltitudeMeters;
    public final double maxPlausibleAltitudeMeters;
    /** Altitudes with a reported vertical accuracy worse than this are not used. */
    public final double maxVerticalAccuracyMeters;
    /** Altitude sigma assumed when no vertical accuracy is available (API &lt; 26 or not reported). */
    public final double unknownVerticalSigmaMeters;
    public final double minVerticalSigmaMeters;
    /** Kalman process noise: how fast the true altitude may wander, in m per sqrt(second). */
    public final double verticalProcessSigmaPerSqrtSecond;
    /** Gain/descent is committed when the smoothed altitude moved at least this far from the reference. */
    public final double minGainThresholdMeters;
    /** ... or this many standard deviations of the smoothed altitude, whichever is larger. */
    public final double gainThresholdSigmaFactor;
    /** A sample is an outlier only if it is both statistically and physically implausible. */
    public final double outlierGateSigma;
    public final double maxVerticalSpeedMps;
    /** After this many consistent outliers the altitude is re-baselined (no gain is counted). */
    public final int resyncAfterOutliers;

    public static final GpsQualityConfig DEFAULT = new Builder().build();

    private GpsQualityConfig(Builder b) {
        maxFixAgeMillis = b.maxFixAgeMillis;
        hardMaxHorizontalAccuracyMeters = b.hardMaxHorizontalAccuracyMeters;
        poorHorizontalAccuracyMeters = b.poorHorizontalAccuracyMeters;
        poorAccuracyPatienceMillis = b.poorAccuracyPatienceMillis;
        unknownAccuracyMeters = b.unknownAccuracyMeters;
        minNoiseRadiusMeters = b.minNoiseRadiusMeters;
        maxNoiseRadiusMeters = b.maxNoiseRadiusMeters;
        noiseRadiusFactor = b.noiseRadiusFactor;
        stationarySpeedMps = b.stationarySpeedMps;
        stationaryDisplacementFactor = b.stationaryDisplacementFactor;
        maxTrustedSpeedAccuracyMps = b.maxTrustedSpeedAccuracyMps;
        maxSpeedMps = b.maxSpeedMps;
        resyncAfterJumps = b.resyncAfterJumps;
        minPlausibleAltitudeMeters = b.minPlausibleAltitudeMeters;
        maxPlausibleAltitudeMeters = b.maxPlausibleAltitudeMeters;
        maxVerticalAccuracyMeters = b.maxVerticalAccuracyMeters;
        unknownVerticalSigmaMeters = b.unknownVerticalSigmaMeters;
        minVerticalSigmaMeters = b.minVerticalSigmaMeters;
        verticalProcessSigmaPerSqrtSecond = b.verticalProcessSigmaPerSqrtSecond;
        minGainThresholdMeters = b.minGainThresholdMeters;
        gainThresholdSigmaFactor = b.gainThresholdSigmaFactor;
        outlierGateSigma = b.outlierGateSigma;
        maxVerticalSpeedMps = b.maxVerticalSpeedMps;
        resyncAfterOutliers = b.resyncAfterOutliers;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Mutable builder; every field starts at its default. */
    public static final class Builder {
        long maxFixAgeMillis = 10_000L;
        double hardMaxHorizontalAccuracyMeters = 100.0;
        double poorHorizontalAccuracyMeters = 40.0;
        long poorAccuracyPatienceMillis = 15_000L;
        double unknownAccuracyMeters = 20.0;
        double minNoiseRadiusMeters = 3.0;
        double maxNoiseRadiusMeters = 30.0;
        double noiseRadiusFactor = 2.0;
        double stationarySpeedMps = 0.5;
        double stationaryDisplacementFactor = 3.0;
        double maxTrustedSpeedAccuracyMps = 1.0;
        double maxSpeedMps = 100.0 / 3.6;
        int resyncAfterJumps = 3;
        double minPlausibleAltitudeMeters = -500.0;
        double maxPlausibleAltitudeMeters = 9000.0;
        double maxVerticalAccuracyMeters = 50.0;
        double unknownVerticalSigmaMeters = 5.0;
        double minVerticalSigmaMeters = 3.0;
        double verticalProcessSigmaPerSqrtSecond = 0.7;
        double minGainThresholdMeters = 3.0;
        double gainThresholdSigmaFactor = 2.5;
        double outlierGateSigma = 3.0;
        double maxVerticalSpeedMps = 8.0;
        int resyncAfterOutliers = 3;

        public Builder maxFixAgeMillis(long v) { maxFixAgeMillis = v; return this; }
        public Builder hardMaxHorizontalAccuracyMeters(double v) { hardMaxHorizontalAccuracyMeters = v; return this; }
        public Builder poorHorizontalAccuracyMeters(double v) { poorHorizontalAccuracyMeters = v; return this; }
        public Builder poorAccuracyPatienceMillis(long v) { poorAccuracyPatienceMillis = v; return this; }
        public Builder minNoiseRadiusMeters(double v) { minNoiseRadiusMeters = v; return this; }
        public Builder maxNoiseRadiusMeters(double v) { maxNoiseRadiusMeters = v; return this; }
        public Builder noiseRadiusFactor(double v) { noiseRadiusFactor = v; return this; }
        public Builder maxSpeedMps(double v) { maxSpeedMps = v; return this; }
        public Builder resyncAfterJumps(int v) { resyncAfterJumps = v; return this; }
        public Builder maxVerticalAccuracyMeters(double v) { maxVerticalAccuracyMeters = v; return this; }
        public Builder minGainThresholdMeters(double v) { minGainThresholdMeters = v; return this; }
        public Builder maxVerticalSpeedMps(double v) { maxVerticalSpeedMps = v; return this; }
        public Builder resyncAfterOutliers(int v) { resyncAfterOutliers = v; return this; }

        public GpsQualityConfig build() {
            return new GpsQualityConfig(this);
        }
    }
}
