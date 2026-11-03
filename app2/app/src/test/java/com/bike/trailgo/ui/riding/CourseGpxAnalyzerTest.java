package com.bike.trailgo.ui.riding;

import com.bike.trailgo.gpx.Gpx;
import com.bike.trailgo.gpx.Route;
import com.bike.trailgo.gpx.RoutePoint;
import com.bike.trailgo.gpx.Track;
import com.bike.trailgo.gpx.TrackPoint;
import com.bike.trailgo.gpx.TrackSegment;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CourseGpxAnalyzerTest {
    @Test
    public void readsTrackPointsAndElevationForPreview() {
        TrackPoint first = trackPoint(37.0, 127.0, 100d);
        TrackPoint second = trackPoint(37.0, 127.01, 112d);
        TrackSegment segment = new TrackSegment.Builder()
                .setTrackPoints(Arrays.asList(first, second)).build();
        Track track = new Track.Builder().setTrackName("산악 코스")
                .setTrackSegments(Collections.singletonList(segment)).build();
        Gpx gpx = new Gpx.Builder().setTracks(Collections.singletonList(track))
                .setRoutes(Collections.emptyList()).setWayPoints(Collections.emptyList()).build();

        CourseGpxAnalyzer.Summary summary = CourseGpxAnalyzer.analyze(gpx, "fallback");

        assertEquals("산악 코스", summary.getCourseName());
        assertEquals(2, summary.getPointCount());
        assertTrue(summary.getTotalDistanceMeters() > 800d);
        assertNotNull(summary.getAscentMeters());
        assertEquals(12d, summary.getAscentMeters(), 0.001d);
    }

    @Test
    public void usesRoutePointsWhenTrackIsAbsentAndElevationIsOptional() {
        Route route = new Route.Builder().setRouteName("루트 코스")
                .setRoutePoints(Arrays.asList(routePoint(37.0, 127.0), routePoint(37.01, 127.0))).build();
        Gpx gpx = new Gpx.Builder().setTracks(Collections.emptyList())
                .setRoutes(Collections.singletonList(route)).setWayPoints(Collections.emptyList()).build();

        CourseGpxAnalyzer.Summary summary = CourseGpxAnalyzer.analyze(gpx, "fallback");

        assertEquals("루트 코스", summary.getCourseName());
        assertEquals(2, summary.getPointCount());
        assertTrue(summary.getTotalDistanceMeters() > 1000d);
        assertNull(summary.getAscentMeters());
    }

    private static TrackPoint trackPoint(double latitude, double longitude, double elevation) {
        TrackPoint.Builder builder = new TrackPoint.Builder();
        builder.setLatitude(latitude);
        builder.setLongitude(longitude);
        builder.setElevation(elevation);
        return builder.build();
    }

    private static RoutePoint routePoint(double latitude, double longitude) {
        RoutePoint.Builder builder = new RoutePoint.Builder();
        builder.setLatitude(latitude);
        builder.setLongitude(longitude);
        return builder.build();
    }
}
