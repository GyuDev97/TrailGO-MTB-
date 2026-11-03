package com.bike.trailgo.rideassistant;

public enum VoicePriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL;

    public static VoicePriority fromServerValue(String value) {
        if (value == null) return NORMAL;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return NORMAL;
        }
    }
}
