package com.bike.trailgo.hazard;

/** Body of POST /app/hazards. The reporter is the signed-in rider; it is never part of the body. */
public class HazardReport {
    public static final int MIN_RADIUS_M = 10;
    public static final int MAX_RADIUS_M = 200;
    public static final int DEFAULT_RADIUS_M = 30;
    public static final int MAX_TITLE_LENGTH = 50;
    public static final int MAX_DESCRIPTION_LENGTH = 300;

    public double latitude;
    public double longitude;
    public int radiusM = DEFAULT_RADIUS_M;
    public String type;
    public String title;
    public String description;

    /**
     * Checks what the server checks, so the rider hears about a mistake without a round trip.
     *
     * @return a message for the rider, or null when the report is acceptable
     */
    public static String validate(Double latitude, Double longitude, HazardType type, String title,
                                  String description, int radiusM) {
        if (latitude == null || longitude == null || latitude.isNaN() || longitude.isNaN()
                || Math.abs(latitude) > 90.0 || Math.abs(longitude) > 180.0
                || (latitude == 0.0 && longitude == 0.0)) {
            return "지도에서 위험지역 위치를 선택해 주세요.";
        }
        if (type == null) return "위험 유형을 선택해 주세요.";
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) return "제목을 입력해 주세요.";
        if (trimmed.length() > MAX_TITLE_LENGTH) return "제목은 " + MAX_TITLE_LENGTH + "자 이하로 입력해 주세요.";
        if (description != null && description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            return "설명은 " + MAX_DESCRIPTION_LENGTH + "자 이하로 입력해 주세요.";
        }
        if (radiusM < MIN_RADIUS_M || radiusM > MAX_RADIUS_M) {
            return "위험 반경은 " + MIN_RADIUS_M + "~" + MAX_RADIUS_M + "m 로 설정해 주세요.";
        }
        return null;
    }
}
