package com.bike.trailgo.rideassistant;

import com.bike.trailgo.dto.DangerousArea;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Converts existing user-reported danger points into one 300m and one 100m event per ride. */
public final class DangerAreaMonitor {
    private final RideAssistantPolicy policy;
    private final Set<String> announcedThresholds = new HashSet<>();
    private List<DangerousArea> areas = Collections.emptyList();

    public DangerAreaMonitor(RideAssistantPolicy policy) {
        this.policy = policy;
    }

    public void setAreas(List<DangerousArea> areas) {
        this.areas = areas == null ? Collections.<DangerousArea>emptyList() : areas;
    }

    public RideEvent evaluate(RideContext context) {
        DangerousArea closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (DangerousArea area : areas) {
            try {
                double distance = distanceMeters(context.latitude, context.longitude,
                        Double.parseDouble(area.getDa_latitude()), Double.parseDouble(area.getDa_longitude()));
                if (distance < closestDistance) {
                    closest = area;
                    closestDistance = distance;
                }
            } catch (NumberFormatException ignored) {
                // One invalid server record must not interrupt a ride.
            }
        }
        if (closest == null || closestDistance > policy.dangerApproachMeters) return null;
        double threshold = closestDistance <= policy.dangerNearMeters
                ? policy.dangerNearMeters : policy.dangerApproachMeters;
        String dangerId = String.valueOf(closest.getDa_num());
        String announcementKey = dangerId + ":" + (int) threshold;
        if (!announcedThresholds.add(announcementKey)) return null;
        int roundedDistance = (int) Math.round(closestDistance);
        String detail = closest.getDa_content();
        String message = threshold == policy.dangerNearMeters
                ? "위험 지역이 약 " + roundedDistance + "미터 앞입니다. 주의하세요."
                : "약 " + roundedDistance + "미터 앞에 위험 지역이 있습니다. 주의하세요.";
        if (detail != null && !detail.trim().isEmpty()) message = detail.trim() + ". " + message;
        return new RideEvent(RideEventType.DANGER_APPROACH, context.timestampMs, context,
                dangerId, closestDistance, message);
    }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371000.0d * 2.0d * Math.atan2(Math.sqrt(a), Math.sqrt(1.0d - a));
    }
}
