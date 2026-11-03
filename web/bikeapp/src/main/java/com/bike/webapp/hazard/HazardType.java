package com.bike.webapp.hazard;

/** The kinds of hazard a rider can report. The code is what is stored and sent; the label is for display. */
public enum HazardType {
	STEEP_SLOPE("급경사"),
	BAD_ROAD("노면 불량"),
	TRAFFIC("차량 주의"),
	SLIPPERY("미끄럼 위험"),
	SHARP_CURVE("급커브"),
	OTHER("기타 위험");

	private final String label;

	HazardType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	/** @return the type for a code, or null when the code is missing or unknown */
	public static HazardType parse(String code) {
		if (code == null) {
			return null;
		}
		for (HazardType type : values()) {
			if (type.name().equals(code.trim())) {
				return type;
			}
		}
		return null;
	}

	/** Stored codes are trusted less than input: anything unknown (older rows) is shown as OTHER. */
	public static HazardType parseOrOther(String code) {
		HazardType type = parse(code);
		return type == null ? OTHER : type;
	}
}
