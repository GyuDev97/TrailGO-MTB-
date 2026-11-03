package com.bike.trailgo.hazard;

/** The six hazard kinds a rider can report. {@link #code} is what the server stores. */
public enum HazardType {
    STEEP_SLOPE("급경사", "급경사", "⛰"),
    BAD_ROAD("노면 불량", "노면 불량", "🕳"),
    TRAFFIC("차량 주의", "차량 주의", "🚗"),
    SLIPPERY("미끄럼 위험", "미끄럼 위험", "💧"),
    SHARP_CURVE("급커브", "급커브", "↪"),
    OTHER("기타 위험", "기타", "⚠");

    public final String label;
    /** Short form used inside spoken sentences. */
    public final String spoken;
    public final String glyph;

    HazardType(String label, String spoken, String glyph) {
        this.label = label;
        this.spoken = spoken;
        this.glyph = glyph;
    }

    public String code() {
        return name();
    }

    /** Unknown or missing codes (older servers, legacy rows) are shown as {@link #OTHER}. */
    public static HazardType fromCode(String code) {
        if (code != null) {
            for (HazardType type : values()) {
                if (type.name().equals(code.trim())) return type;
            }
        }
        return OTHER;
    }
}
