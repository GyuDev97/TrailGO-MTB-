package com.bike.trailgo.rideassistant;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

interface GyuvisApi {
    @POST("api/events/riding")
    Call<GyuvisResponse> submitRideEvent(@Body GyuvisRequest request);
}
