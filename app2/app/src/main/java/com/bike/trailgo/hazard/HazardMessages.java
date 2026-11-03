package com.bike.trailgo.hazard;

import java.util.List;

/** The words for a hazard warning, spoken and written. One sentence per batch, so voices never overlap. */
public final class HazardMessages {
    private HazardMessages() {
    }

    /** Spoken text for the alerts of one location update (already ordered most important first), or null. */
    public static String speech(List<HazardAlert> alerts) {
        if (alerts == null || alerts.isEmpty()) return null;
        HazardAlert primary = alerts.get(0);
        String type = primary.hazard.hazardType().spoken;
        String text;
        if (primary.kind == HazardAlert.Kind.ENTER) {
            text = type + " 위험구역에 진입했습니다. 주의하세요.";
        } else {
            text = "전방 약 " + roundedDistance(primary.edgeDistanceM) + "미터에 " + type + " 위험지역이 있습니다.";
        }
        int others = alerts.size() - 1;
        if (others > 0) text += " 근처에 위험지역이 " + others + "곳 더 있습니다.";
        return text;
    }

    /** First line of the banner: "⚠ 노면 불량 위험지역" / "⚠ 위험구역 진입". */
    public static String bannerTitle(HazardStatus status) {
        HazardType type = status.hazard.hazardType();
        return status.inside
                ? type.glyph + " 위험구역 진입"
                : type.glyph + " " + type.label + " 위험지역";
    }

    /** Second line of the banner. */
    public static String bannerDetail(HazardStatus status) {
        String text;
        if (status.inside) {
            text = status.hazard.hazardType().label + " 구간입니다. 주의하세요.";
        } else {
            text = "전방 약 " + roundedDistance(status.edgeDistanceM) + "m";
        }
        if (status.otherCount > 0) text += " · 외 " + status.otherCount + "곳";
        if (!status.hazard.verified) text += " · 미검증 제보";
        return text;
    }

    /** Rounded to 10 m (never below 10), because a spoken "97 meters" is false precision. */
    public static int roundedDistance(double meters) {
        long rounded = Math.round(meters / 10.0) * 10L;
        return (int) Math.max(10L, rounded);
    }
}
