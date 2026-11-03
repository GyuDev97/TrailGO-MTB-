package com.bike.trailgo.rideassistant;

final class GyuvisResponse {
    boolean speak;
    String message;
    String priority;
    @com.google.gson.annotations.SerializedName("cooldown_sec") long cooldownSec;
}
