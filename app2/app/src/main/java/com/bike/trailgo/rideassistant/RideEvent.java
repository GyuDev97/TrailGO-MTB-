package com.bike.trailgo.rideassistant;

import java.util.UUID;

public final class RideEvent {
    public final String eventId;
    public final RideEventType type;
    public final long timestampMs;
    public final RideContext context;
    public final String dangerId;
    public final double dangerDistanceMeters;
    public final String localSafetyMessage;

    public RideEvent(RideEventType type, long timestampMs, RideContext context) {
        this(type, timestampMs, context, null, -1.0d, null);
    }

    public RideEvent(RideEventType type, long timestampMs, RideContext context, String dangerId,
                     double dangerDistanceMeters, String localSafetyMessage) {
        this.eventId = UUID.randomUUID().toString();
        this.type = type;
        this.timestampMs = timestampMs;
        this.context = context;
        this.dangerId = dangerId;
        this.dangerDistanceMeters = dangerDistanceMeters;
        this.localSafetyMessage = localSafetyMessage;
    }
}
