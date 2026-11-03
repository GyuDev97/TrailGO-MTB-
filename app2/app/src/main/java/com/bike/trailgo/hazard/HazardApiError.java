package com.bike.trailgo.hazard;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** The server's {@code {"code": "...", "message": "..."}} error body, read defensively. */
public final class HazardApiError {
    public final int httpStatus;
    public final String code;
    public final String message;
    /** For DUPLICATE_HAZARD: the zone that is already reported nearby, else 0. */
    public final int existingHazardId;

    private HazardApiError(int httpStatus, String code, String message, int existingHazardId) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
        this.existingHazardId = existingHazardId;
    }

    public static HazardApiError parse(int httpStatus, String body) {
        String code = null;
        String message = null;
        int existing = 0;
        try {
            if (body != null && !body.trim().isEmpty()) {
                JsonObject json = new JsonParser().parse(body).getAsJsonObject();
                if (json.has("code") && !json.get("code").isJsonNull()) code = json.get("code").getAsString();
                if (json.has("message") && !json.get("message").isJsonNull()) message = json.get("message").getAsString();
                if (json.has("hazardId") && !json.get("hazardId").isJsonNull()) existing = json.get("hazardId").getAsInt();
            }
        } catch (RuntimeException ignored) {
            // not our JSON (proxy page, empty body): fall back to the HTTP status below
        }
        return new HazardApiError(httpStatus, code, message, existing);
    }

    /** A sentence for the rider; never raw server text for statuses the app can explain itself. */
    public String userMessage() {
        if (httpStatus == 401) return "로그인이 필요합니다. 다시 로그인한 뒤 시도해 주세요.";
        if (httpStatus == 429) return "제보가 너무 많습니다. 잠시 후 다시 시도해 주세요.";
        if ("DUPLICATE_HAZARD".equals(code)) return "근처에 이미 같은 유형의 제보가 있습니다.";
        if (httpStatus >= 400 && httpStatus < 500 && message != null && !message.isEmpty()) return message;
        return "서버에 문제가 있어 제보하지 못했습니다. 잠시 후 다시 시도해 주세요.";
    }

    public boolean isDuplicate() {
        return "DUPLICATE_HAZARD".equals(code);
    }
}
