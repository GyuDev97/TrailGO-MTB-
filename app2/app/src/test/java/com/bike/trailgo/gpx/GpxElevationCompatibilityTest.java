package com.bike.trailgo.gpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bike.trailgo.ui.records.ElevationProfile;

import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** GPX written by the app: unknown altitude has no <ele>; GPX written by older versions still reads. */
public class GpxElevationCompatibilityTest {

    private static String write(double[] elevations) throws Exception {
        GpxInfo gpx = new GpxInfo();
        gpx.setCreator("TrailGO");
        GpxInfo.metadata metadata = new GpxInfo.metadata();
        metadata.setDesc("TrailGO");
        GpxInfo.bounds bounds = new GpxInfo.bounds();
        bounds.setMaxLat(36);
        bounds.setMaxLon(129);
        bounds.setMinLat(35);
        bounds.setMinLon(128);
        metadata.setBounds(bounds);
        gpx.setMetadata(metadata);
        List<GpxInfo.trkpt> points = new ArrayList<>();
        for (int i = 0; i < elevations.length; i++) {
            points.add(new GpxInfo.trkpt(35.8329 + i * 0.0005, 128.565, elevations[i]));
        }
        GpxInfo.trkseg segment = new GpxInfo.trkseg();
        segment.setTrkptList(points);
        GpxInfo.trk track = new GpxInfo.trk();
        track.setTrkseg(segment);
        gpx.setTrk(track);
        StringWriter out = new StringWriter();
        Serializer serializer = new Persister();
        serializer.write(gpx, out);
        return out.toString();
    }

    private static ElevationProfile profileOf(String gpxXml) throws Exception {
        Gpx parsed = new GPXParser().parse(
                new ByteArrayInputStream(gpxXml.getBytes(StandardCharsets.UTF_8)), new KXmlParser());
        assertNotNull(parsed);
        ElevationProfile.Builder builder = new ElevationProfile.Builder();
        for (Track track : parsed.getTracks()) {
            for (TrackSegment segment : track.getTrackSegments()) {
                for (TrackPoint point : segment.getTrackPoints()) {
                    builder.add(point.getLatitude(), point.getLongitude(), point.getElevation());
                }
            }
        }
        return builder.build();
    }

    @Test
    public void unknownAltitudeIsWrittenWithoutAnEleElement() throws Exception {
        String xml = write(new double[]{120.5, Double.NaN, 121.0});
        int eleCount = xml.split("<ele>", -1).length - 1;
        assertEquals(2, eleCount);
        assertTrue(xml.contains("<ele>120.5</ele>"));
    }

    @Test
    public void pointsWithoutEleParseAsNoElevationNotAsZero() throws Exception {
        String xml = write(new double[]{120.5, Double.NaN, 121.0, Double.NaN, 122.0, 123.0, 124.0, 125.0});
        Gpx parsed = new GPXParser().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), new KXmlParser());
        List<TrackPoint> points = parsed.getTracks().get(0).getTrackSegments().get(0).getTrackPoints();
        assertEquals(8, points.size());
        assertNull(points.get(1).getElevation());
        assertEquals(Double.valueOf(120.5), points.get(0).getElevation());
    }

    @Test
    public void saveScreenAndDetailAgreeBecauseTheyReadTheSameGpxPoints() throws Exception {
        double[] elevations = {100, 101, 102.5, 104, 103, 105, 107, 108, 110, 111};
        ElevationProfile.Builder fromLists = new ElevationProfile.Builder();
        for (int i = 0; i < elevations.length; i++) {
            fromLists.add(35.8329 + i * 0.0005, 128.565, elevations[i]);
        }
        ElevationProfile saveScreen = fromLists.build();
        ElevationProfile detail = profileOf(write(elevations));

        assertEquals(saveScreen.getAvailability(), detail.getAvailability());
        assertEquals(saveScreen.getMinElevation(), detail.getMinElevation(), 1e-9);
        assertEquals(saveScreen.getMaxElevation(), detail.getMaxElevation(), 1e-9);
        assertEquals(saveScreen.getSamples().size(), detail.getSamples().size());
    }

    @Test
    public void theReportedOneMetreRideGivesTheSameMessageOnBothScreens() throws Exception {
        double[] elevations = {100, 112, 118, 100, 119, 101, 117, 102};
        ElevationProfile.Builder fromLists = new ElevationProfile.Builder();
        for (int i = 0; i < elevations.length; i++) {
            fromLists.add(35.8329 + i * 0.000001, 128.565, elevations[i]); // ~0.1 m apart
        }
        ElevationProfile saveScreen = fromLists.build();
        ElevationProfile detail = profileOf(write(elevations).replaceAll("lat=\"35\\.83(\\d+)\"", "lat=\"35.8329\""));

        assertFalse(saveScreen.isAvailable());
        assertEquals(ElevationProfile.Availability.TOO_SHORT, saveScreen.getAvailability());
        assertEquals(saveScreen.getAvailability(), detail.getAvailability());
        assertEquals(saveScreen.getAvailability().message, detail.getAvailability().message);
    }

    @Test
    public void gpxFromOlderVersionsWithZeroElevationStillReads() throws Exception {
        // older builds wrote <ele>0.0</ele> when the phone had no altitude
        StringBuilder xml = new StringBuilder("<gpx version=\"1.1\" creator=\"TrailGO\"><trk><trkseg>");
        for (int i = 0; i < 6; i++) {
            xml.append("<trkpt lat=\"").append(35.8329 + i * 0.0005).append("\" lon=\"128.565\"><ele>0.0</ele></trkpt>");
        }
        xml.append("</trkseg></trk></gpx>");
        ElevationProfile profile = profileOf(xml.toString());
        assertEquals(ElevationProfile.Availability.NO_ELEVATION, profile.getAvailability());

        // ... and a mixed legacy file keeps its real values
        StringBuilder mixed = new StringBuilder("<gpx version=\"1.1\" creator=\"TrailGO\"><trk><trkseg>");
        double[] values = {0.0, 101.0, 102.0, 0.0, 104.0, 105.0, 107.0};
        for (int i = 0; i < values.length; i++) {
            mixed.append("<trkpt lat=\"").append(35.8329 + i * 0.0005).append("\" lon=\"128.565\"><ele>")
                    .append(values[i]).append("</ele></trkpt>");
        }
        mixed.append("</trkseg></trk></gpx>");
        ElevationProfile legacy = profileOf(mixed.toString());
        assertTrue(legacy.isAvailable());
        assertEquals(101.0, legacy.getMinElevation(), 0.0);
        assertEquals(107.0, legacy.getMaxElevation(), 0.0);
    }
}
