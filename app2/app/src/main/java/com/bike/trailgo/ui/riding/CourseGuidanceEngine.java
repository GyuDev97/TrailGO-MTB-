package com.bike.trailgo.ui.riding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight, offline GPX course matcher. It deliberately does not attempt road routing or
 * rerouting; it only reports how the live GPS position relates to the already loaded course.
 */
final class CourseGuidanceEngine {
    static final double OFF_ROUTE_METERS = 60d;
    static final double BACK_ON_ROUTE_METERS = 35d;
    static final float MAX_ACCURACY_METERS = 45f;
    private static final int OFF_ROUTE_CONFIRMATIONS = 2;

    enum Event { NONE, OFF_ROUTE, BACK_ON_ROUTE, LOW_ACCURACY }

    static final class Point {
        final double latitude;
        final double longitude;

        Point(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    static final class Update {
        final boolean routeReady;
        final boolean usableLocation;
        final boolean offRoute;
        final double distanceToRouteMeters;
        final double progressMeters;
        final double totalMeters;
        final Event event;

        Update(boolean routeReady, boolean usableLocation, boolean offRoute,
               double distanceToRouteMeters, double progressMeters, double totalMeters, Event event) {
            this.routeReady = routeReady;
            this.usableLocation = usableLocation;
            this.offRoute = offRoute;
            this.distanceToRouteMeters = distanceToRouteMeters;
            this.progressMeters = progressMeters;
            this.totalMeters = totalMeters;
            this.event = event;
        }

        int progressPercent() {
            return totalMeters <= 0d ? 0 : Math.max(0, Math.min(100,
                    (int) Math.round((progressMeters / totalMeters) * 100d)));
        }

        double remainingMeters() {
            return Math.max(0d, totalMeters - progressMeters);
        }
    }

    private final ArrayList<Point> route = new ArrayList<>();
    private final ArrayList<Double> cumulativeMeters = new ArrayList<>();
    private double totalMeters;
    private double furthestProgressMeters;
    private int offRouteSamples;
    private boolean offRoute;

    void setRoute(List<Point> points) {
        route.clear();
        cumulativeMeters.clear();
        totalMeters = 0d;
        furthestProgressMeters = 0d;
        offRouteSamples = 0;
        offRoute = false;
        if (points == null) return;

        for (Point point : points) {
            if (point == null || !isCoordinate(point.latitude, point.longitude)) continue;
            if (!route.isEmpty() && distanceMeters(route.get(route.size() - 1), point) < 1d) continue;
            route.add(point);
            if (route.size() == 1) {
                cumulativeMeters.add(0d);
            } else {
                totalMeters += distanceMeters(route.get(route.size() - 2), point);
                cumulativeMeters.add(totalMeters);
            }
        }
    }

    boolean hasRoute() {
        return route.size() >= 2 && totalMeters > 1d;
    }

    Update update(double latitude, double longitude, boolean hasAccuracy, float accuracyMeters) {
        if (!hasRoute()) {
            return new Update(false, false, false, 0d, 0d, 0d, Event.NONE);
        }
        if (!isCoordinate(latitude, longitude) || (hasAccuracy && accuracyMeters > MAX_ACCURACY_METERS)) {
            return new Update(true, false, offRoute, 0d, furthestProgressMeters, totalMeters, Event.LOW_ACCURACY);
        }

        Match nearest = findNearest(latitude, longitude);
        if (nearest.progressMeters > furthestProgressMeters) {
            furthestProgressMeters = Math.min(totalMeters, nearest.progressMeters);
        }

        Event event = Event.NONE;
        if (nearest.distanceMeters > OFF_ROUTE_METERS) {
            offRouteSamples++;
            if (!offRoute && offRouteSamples >= OFF_ROUTE_CONFIRMATIONS) {
                offRoute = true;
                event = Event.OFF_ROUTE;
            }
        } else if (nearest.distanceMeters <= BACK_ON_ROUTE_METERS) {
            offRouteSamples = 0;
            if (offRoute) {
                offRoute = false;
                event = Event.BACK_ON_ROUTE;
            }
        } else {
            // The hysteresis band prevents noisy GPS fixes from flipping state repeatedly.
            offRouteSamples = 0;
        }
        return new Update(true, true, offRoute, nearest.distanceMeters,
                furthestProgressMeters, totalMeters, event);
    }

    private Match findNearest(double latitude, double longitude) {
        Point position = new Point(latitude, longitude);
        double bestDistance = Double.MAX_VALUE;
        double bestProgress = 0d;
        for (int index = 0; index < route.size() - 1; index++) {
            Point start = route.get(index);
            Point end = route.get(index + 1);
            Projection projection = project(position, start, end);
            if (projection.distanceMeters < bestDistance) {
                bestDistance = projection.distanceMeters;
                bestProgress = cumulativeMeters.get(index)
                        + distanceMeters(start, end) * projection.fraction;
            }
        }
        return new Match(bestDistance, bestProgress);
    }

    private static Projection project(Point position, Point start, Point end) {
        double latitudeScale = 111_320d;
        double longitudeScale = latitudeScale * Math.cos(Math.toRadians(position.latitude));
        double startX = (start.longitude - position.longitude) * longitudeScale;
        double startY = (start.latitude - position.latitude) * latitudeScale;
        double endX = (end.longitude - position.longitude) * longitudeScale;
        double endY = (end.latitude - position.latitude) * latitudeScale;
        double dx = endX - startX;
        double dy = endY - startY;
        double lengthSquared = dx * dx + dy * dy;
        double fraction = lengthSquared <= 0d ? 0d : -(startX * dx + startY * dy) / lengthSquared;
        fraction = Math.max(0d, Math.min(1d, fraction));
        double nearestX = startX + fraction * dx;
        double nearestY = startY + fraction * dy;
        return new Projection(Math.hypot(nearestX, nearestY), fraction);
    }

    private static boolean isCoordinate(double latitude, double longitude) {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude)
                && latitude >= -90d && latitude <= 90d && longitude >= -180d && longitude <= 180d;
    }

    private static double distanceMeters(Point left, Point right) {
        double dLat = Math.toRadians(right.latitude - left.latitude);
        double dLon = Math.toRadians(right.longitude - left.longitude);
        double a = Math.sin(dLat / 2d) * Math.sin(dLat / 2d)
                + Math.cos(Math.toRadians(left.latitude)) * Math.cos(Math.toRadians(right.latitude))
                * Math.sin(dLon / 2d) * Math.sin(dLon / 2d);
        return 6_371_000d * 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
    }

    private static final class Match {
        final double distanceMeters;
        final double progressMeters;

        Match(double distanceMeters, double progressMeters) {
            this.distanceMeters = distanceMeters;
            this.progressMeters = progressMeters;
        }
    }

    private static final class Projection {
        final double distanceMeters;
        final double fraction;

        Projection(double distanceMeters, double fraction) {
            this.distanceMeters = distanceMeters;
            this.fraction = fraction;
        }
    }
}
