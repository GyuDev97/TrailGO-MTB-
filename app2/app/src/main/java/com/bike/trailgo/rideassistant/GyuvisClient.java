package com.bike.trailgo.rideassistant;

import android.util.Log;

import com.bike.trailgo.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Optional event-only client. It never blocks location collection or local alerts. */
public final class GyuvisClient {
    private static final String TAG = "GyuvisClient";
    public interface Listener {
        void onResponse(RideEvent event, GyuvisResponse response, int httpStatus);
        void onFailure(RideEvent event, String reason);
    }

    private final GyuvisApi api;

    private GyuvisClient(GyuvisApi api) {
        this.api = api;
    }

    public static GyuvisClient createIfConfigured(String baseUrl, final String apiToken) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) return null;
        String normalized = baseUrl.trim();
        if (!normalized.endsWith("/")) normalized += "/";
        try {
            OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder()
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(6, TimeUnit.SECONDS)
                    .writeTimeout(6, TimeUnit.SECONDS);
            if (apiToken != null && !apiToken.trim().isEmpty()) {
                httpBuilder.addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .header("X-API-Token", apiToken.trim())
                            .build();
                    return chain.proceed(request);
                });
            }
            OkHttpClient http = httpBuilder.build();
            GyuvisApi api = new Retrofit.Builder().baseUrl(normalized).client(http)
                    .addConverterFactory(GsonConverterFactory.create()).build().create(GyuvisApi.class);
            return new GyuvisClient(api);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public void send(final String rideId, final RideEvent event, final Listener listener) {
        try {
            api.submitRideEvent(GyuvisRequest.from(rideId, event)).enqueue(new Callback<GyuvisResponse>() {
                @Override
                public void onResponse(Call<GyuvisResponse> call, Response<GyuvisResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponse(event, response.body(), response.code());
                    } else {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "GYUVIS response event_id=" + event.eventId + " http=" + response.code());
                        }
                        listener.onFailure(event, "HTTP " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<GyuvisResponse> call, Throwable ignored) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "GYUVIS request failed event_id=" + event.eventId);
                    listener.onFailure(event, "NETWORK_FAILURE");
                    // The ride, local warning, GPX, and UI intentionally continue without GYUVIS.
                }
            });
        } catch (RuntimeException ignored) {
            if (BuildConfig.DEBUG) Log.d(TAG, "GYUVIS request setup failed event_id=" + event.eventId);
            listener.onFailure(event, "REQUEST_SETUP_FAILURE");
            // Protect the foreground location service from malformed network data or configuration.
        }
    }
}
