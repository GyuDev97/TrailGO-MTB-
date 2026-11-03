package com.bike.trailgo.gpx;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Version;

import java.util.List;

@Root
@Namespace(reference="http://www.topografix.com/GPX/1/1")
public class GpxInfo
{

    @Version(revision=1.1)
    private double version;

    @Attribute
    private String creator;

    @Element
    public metadata metadata;

    @Element
    public trk trk;

    @Root
    public static class metadata{
        @Element
        public String desc;

        @Element
        public bounds bounds;

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public GpxInfo.bounds getBounds() {
            return bounds;
        }

        public void setBounds(GpxInfo.bounds bounds) {
            this.bounds = bounds;
        }
    }

    @Root
    public static class bounds {
        @Attribute
        private double maxLat;

        @Attribute
        private double minLat;

        @Attribute
        private double maxLon;

        @Attribute
        private double minLon;

        public double getMaxLat() {
            return maxLat;
        }

        public void setMaxLat(double maxLat) {
            this.maxLat = maxLat;
        }

        public double getMinLat() {
            return minLat;
        }

        public void setMinLat(double minLat) {
            this.minLat = minLat;
        }

        public double getMaxLon() {
            return maxLon;
        }

        public void setMaxLon(double maxLon) {
            this.maxLon = maxLon;
        }

        public double getMinLon() {
            return minLon;
        }

        public void setMinLon(double minLon) {
            this.minLon = minLon;
        }
    }

    @Root
    public static class trk {
        @Element
        public trkseg trkseg;

        public GpxInfo.trkseg getTrkseg() {
            return trkseg;
        }

        public void setTrkseg(GpxInfo.trkseg trkseg) {
            this.trkseg = trkseg;
        }
    }

    @Root
    public static class trkseg {

        @ElementList ( inline = true )
        public List<trkpt> trkptList;

        public List<trkpt> getTrkptList() {
            return trkptList;
        }

        public void setTrkptList(List<trkpt> trkptList) {
            this.trkptList = trkptList;
        }
    }

    @Root
    public static class trkpt {

        @Attribute
        private double lat;

        @Attribute
        private double lon;

        // Optional: omitted when the phone gave no (reliable) altitude, so "unknown" is never
        // written as 0 m. Older files that always have <ele> are still read as before.
        @Element(required = false)
        private Double ele;

        public trkpt(){

        }

        /** @param ele altitude in metres, or NaN when unknown (then no {@code <ele>} is written) */
        public trkpt ( double lat,  double lon,  double ele ){
            this.lat = lat;
            this.lon = lon;
            this.ele = Double.isNaN(ele) || Double.isInfinite(ele) ? null : ele;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }

        /** @return the altitude in metres, or NaN when the point has none */
        public double getEle() {
            return ele == null ? Double.NaN : ele;
        }

        public void setEle(double ele) {
            this.ele = Double.isNaN(ele) || Double.isInfinite(ele) ? null : ele;
        }
    }

    public GpxInfo() {

    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public GpxInfo.metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(GpxInfo.metadata metadata) {
        this.metadata = metadata;
    }

    public GpxInfo.trk getTrk() {
        return trk;
    }

    public void setTrk(GpxInfo.trk trk) {
        this.trk = trk;
    }
}