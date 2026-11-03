package com.bike.trailgo.ui.home;

import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.ui.records.RecordFormatter;

import java.util.List;
import java.util.TimeZone;

/**
 * Totals of the rides saved in one calendar month.
 *
 * <p>Records are never deleted or corrected here. A record whose riding time cannot belong to its
 * distance (see {@link RecordFormatter#isSuspiciousDuration}) stays in the sums exactly as stored;
 * it is only counted separately so the screen can say that the totals contain such a record. A
 * record with an unreadable date is not part of any month.</p>
 */
public final class MonthlySummary {
    public final int monthKey;
    public final int rideCount;
    public final long distanceMeters;
    public final long elevationGainMeters;
    public final long ridingSeconds;
    /** Rides in this month whose stored time looks wrong (still included in the sums above). */
    public final int suspiciousCount;
    /** Records that could not be placed in any month because their date is unreadable. */
    public final int undatedCount;

    private MonthlySummary(int monthKey, int rideCount, long distanceMeters, long elevationGainMeters,
                           long ridingSeconds, int suspiciousCount, int undatedCount) {
        this.monthKey = monthKey;
        this.rideCount = rideCount;
        this.distanceMeters = distanceMeters;
        this.elevationGainMeters = elevationGainMeters;
        this.ridingSeconds = ridingSeconds;
        this.suspiciousCount = suspiciousCount;
        this.undatedCount = undatedCount;
    }

    public static MonthlySummary of(List<RidingRecord> records, int monthKey, TimeZone zoneForOffsetDates) {
        int count = 0;
        int suspicious = 0;
        int undated = 0;
        long distance = 0;
        long gain = 0;
        long seconds = 0;
        if (records != null) {
            for (RidingRecord record : records) {
                if (record == null) {
                    continue;
                }
                RecordDates.Parts date = RecordDates.parse(record.getRr_date(), zoneForOffsetDates);
                if (date == null) {
                    undated++;
                    continue;
                }
                if (RecordDates.monthKey(date) != monthKey) {
                    continue;
                }
                int meters = Math.max(0, record.getRr_distance());
                int time = Math.max(0, record.getRr_time());
                count++;
                distance += meters;
                gain += Math.max(0, record.getRr_high());
                seconds += time;
                if (RecordFormatter.isSuspiciousDuration(meters, time)) {
                    suspicious++;
                }
            }
        }
        return new MonthlySummary(monthKey, count, distance, gain, seconds, suspicious, undated);
    }

    public int year() {
        return monthKey / 100;
    }

    public int month() {
        return monthKey % 100;
    }
}
