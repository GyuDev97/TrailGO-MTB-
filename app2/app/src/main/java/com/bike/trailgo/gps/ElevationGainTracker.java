package com.bike.trailgo.gps;

/**
 * Accumulates climbing and descent from noisy GPS altitudes.
 *
 * <ol>
 * <li>Only real altitudes are used: missing/NaN/implausible values and altitudes whose reported
 * vertical accuracy is poor are skipped, never treated as 0 m. The first usable altitude becomes the
 * baseline; it does not count as gain.</li>
 * <li>A 1-D Kalman filter smooths the altitude. Its measurement noise follows the reported vertical
 * accuracy (API 26+) or a default sigma, and its process noise allows genuine climbs/descents, so
 * steep sections are followed, not clipped. No fixed grade or distance limit is applied.</li>
 * <li>Single wild samples (statistically and physically implausible, see
 * {@link GpsQualityConfig#maxVerticalSpeedMps}) are ignored; if the new level persists it is
 * adopted as the new baseline without counting any gain.</li>
 * <li>Gain/descent are committed with hysteresis: the smoothed altitude must move away from the last
 * reference by at least max(minThreshold, k * sigma) before it counts.</li>
 * </ol>
 *
 * <p>Only feed it fixes that were recorded as movement (stationary jitter must not reach it).
 * Pure Java.</p>
 */
public final class ElevationGainTracker {

    public enum Status { SKIPPED_NO_ALTITUDE, SKIPPED_POOR_ACCURACY, BASELINE, UPDATED, OUTLIER, RESYNC }

    private final GpsQualityConfig config;
    private double gainMeters;
    private double descentMeters;

    private boolean initialized;
    private double estimate;
    private double variance;
    private double reference;
    private long lastNanos;
    private int outlierRun;
    private double outlierAltitude;
    private double lastRawAltitude;

    public ElevationGainTracker(GpsQualityConfig config) {
        this.config = config;
    }

    public double getGainMeters() {
        return gainMeters;
    }

    public double getDescentMeters() {
        return descentMeters;
    }

    /** Smoothed altitude, or NaN before the first usable sample. */
    public double getSmoothedAltitude() {
        return initialized ? estimate : Double.NaN;
    }

    /** Forget the filter state (ride paused/restored) but keep the totals. */
    public void resetBaseline() {
        initialized = false;
        outlierRun = 0;
    }

    /** Whether this fix carries an altitude that may be recorded and used. */
    public boolean isUsable(GpsFix fix) {
        if (!fix.hasAltitude()) {
            return false;
        }
        double altitude = fix.altitudeMeters;
        if (altitude < config.minPlausibleAltitudeMeters || altitude > config.maxPlausibleAltitudeMeters) {
            return false;
        }
        return !(fix.hasVerticalAccuracy() && fix.verticalAccuracyMeters > config.maxVerticalAccuracyMeters);
    }

    public Status onFix(GpsFix fix) {
        if (!fix.hasAltitude() || fix.altitudeMeters < config.minPlausibleAltitudeMeters
                || fix.altitudeMeters > config.maxPlausibleAltitudeMeters) {
            return Status.SKIPPED_NO_ALTITUDE;
        }
        if (!isUsable(fix)) {
            return Status.SKIPPED_POOR_ACCURACY;
        }
        double altitude = fix.altitudeMeters;
        double sigma = fix.hasVerticalAccuracy()
                ? Math.max(fix.verticalAccuracyMeters, config.minVerticalSigmaMeters)
                : config.unknownVerticalSigmaMeters;
        double measurementVariance = sigma * sigma;
        long t = fix.elapsedRealtimeNanos;

        if (!initialized) {
            estimate = altitude;
            variance = measurementVariance;
            reference = altitude;
            lastNanos = t;
            lastRawAltitude = altitude;
            outlierRun = 0;
            initialized = true;
            return Status.BASELINE;
        }

        double seconds = t > lastNanos ? (t - lastNanos) / 1e9 : 0.0;
        double predictedVariance = variance
                + config.verticalProcessSigmaPerSqrtSecond * config.verticalProcessSigmaPerSqrtSecond * seconds;
        double innovation = altitude - estimate;
        double innovationSigma = Math.sqrt(predictedVariance + measurementVariance);
        // A spike is a jump between consecutive raw samples that no cyclist could make AND that the
        // smoothed track does not explain. Comparing raw samples (not the smoothed estimate) keeps a
        // steady steep climb, where the estimate lags a few metres behind, from looking like a spike.
        double rawStep = Math.abs(altitude - lastRawAltitude);
        // ... and the step must also be well beyond what the sample noise alone can produce.
        double spikeStep = Math.max(config.maxVerticalSpeedMps * Math.max(seconds, 0.5),
                config.outlierGateSigma * Math.sqrt(2.0) * sigma);
        lastRawAltitude = altitude;
        boolean gateExceeded = Math.abs(innovation) > config.outlierGateSigma * innovationSigma;
        boolean spike = gateExceeded && rawStep > spikeStep;
        // Once a spike was seen, samples that stay at that new level are judged together.
        boolean continuing = gateExceeded && outlierRun > 0
                && Math.abs(altitude - outlierAltitude) <= config.outlierGateSigma * sigma;

        if (spike || continuing) {
            if (outlierRun > 0 && Math.abs(altitude - outlierAltitude) <= config.outlierGateSigma * sigma) {
                outlierRun++;
            } else {
                outlierRun = 1;
            }
            outlierAltitude = altitude;
            if (outlierRun >= config.resyncAfterOutliers) {
                estimate = altitude;
                variance = measurementVariance;
                reference = altitude;
                lastNanos = t;
                outlierRun = 0;
                return Status.RESYNC;
            }
            // keep the state, but let time pass so the uncertainty keeps growing
            variance = predictedVariance;
            lastNanos = t;
            return Status.OUTLIER;
        }

        outlierRun = 0;
        double gain = predictedVariance / (predictedVariance + measurementVariance);
        estimate += gain * innovation;
        variance = (1.0 - gain) * predictedVariance;
        lastNanos = t;

        double threshold = Math.max(config.minGainThresholdMeters,
                config.gainThresholdSigmaFactor * Math.sqrt(variance));
        double moved = estimate - reference;
        if (moved >= threshold) {
            gainMeters += moved;
            reference = estimate;
        } else if (-moved >= threshold) {
            descentMeters += -moved;
            reference = estimate;
        }
        return Status.UPDATED;
    }
}
