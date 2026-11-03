package com.bike.trailgo.gpx;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Reads just the positions of a GPX file (for small route pictures). The file is never changed. */
public final class GpxTrackPoints {
    private GpxTrackPoints() {
    }

    /**
     * @param parser an XmlPullParser (Android's {@code Xml.newPullParser()} or kXML in tests)
     * @return {lat, lon} of every valid track point, in file order; empty when there are none
     * @throws IOException / XmlPullParserException when the file cannot be read as GPX
     */
    public static List<double[]> read(InputStream in, XmlPullParser parser) throws IOException, XmlPullParserException {
        Gpx gpx = new GPXParser().parse(in, parser);
        List<double[]> points = new ArrayList<>();
        if (gpx == null) {
            return points;
        }
        for (Track track : gpx.getTracks()) {
            for (TrackSegment segment : track.getTrackSegments()) {
                for (TrackPoint point : segment.getTrackPoints()) {
                    Double lat = point.getLatitude();
                    Double lon = point.getLongitude();
                    if (lat != null && lon != null) {
                        points.add(new double[]{lat, lon});
                    }
                }
            }
        }
        return points;
    }
}
