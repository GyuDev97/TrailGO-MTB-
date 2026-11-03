package com.bike.webapp.hazard;

/** Korean display names for hazard types and review statuses (the admin screens and their JSON). */
public final class HazardLabels {
	private HazardLabels() {
	}

	public static String type(String code) {
		if (code == null) return "기타 위험";
		switch (code) {
		case "ROCKFALL": return "낙석";
		case "STEEP_SLOPE": return "급경사";
		case "ROAD_DAMAGE": return "노면 파손";
		case "VEHICLE_TRAFFIC": return "차량 통행";
		case "CONSTRUCTION": return "공사 중";
		case "BLOCKED": return "길 막힘";
		case "BAD_ROAD": return "노면 불량";
		case "TRAFFIC": return "차량 주의";
		case "SLIPPERY": return "미끄럼 위험";
		case "SHARP_CURVE": return "급커브";
		default: return "기타 위험";
		}
	}

	public static String status(String code, boolean merged) {
		if (merged || "MERGED".equals(code)) return "병합됨";
		if ("APPROVED".equals(code)) return "승인됨";
		if ("REJECTED".equals(code)) return "거절됨";
		if ("REVIEWING".equals(code)) return "검토 중";
		if ("INACTIVE".equals(code)) return "숨김";
		if ("EXPIRED".equals(code)) return "만료";
		return "승인 대기";
	}
}
