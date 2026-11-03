package com.bike.trailgo.gpx;

import org.junit.Test;
import org.kxml2.io.KXmlParser;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GPXParserTest {
    @Test
    public void parseCoordinateAcceptsOnlyFiniteNumericValues() {
        assertEquals(Double.valueOf(37.5d), GPXParser.parseCoordinate("37.5"));
        assertEquals(Double.valueOf(37.5d), GPXParser.parseCoordinate("  37.5  "));

        assertNull(GPXParser.parseCoordinate(null));
        assertNull(GPXParser.parseCoordinate(""));
        assertNull(GPXParser.parseCoordinate("   "));
        assertNull(GPXParser.parseCoordinate("N/A"));
        assertNull(GPXParser.parseCoordinate("37.5,127.0"));
        assertNull(GPXParser.parseCoordinate("--"));
        assertNull(GPXParser.parseCoordinate("NaN"));
        assertNull(GPXParser.parseCoordinate("Infinity"));
        assertNull(GPXParser.parseCoordinate("-Infinity"));
    }

    @Test
    public void parseSkipsOnlyMalformedPointAndConsumesItsChildren() throws Exception {
        String gpx = "<gpx version=\"1.1\" creator=\"test\">"
                + "<trk><trkseg>"
                + "<trkpt lat=\"37.1000\" lon=\"127.1000\"><ele>220.5</ele></trkpt>"
                + "<trkpt lat=\"N/A\" lon=\"127.2000\"><ele>bad</ele><time>not-a-time</time></trkpt>"
                + "<trkpt lat=\"37.3000\" lon=\"127.3000\"><ele></ele><time>also-bad</time></trkpt>"
                + "</trkseg></trk></gpx>";

        Gpx parsed = new GPXParser().parse(
                new ByteArrayInputStream(gpx.getBytes(StandardCharsets.UTF_8)), new KXmlParser());

        assertNotNull(parsed);
        assertEquals(1, parsed.getTracks().size());
        assertEquals(2, parsed.getTracks().get(0).getTrackSegments().get(0).getTrackPoints().size());
        TrackPoint first = parsed.getTracks().get(0).getTrackSegments().get(0).getTrackPoints().get(0);
        TrackPoint second = parsed.getTracks().get(0).getTrackSegments().get(0).getTrackPoints().get(1);
        assertEquals(Double.valueOf(37.1d), first.getLatitude());
        assertEquals(Double.valueOf(127.3d), second.getLongitude());
        assertNull(second.getElevation());
        assertNull(second.getTime());
    }
}
