package com.bike.trailgo.ui.records;

import java.util.Locale;

/**
 * Single place that turns stored ride values into display text, shared by the record list and the
 * record detail screen.
 *
 * <p>Stored units (see {@code RidingRecord}): distance in metres, time in seconds, speeds and
 * elevation gain as whole numbers. Speeds are truncated to whole km/h when a ride is saved, so a
 * stored {@code 0} only means "below 1 km/h".</p>
 *
 * <p>Pure Java so it can be unit tested on the JVM.</p>
 */
public final class RecordFormatter {
    public static final String EMPTY = "-";

    /** Rough cycling estimate. It is not physiological data, so the UI labels it "추정". */
    static final double ESTIMATED_KCAL_PER_KM = 50.0;
    private static final double SUSPICIOUS_DURATION_SECONDS = 24 * 3600;

    private RecordFormatter() {
    }

    /** Below 1 km the exact metres are shown ("50 m"), otherwise kilometres ("1.25 km"). */
    public static String formatDistance(int meters) {
        if (meters <= 0) {
            return EMPTY;
        }
        if (meters < 1000) {
            return meters + " m";
        }
        return String.format(Locale.KOREA, "%.2f km", meters / 1000.0);
    }

    public static String formatDuration(int seconds) {
        if (seconds <= 0) {
            return EMPTY;
        }
        if (seconds < 60) {
            return "1분 미만";
        }
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "시간 " + minutes + "분";
        }
        return minutes + "분";
    }

    /**
     * @param kmh      stored (truncated) speed
     * @param distance stored distance in metres
     * @param seconds  stored riding time in seconds
     */
    public static String formatSpeed(int kmh, int distance, int seconds) {
        if (distance <= 0 || seconds <= 0 || kmh < 0) {
            return EMPTY;
        }
        if (kmh == 0) {
            return "1 km/h 미만";
        }
        return kmh + " km/h";
    }

    /** Accumulated climbing, not altitude above sea level. */
    public static String formatElevationGain(int meters) {
        return Math.max(0, meters) + " m";
    }

    /** Whole metres above the GPS reference, e.g. a GPX minimum/maximum. */
    public static String formatElevation(double meters) {
        return Math.round(meters) + " m";
    }

    public static int estimateCalories(int meters) {
        if (meters <= 0) {
            return 0;
        }
        return (int) Math.round(meters / 1000.0 * ESTIMATED_KCAL_PER_KM);
    }

    public static String formatEstimatedCalories(int meters) {
        int kcal = estimateCalories(meters);
        return kcal <= 0 ? EMPTY : "약 " + kcal + " kcal";
    }

    /**
     * "맑음 · 21°C" for a weather snapshot stored with the ride, or null when it must not be shown
     * (not available, no label, no temperature). Never invents a value.
     */
    public static String formatWeather(boolean available, String label, Double temperatureC) {
        if (!available || label == null || label.trim().isEmpty()
                || temperatureC == null || temperatureC.isNaN() || temperatureC.isInfinite()) {
            return null;
        }
        double rounded = Math.round(temperatureC * 10.0) / 10.0;
        String temperature = rounded == Math.rint(rounded)
                ? String.format(Locale.KOREA, "%d°C", (long) rounded)
                : String.format(Locale.KOREA, "%.1f°C", rounded);
        return label.trim() + " · " + temperature;
    }

    /** "18:00 관측" style time of an ISO timestamp such as 2026-10-10T18:00:00+09:00, or null when unreadable. */
    public static String formatObservedTime(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.length() < 16 || isoTimestamp.charAt(10) != 'T' || isoTimestamp.charAt(13) != ':') {
            return null;
        }
        String hhmm = isoTimestamp.substring(11, 16);
        if (!hhmm.matches("[0-2][0-9]:[0-5][0-9]")) {
            return null;
        }
        return hhmm + " 관측";
    }

    /** "1.5mm" / "2mm" for a measured precipitation amount; null when there was none or it is unknown. */
    public static String formatPrecipitationAmount(Double mm) {
        if (mm == null || mm.isNaN() || mm.isInfinite() || mm <= 0.0) {
            return null;
        }
        double rounded = Math.round(mm * 10.0) / 10.0;
        return rounded == Math.rint(rounded)
                ? String.format(Locale.KOREA, "%dmm", (long) rounded)
                : String.format(Locale.KOREA, "%.1fmm", rounded);
    }

    /** "시작 시각 기준 · 18:00 관측 · 1시간 강수 1.5mm": what the weather line says about its source. */
    public static String formatWeatherNote(String observedAtIso, Double precipitationMm) {
        StringBuilder note = new StringBuilder("시작 시각 기준");
        String observed = formatObservedTime(observedAtIso);
        if (observed != null) {
            note.append(" · ").append(observed);
        }
        String amount = formatPrecipitationAmount(precipitationMm);
        if (amount != null) {
            note.append(" · 1시간 강수 ").append(amount);
        }
        return note.toString();
    }
    public static boolean hasMeasurement(int meters, int seconds) {
        return meters > 0 && seconds > 0;
    }

    /**
     * True when the stored riding time cannot plausibly belong to the stored distance: more than a
     * day, or an hour or more that implies an average below 1 km/h. The values are still shown
     * as-is; this only lets the screen warn that the record may be wrong.
     */
    public static boolean isSuspiciousDuration(int meters, int seconds) {
        if (seconds <= 0) {
            return false;
        }
        if (seconds >= SUSPICIOUS_DURATION_SECONDS) {
            return true;
        }
        return seconds >= 3600 && meters * 3.6 / seconds < 1.0;
    }

    /** Reads back {@link #formatDistance(int)} output ("50 m", "1.25 km"). -1 when unreadable. */
    public static int parseDistanceMeters(String text) {
        if (text == null) {
            return -1;
        }
        String value = text.trim().toLowerCase(Locale.ROOT);
        try {
            if (value.endsWith("km")) {
                return (int) Math.round(Double.parseDouble(value.substring(0, value.length() - 2).trim()) * 1000.0);
            }
            if (value.endsWith("m")) {
                return (int) Math.round(Double.parseDouble(value.substring(0, value.length() - 1).trim()));
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return -1;
    }

    /** Reads back {@link #formatDuration(int)} output ("1시간 5분", "12분"). -1 when unreadable. */
    public static int parseDurationSeconds(String text) {
        if (text == null) {
            return -1;
        }
        String value = text.trim();
        if (value.equals("1분 미만")) {
            return 30;
        }
        try {
            int hourIndex = value.indexOf("시간");
            int hours = 0;
            String rest = value;
            if (hourIndex >= 0) {
                hours = Integer.parseInt(value.substring(0, hourIndex).trim());
                rest = value.substring(hourIndex + 2).trim();
            }
            int minutes = 0;
            if (rest.endsWith("분")) {
                minutes = Integer.parseInt(rest.substring(0, rest.length() - 1).trim());
            } else if (!rest.isEmpty()) {
                return -1;
            }
            return hours * 3600 + minutes * 60;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
