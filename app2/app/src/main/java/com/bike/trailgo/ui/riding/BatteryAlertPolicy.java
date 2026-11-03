package com.bike.trailgo.ui.riding;

import androidx.annotation.Nullable;

/**
 * Decides when the rider is told that the phone battery is running low. One warning at 20 % and a
 * last one at 10 %, each at most once per ride; charging cancels them. Pure, so it is easy to test.
 */
final class BatteryAlertPolicy {
    static final int LOW_PERCENT = 20;
    static final int CRITICAL_PERCENT = 10;
    /** Above this the warnings arm again (the battery was charged during the ride). */
    static final int REARM_PERCENT = 30;

    private boolean lowWarned;
    private boolean criticalWarned;

    /**
     * @param percent  battery level 0..100, or a negative number when unknown
     * @param charging true while a charger is connected
     * @return the message to announce now, or null
     */
    @Nullable
    String evaluate(int percent, boolean charging) {
        if (percent < 0 || percent > 100) {
            return null;
        }
        if (charging || percent >= REARM_PERCENT) {
            if (percent >= REARM_PERCENT) {
                lowWarned = false;
                criticalWarned = false;
            }
            return null;
        }
        if (percent <= CRITICAL_PERCENT && !criticalWarned) {
            criticalWarned = true;
            lowWarned = true;
            return "배터리가 " + percent + "%입니다. 곧 꺼질 수 있어요. 주행을 마치고 기록을 저장해 주세요.";
        }
        if (percent <= LOW_PERCENT && percent > CRITICAL_PERCENT && !lowWarned) {
            lowWarned = true;
            return "배터리가 " + percent + "%입니다. 충전하거나 배터리 절약 모드를 켜 주세요.";
        }
        return null;
    }
}
