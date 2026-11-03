package com.bike.trailgo.ui.home;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the {@code rr_date} of a ride record.
 *
 * <p>Two shapes exist: the usual wall-clock text {@code "2026-10-10 19:49:00"} (taken as it is:
 * it already is the local time of whoever saved it) and an ISO timestamp with an offset such as
 * {@code "2026-10-10T10:49:00.000+00:00"} or {@code ...Z}, which is converted into the given zone
 * so that "which month is this ride in" is decided in Korea time, not UTC.</p>
 */
public final class RecordDates {
    public static final TimeZone SEOUL = TimeZone.getTimeZone("Asia/Seoul");

    private static final Pattern WALL_CLOCK = Pattern.compile(
            "^(\\d{4})-(\\d{2})-(\\d{2})(?:[ T](\\d{2}):(\\d{2})(?::(\\d{2}))?(?:\\.\\d+)?)?$");
    private static final Pattern WITH_OFFSET = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}:\\d{2})(?:\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})$");

    /** A calendar date and time of day; {@code hour} is -1 when the text had no time. */
    public static final class Parts {
        public final int year;
        public final int month;
        public final int day;
        public final int hour;
        public final int minute;

        Parts(int year, int month, int day, int hour, int minute) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.hour = hour;
            this.minute = minute;
        }

        public boolean hasTime() {
            return hour >= 0;
        }
    }

    private RecordDates() {
    }

    /** @return the parts, or null when the text is not a recognisable date */
    public static Parts parse(String raw, TimeZone zoneForOffsetDates) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        Matcher offset = WITH_OFFSET.matcher(text);
        if (offset.matches()) {
            return parseWithOffset(offset, zoneForOffsetDates);
        }
        Matcher wall = WALL_CLOCK.matcher(text);
        if (!wall.matches()) {
            return null;
        }
        int year = Integer.parseInt(wall.group(1));
        int month = Integer.parseInt(wall.group(2));
        int day = Integer.parseInt(wall.group(3));
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            return null;
        }
        if (wall.group(4) == null) {
            return new Parts(year, month, day, -1, 0);
        }
        int hour = Integer.parseInt(wall.group(4));
        int minute = Integer.parseInt(wall.group(5));
        if (hour > 23 || minute > 59) {
            return null;
        }
        return new Parts(year, month, day, hour, minute);
    }

    private static Parts parseWithOffset(Matcher offset, TimeZone zone) {
        String zoneText = offset.group(3);
        if ("Z".equals(zoneText)) {
            zoneText = "+0000";
        } else if (zoneText.length() == 6) {
            zoneText = zoneText.substring(0, 3) + zoneText.substring(4);
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT);
            format.setLenient(false);
            Date instant = format.parse(offset.group(1) + "T" + offset.group(2) + zoneText);
            Calendar calendar = Calendar.getInstance(zone, Locale.ROOT);
            calendar.setTime(instant);
            return new Parts(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE));
        } catch (ParseException error) {
            return null;
        }
    }

    /** The current year/month in the given zone, as {@code year * 100 + month} (202610). */
    public static int currentMonthKey(TimeZone zone, long nowMillis) {
        Calendar calendar = Calendar.getInstance(zone, Locale.ROOT);
        calendar.setTimeInMillis(nowMillis);
        return calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH) + 1;
    }

    public static int monthKey(Parts parts) {
        return parts.year * 100 + parts.month;
    }
}
