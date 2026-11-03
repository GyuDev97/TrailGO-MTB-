package com.bike.trailgo.ui.records;

import androidx.annotation.Nullable;

/** The name a ride gets when it is exported as a GPX file. */
final class GpxExport {
    static final String MIME_TYPE = "application/gpx+xml";
    private static final int MAX_NAME_LENGTH = 60;

    private GpxExport() {
    }

    /**
     * "title_2026-10-13.gpx": the title with everything a file name should not contain replaced by
     * "_", and the date (yyyy-MM-dd) when it is known.
     */
    static String fileName(@Nullable String title, @Nullable String dateText) {
        String base = title == null ? "" : title.trim()
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .replaceAll("\\s+", " ")
                .replaceAll("^[._ ]+|[._ ]+$", "");
        if (base.isEmpty()) {
            base = "TrailGo";
        }
        if (base.length() > MAX_NAME_LENGTH) {
            base = base.substring(0, MAX_NAME_LENGTH).trim();
        }
        String day = dateText != null && dateText.trim().length() >= 10
                && dateText.trim().substring(0, 10).matches("\\d{4}-\\d{2}-\\d{2}")
                ? dateText.trim().substring(0, 10) : null;
        return day == null ? base + ".gpx" : base + "_" + day + ".gpx";
    }
}
