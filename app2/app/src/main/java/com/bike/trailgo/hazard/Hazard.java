package com.bike.trailgo.hazard;

/** A circular hazard zone as the server sends it (GET /app/hazards/nearby). Field names are the JSON keys. */
public class Hazard {
    public int id;
    public double latitude;
    public double longitude;
    public int radiusM;
    public String type;
    public String typeLabel;
    public String title;
    public String description;
    public String address;
    /** True only for reports an operator approved; every other zone is an unverified rider report. */
    public boolean verified;
    public long createdAtMs;
    public long updatedAtMs;
    public boolean mine;
    /** Not persisted meaning: distance from the query point when it was fetched. */
    public Double distanceM;
    /** Local bookkeeping for the cache; the server does not send it. */
    public long cachedAtMs;

    public Hazard() {
    }

    public Hazard(int id, double latitude, double longitude, int radiusM, HazardType type, String title) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusM = radiusM;
        this.type = type.code();
        this.typeLabel = type.label;
        this.title = title;
    }

    public HazardType hazardType() {
        return HazardType.fromCode(type);
    }

    /** A zone without a usable radius still warns, with the default radius. */
    public int effectiveRadiusM() {
        return radiusM > 0 ? radiusM : 30;
    }

    public boolean hasValidPosition() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude)
                && Math.abs(latitude) <= 90.0 && Math.abs(longitude) <= 180.0
                && !(latitude == 0.0 && longitude == 0.0);
    }
}
