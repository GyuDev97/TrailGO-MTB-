package com.bike.trailgo.ui.home;

import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.ui.records.RecordFormatter;

import java.util.Locale;
import java.util.TimeZone;

/** Display text of the home screen: big value and small unit apart, plus the recent-ride row texts. */
public final class HomeFormat {
    /** A number and its unit, shown in two sizes. {@code unit} may be empty. */
    public static final class Value {
        public final String number;
        public final String unit;

        Value(String number, String unit) {
            this.number = number;
            this.unit = unit;
        }

        public String joined() {
            return unit.isEmpty() ? number : number + " " + unit;
        }
    }

    private HomeFormat() {
    }

    public static Value count(int rides) {
        return new Value(String.valueOf(Math.max(0, rides)), "회");
    }

    /** "49 m" below a kilometre, "12.3 km" from there on. */
    public static Value distance(long meters) {
        long safe = Math.max(0, meters);
        if (safe < 1000) {
            return new Value(String.valueOf(safe), "m");
        }
        double km = safe / 1000.0;
        return new Value(km >= 100 ? String.format(Locale.KOREA, "%,.0f", km)
                : String.format(Locale.KOREA, "%.1f", km), "km");
    }

    public static Value elevation(long meters) {
        return new Value(String.format(Locale.KOREA, "%,d", Math.max(0, meters)), "m");
    }

    /** "68:20 시간" from one hour on, otherwise whole minutes; a few seconds are "<1 분", never "0". */
    public static Value duration(long seconds) {
        long safe = Math.max(0, seconds);
        if (safe >= 3600) {
            return new Value(String.format(Locale.KOREA, "%d:%02d", safe / 3600, (safe % 3600) / 60), "시간");
        }
        if (safe >= 60) {
            return new Value(String.valueOf(safe / 60), "분");
        }
        return safe > 0 ? new Value("<1", "분") : new Value("0", "분");
    }

    /** Title shown for a ride: its name, else its area, else a neutral label. */
    public static String title(RidingRecord record) {
        String name = record.getRr_name();
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        String area = record.getRr_area();
        if (area != null && !area.trim().isEmpty()) {
            return area.trim() + " 라이딩";
        }
        return "라이딩 기록";
    }

    /** "2026년 10월 13일 · 오후 10:59"; just the date when the record has no time; "-" when unreadable. */
    public static String dateAndTime(String raw, TimeZone zone) {
        RecordDates.Parts parts = RecordDates.parse(raw, zone);
        if (parts == null) {
            return "-";
        }
        String date = parts.year + "년 " + parts.month + "월 " + parts.day + "일";
        if (!parts.hasTime()) {
            return date;
        }
        int hour12 = parts.hour % 12 == 0 ? 12 : parts.hour % 12;
        return date + " · " + (parts.hour < 12 ? "오전 " : "오후 ") + hour12 + ":" + String.format(Locale.KOREA, "%02d", parts.minute);
    }

    public static String monthLabel(int monthKey) {
        return (monthKey / 100) + "년 " + (monthKey % 100) + "월";
    }

    /** Distance of a ride row; "-" when nothing was measured. */
    public static String rowDistance(RidingRecord record) {
        return RecordFormatter.formatDistance(record.getRr_distance());
    }

    public static String rowElevation(RidingRecord record) {
        return RecordFormatter.formatElevationGain(record.getRr_high());
    }

    public static String rowDuration(RidingRecord record) {
        return RecordFormatter.formatDuration(record.getRr_time());
    }
}
