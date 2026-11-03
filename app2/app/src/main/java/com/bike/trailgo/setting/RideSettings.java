package com.bike.trailgo.setting;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Window;
import android.view.WindowManager;

/**
 * Single owner for settings that change real riding behavior.
 * Values are read by the foreground service and riding screens, not copied into each Activity.
 */
public final class RideSettings {
    public static final String PREFS = "trailgo_settings";

    public static final String KEY_LOCATION_INTERVAL = "location_interval";
    public static final String KEY_BATTERY_SAVER = "battery_saver";
    public static final String KEY_VOICE_GUIDE = "voice_guide";
    public static final String KEY_DANGER_ALERTS = "notify_danger";
    /** Show hazard zones (markers and radius circles) on the maps. */
    public static final String KEY_HAZARD_DISPLAY = "hazard_display";
    /** Speak hazard warnings. Needs the general voice guidance ({@link #KEY_VOICE_GUIDE}) to be on as well. */
    public static final String KEY_HAZARD_VOICE = "hazard_voice";
    /** Distance from a zone's edge at which the pre-warning comes, stored as "100m". */
    public static final String KEY_HAZARD_WARN_DISTANCE = "hazard_warn_distance";
    public static final String[] HAZARD_WARN_DISTANCE_OPTIONS = {"50m", "100m", "150m", "200m"};
    public static final String KEY_KEEP_SCREEN = "keep_screen";
    public static final String KEY_KEEP_BRIGHTNESS = "keep_brightness";
    public static final String KEY_RECORD_VISIBILITY = "record_visibility";
    public static final String KEY_START_SCREEN = "start_screen";
    public static final String KEY_COURSE_ALERTS = "notify_course";
    public static final String KEY_BATTERY_ALERTS = "notify_battery";
    public static final String KEY_VIBRATION = "alert_vibration";

    public static final String START_HOME = "홈";
    public static final String START_RECORDS = "기록";
    public static final String START_COURSES = "코스";

    private RideSettings() {
    }

    public static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Migrates the old unsupported follower-only default to the legacy API's public value. */
    public static void migrate(Context context) {
        SharedPreferences preferences = preferences(context);
        if ("친구 공개".equals(preferences.getString(KEY_RECORD_VISIBILITY, "전체 공개"))) {
            preferences.edit().putString(KEY_RECORD_VISIBILITY, "전체 공개").apply();
        }
    }

    /** The screen the app opens on when it is started (not when it returns from the background). */
    public static String startScreen(Context context) {
        String value = preferences(context).getString(KEY_START_SCREEN, START_HOME);
        return START_RECORDS.equals(value) || START_COURSES.equals(value) ? value : START_HOME;
    }

    public static long locationIntervalMillis(Context context) {
        SharedPreferences preferences = preferences(context);
        if (preferences.getBoolean(KEY_BATTERY_SAVER, false)) {
            return 5_000L;
        }
        String value = preferences.getString(KEY_LOCATION_INTERVAL, "1초");
        if ("5초".equals(value)) return 5_000L;
        if ("3초".equals(value)) return 3_000L;
        return 1_000L;
    }

    public static boolean isVoiceGuidanceEnabled(Context context) {
        return preferences(context).getBoolean(KEY_VOICE_GUIDE, true);
    }

    public static boolean areDangerAlertsEnabled(Context context) {
        return preferences(context).getBoolean(KEY_DANGER_ALERTS, true);
    }

    public static boolean areHazardsShownOnMap(Context context) {
        return preferences(context).getBoolean(KEY_HAZARD_DISPLAY, true);
    }

    public static boolean isHazardVoiceEnabled(Context context) {
        return preferences(context).getBoolean(KEY_HAZARD_VOICE, true);
    }

    /** The pre-warning distance in metres; a missing or corrupt value is the 100 m default. */
    public static int hazardWarnDistanceMeters(Context context) {
        String value = preferences(context).getString(KEY_HAZARD_WARN_DISTANCE, "100m");
        try {
            return com.bike.trailgo.hazard.HazardAlertConfig.sanitizeWarnDistance(
                    Integer.parseInt(value.replace("m", "").trim()));
        } catch (RuntimeException e) {
            return com.bike.trailgo.hazard.HazardAlertConfig.DEFAULT_WARN_DISTANCE_M;
        }
    }

    public static boolean areCourseAlertsEnabled(Context context) {
        return preferences(context).getBoolean(KEY_COURSE_ALERTS, true);
    }

    public static boolean areBatteryAlertsEnabled(Context context) {
        return preferences(context).getBoolean(KEY_BATTERY_ALERTS, true);
    }

    public static boolean isVibrationEnabled(Context context) {
        return preferences(context).getBoolean(KEY_VIBRATION, true);
    }

    public static boolean isRecordPublic(Context context) {
        return !"비공개".equals(preferences(context).getString(KEY_RECORD_VISIBILITY, "전체 공개"));
    }

    /** Applies display options only while a riding Activity is visible. */
    public static void applyRidingDisplay(Activity activity) {
        Window window = activity.getWindow();
        if (preferences(activity).getBoolean(KEY_KEEP_SCREEN, true)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.screenBrightness = preferences(activity).getBoolean(KEY_KEEP_BRIGHTNESS, true)
                ? WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        window.setAttributes(attributes);
    }
}
