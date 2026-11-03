package com.bike.trailgo.ui.riding;

import com.bike.trailgo.gpx.Gpx;
import com.bike.trailgo.gpx.Metadata;
import com.bike.trailgo.gpx.Point;
import com.bike.trailgo.gpx.Route;
import com.bike.trailgo.gpx.RoutePoint;
import com.bike.trailgo.gpx.Track;
import com.bike.trailgo.gpx.TrackPoint;
import com.bike.trailgo.gpx.TrackSegment;

import java.util.ArrayList;
import java.util.List;

/** Extracts the one route representation TrailGo needs from standard GPX tracks or routes. */
public final class CourseGpxAnalyzer {
    private CourseGpxAnalyzer() {
    }

    public static final class Summary {
        private final String courseName;
        private final int pointCount;
        private final double totalDistanceMeters;
        private final Double ascentMeters;
        final ArrayList<CourseGuidanceEngine.Point> guidancePoints;

        Summary(String courseName, int pointCount, double totalDistanceMeters, Double ascentMeters,
                ArrayList<CourseGuidanceEngine.Point> guidancePoints) {
            this.courseName = courseName;
            this.pointCount = pointCount;
            this.totalDistanceMeters = totalDistanceMeters;
            this.ascentMeters = ascentMeters;
            this.guidancePoints = guidancePoints;
        }

        public String getCourseName() { return courseName; }
        public int getPointCount() { return pointCount; }
        public double getTotalDistanceMeters() { return totalDistanceMeters; }
        public Double getAscentMeters() { return ascentMeters; }
    }

    public static Summary analyze(Gpx gpx, String fallbackName) {
        ArrayList<Point> rawPoints = new ArrayList<>();
        String courseName = firstMeaningfulName(gpx == null ? null : gpx.getMetadata(), null);
        if (gpx != null) {
            for (Track track : gpx.getTracks()) {
                if (isBlank(courseName) && !isBlank(track.getTrackName())) courseName = track.getTrackName().trim();
                for (TrackSegment segment : track.getTrackSegments()) rawPoints.addAll(segment.getTrackPoints());
            }
            // A GPX may contain both a display route and a dense recorded track. Prefer the track
            // when it is usable; rte/rtept is a fully supported fallback for route-only files.
            if (rawPoints.size() < 2) {
                for (Route route : gpx.getRoutes()) {
                    if (isBlank(courseName) && !isBlank(route.getRouteName())) courseName = route.getRouteName().trim();
                    List<RoutePoint> routePoints = route.getRoutePoints();
                    rawPoints.addAll(routePoints);
                }
            }
        }

        ArrayList<CourseGuidanceEngine.Point> guidancePoints = new ArrayList<>();
        Double ascent = null;
        Double previousElevation = null;
        for (Point point : rawPoints) {
            if (point == null || point.getLatitude() == null || point.getLongitude() == null) continue;
            double latitude = point.getLatitude();
            double longitude = point.getLongitude();
            if (latitude < -90d || latitude > 90d || longitude < -180d || longitude > 180d) continue;
            if (!guidancePoints.isEmpty()) {
                CourseGuidanceEngine.Point previous = guidancePoints.get(guidancePoints.size() - 1);
                if (distanceMeters(previous.latitude, previous.longitude, latitude, longitude) < 1d) continue;
            }
            guidancePoints.add(new CourseGuidanceEngine.Point(latitude, longitude));
            Double elevation = point.getElevation();
            if (elevation != null) {
                if (previousElevation != null && elevation - previousElevation >= 2d) {
                    ascent = (ascent == null ? 0d : ascent) + (elevation - previousElevation);
                }
                previousElevation = elevation;
            }
        }

        double totalMeters = 0d;
        for (int index = 1; index < guidancePoints.size(); index++) {
            CourseGuidanceEngine.Point previous = guidancePoints.get(index - 1);
            CourseGuidanceEngine.Point current = guidancePoints.get(index);
            totalMeters += distanceMeters(previous.latitude, previous.longitude, current.latitude, current.longitude);
        }
        return new Summary(isBlank(courseName) ? (isBlank(fallbackName) ? "불러온 GPX 코스" : fallbackName.trim()) : courseName,
                guidancePoints.size(), totalMeters, ascent, guidancePoints);
    }

    private static String firstMeaningfulName(Metadata metadata, String fallbackName) {
        if (metadata != null && !isBlank(metadata.getName())) return metadata.getName().trim();
        return isBlank(fallbackName) ? null : fallbackName.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2d) * Math.sin(dLat / 2d)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2d) * Math.sin(dLon / 2d);
        return 6_371_000d * 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
    }
}
