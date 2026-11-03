package com.bike.trailgo.dto;

import androidx.annotation.Nullable;

/**
 * Weather observed near the start of a ride, as normalised by the TrailGo server. The server asks
 * the 기상청 (초단기실황); the app never talks to it and never interprets its codes.
 *
 * <p>An observation has temperature, precipitation type and precipitation amount, but no sky
 * condition: {@code sky} is only ever present for snapshots stored by an older server version.
 * Only shown when {@link #isAvailable()}.</p>
 */
public class RecordWeather {
    private boolean available;
    private String observedAt;
    private Double temperatureC;
    private String sky;
    private String precipitation;
    private Double precipitationMm;
    private String label;
    // Only present in the answer of the weather lookup: hand it back when the ride is saved.
    private String token;

    public enum Icon { SUN, CLOUD, NEUTRAL }

    public boolean isAvailable() {
        return available;
    }

    @Nullable
    public String getObservedAt() {
        return observedAt;
    }

    @Nullable
    public Double getTemperatureC() {
        return temperatureC;
    }

    @Nullable
    public String getSky() {
        return sky;
    }

    /** NONE, RAIN, RAIN_SNOW, SNOW, SHOWER, DRIZZLE, DRIZZLE_SNOW or SNOW_FLURRY. */
    @Nullable
    public String getPrecipitation() {
        return precipitation;
    }

    /** One-hour precipitation in mm, null when the observation had none to report. */
    @Nullable
    public Double getPrecipitationMm() {
        return precipitationMm;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    @Nullable
    public String getToken() {
        return token;
    }

    public boolean isPrecipitating() {
        return precipitation != null && !"NONE".equals(precipitation);
    }

    /**
     * Which icon fits what was actually observed: precipitation gets the cloud, a clear sky (only
     * from old snapshots) the sun, and "no precipitation" a neutral thermometer, never a sun.
     */
    public Icon iconKind() {
        if (isPrecipitating()) {
            return Icon.CLOUD;
        }
        return "CLEAR".equals(sky) ? Icon.SUN : Icon.NEUTRAL;
    }

    /** A copy without the server token, for storing with a guest record. */
    public RecordWeather withoutToken() {
        RecordWeather copy = new RecordWeather();
        copy.available = available;
        copy.observedAt = observedAt;
        copy.temperatureC = temperatureC;
        copy.sky = sky;
        copy.precipitation = precipitation;
        copy.precipitationMm = precipitationMm;
        copy.label = label;
        return copy;
    }
}