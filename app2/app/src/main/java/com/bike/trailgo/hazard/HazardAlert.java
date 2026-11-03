package com.bike.trailgo.hazard;

/** One announcement the rider should get: "approaching this zone" or "you are inside this zone". */
public final class HazardAlert {
    public enum Kind {
        PRE_WARN,
        ENTER
    }

    public final Kind kind;
    public final Hazard hazard;
    /** Metres from the rider to the zone edge (negative inside the zone). */
    public final double edgeDistanceM;

    public HazardAlert(Kind kind, Hazard hazard, double edgeDistanceM) {
        this.kind = kind;
        this.hazard = hazard;
        this.edgeDistanceM = edgeDistanceM;
    }
}
