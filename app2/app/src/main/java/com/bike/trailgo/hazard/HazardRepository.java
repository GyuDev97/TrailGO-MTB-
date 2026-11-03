package com.bike.trailgo.hazard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.bike.trailgo.net.Server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Where the app gets hazard zones from: the persisted cache first (so a ride works offline), refreshed from
 * the server when {@link HazardRefreshPolicy} allows. Every failure is swallowed here: hazards are an
 * extra, never a reason for a ride, a map or the foreground service to fail.
 */
public final class HazardRepository {

    public interface Listener {
        /** Called on the main thread whenever the known zones changed. */
        void onHazardsChanged(List<Hazard> hazards);
    }

    private static final String PREFS = "trailgo_hazard_cache";
    private static final String KEY = "zones_v1";

    private static HazardRepository instance;

    private final HazardCache cache;
    private final HazardRefreshPolicy policy = new HazardRefreshPolicy();
    private final CopyOnWriteArraySet<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private Call<NearbyHazards> inFlight;

    private HazardRepository(final Context context) {
        final SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        cache = HazardCache.create(new HazardCache.Storage() {
            @Override
            public String read() {
                return prefs.getString(KEY, null);
            }

            @Override
            public void write(String json) {
                prefs.edit().putString(KEY, json).apply();
            }
        });
        cache.load(System.currentTimeMillis());
    }

    public static synchronized HazardRepository get(Context context) {
        if (instance == null) instance = new HazardRepository(context);
        return instance;
    }

    public List<Hazard> cached() {
        return cache.snapshot();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /** A new ride: ask the server again as soon as there is a position. */
    public void resetRefresh() {
        policy.reset();
    }

    /** Called with every location while riding; only sometimes does it send a request. */
    public void refreshIfDue(double lat, double lon) {
        long now = System.currentTimeMillis();
        if (!policy.shouldRefresh(lat, lon, now)) return;
        fetch(lat, lon, HazardRefreshPolicy.QUERY_RADIUS_M, policy, null);
    }

    /**
     * For map screens, which keep their own {@link HazardRefreshPolicy} (their own idea of "moved").
     *
     * @param done called on the main thread when the request ended, successful or not; may be null
     */
    public void refreshIfDue(double lat, double lon, int radiusM, HazardRefreshPolicy screenPolicy, Runnable done) {
        if (!screenPolicy.shouldRefresh(lat, lon, System.currentTimeMillis())) return;
        fetch(lat, lon, radiusM, screenPolicy, done);
    }

    /** Puts a zone the rider just reported into the cache so it shows and warns at once. */
    public void onReported(Hazard hazard) {
        if (hazard == null) return;
        cache.merge(hazard.latitude, hazard.longitude, 1, java.util.Collections.singletonList(hazard), false,
                System.currentTimeMillis());
        notifyChanged();
    }

    public void onWithdrawn(int id) {
        cache.remove(id);
        notifyChanged();
    }

    public void cancel() {
        if (inFlight != null) inFlight.cancel();
        inFlight = null;
    }

    private void fetch(final double lat, final double lon, final int radiusM, final HazardRefreshPolicy usedPolicy,
                       final Runnable done) {
        final Call<NearbyHazards> call;
        try {
            call = Server.getInstance().getApi().getNearbyHazards(lat, lon, radiusM);
        } catch (RuntimeException e) {
            return; // server client unavailable: stay on the cache
        }
        usedPolicy.onAttempt(System.currentTimeMillis());
        inFlight = call;
        call.enqueue(new Callback<NearbyHazards>() {
            @Override
            public void onResponse(Call<NearbyHazards> c, Response<NearbyHazards> response) {
                NearbyHazards body = response.body();
                if (!response.isSuccessful() || body == null) {
                    usedPolicy.onFailure();
                } else {
                    long now = System.currentTimeMillis();
                    cache.merge(lat, lon, radiusM, body.itemsOrEmpty(), !body.truncated, now, body.removedIdsOrEmpty());
                    usedPolicy.onSuccess(lat, lon, now);
                    notifyChanged();
                }
                finish(c, done);
            }

            @Override
            public void onFailure(Call<NearbyHazards> c, Throwable t) {
                usedPolicy.onFailure();
                finish(c, done);
            }
        });
    }

    private void finish(Call<NearbyHazards> call, Runnable done) {
        if (inFlight == call) inFlight = null;
        if (done != null) done.run();
    }

    private void notifyChanged() {
        final List<Hazard> snapshot = cache.snapshot();
        main.post(new Runnable() {
            @Override
            public void run() {
                for (Listener listener : listeners) listener.onHazardsChanged(snapshot);
            }
        });
    }
}
