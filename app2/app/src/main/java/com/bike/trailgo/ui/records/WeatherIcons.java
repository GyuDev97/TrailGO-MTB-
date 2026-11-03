package com.bike.trailgo.ui.records;

import com.bike.trailgo.R;
import com.bike.trailgo.dto.RecordWeather;

/** Icon for a stored weather: matches what was observed, never implies a clear sky that was not measured. */
public final class WeatherIcons {
    private WeatherIcons() {
    }

    public static int resource(RecordWeather weather) {
        switch (weather.iconKind()) {
            case SUN:
                return R.drawable.records_ic_sun;
            case CLOUD:
                return R.drawable.records_ic_cloud;
            default:
                return R.drawable.ic_weather_thermometer;
        }
    }
}