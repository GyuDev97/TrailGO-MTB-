package com.bike.trailgo.rideassistant;

/** Current riding condition; this deliberately is not a notification event. */
public enum RideState {
    FLAT,
    CLIMB,
    LONG_CLIMB,
    DESCENT,
    STOPPED
}
