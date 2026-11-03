package com.bike.trailgo.hazard;

/** What the on-screen banner shows right now: the most relevant zone around the rider. */
public final class HazardStatus {
    public final Hazard hazard;
    public final boolean inside;
    /** Metres to the zone edge; 0 while inside. */
    public final double edgeDistanceM;
    /** Further zones within warning distance that the banner does not name. */
    public final int otherCount;

    public HazardStatus(Hazard hazard, boolean inside, double edgeDistanceM, int otherCount) {
        this.hazard = hazard;
        this.inside = inside;
        this.edgeDistanceM = edgeDistanceM;
        this.otherCount = otherCount;
    }
}
