package com.bike.trailgo.hazard;

import com.bike.trailgo.gps.GeoMath;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The hazard zones the app already knows. Detection reads from here, so a ride keeps warning when the network
 * drops. Zones are merged per query area: a zone that disappears from a complete answer (hidden or removed
 * by an operator) disappears here too. Persisted through a tiny {@link Storage} so it survives restarts.
 */
public final class HazardCache {

    public interface Storage {
        /** @return the stored JSON or null */
        String read();

        void write(String json);
    }

    public static final long DEFAULT_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L;
    public static final int DEFAULT_MAX_ENTRIES = 1000;
    /** A zone this close to the edge of the queried circle is not expected to be in the answer. */
    private static final double EDGE_SAFETY_M = 50.0;

    private final Storage storage;
    private final long maxAgeMs;
    private final int maxEntries;
    private final Gson gson = new Gson();
    private final Map<Integer, Hazard> byId = new HashMap<>();

    public HazardCache(Storage storage, long maxAgeMs, int maxEntries) {
        this.storage = storage;
        this.maxAgeMs = maxAgeMs;
        this.maxEntries = maxEntries;
    }

    public static HazardCache create(Storage storage) {
        return new HazardCache(storage, DEFAULT_MAX_AGE_MS, DEFAULT_MAX_ENTRIES);
    }

    /** Reads the persisted zones; corrupt or unreadable data is ignored (the cache just starts empty). */
    public synchronized void load(long nowMs) {
        byId.clear();
        String json;
        try {
            json = storage.read();
        } catch (RuntimeException e) {
            return;
        }
        if (json == null || json.isEmpty()) return;
        try {
            List<Hazard> saved = gson.fromJson(json, new TypeToken<List<Hazard>>() { }.getType());
            if (saved == null) return;
            for (Hazard hazard : saved) {
                if (hazard != null && hazard.hasValidPosition() && nowMs - hazard.cachedAtMs <= maxAgeMs) {
                    byId.put(hazard.id, hazard);
                }
            }
        } catch (RuntimeException e) {
            byId.clear();
        }
    }

    public synchronized List<Hazard> snapshot() {
        return new ArrayList<>(byId.values());
    }

    public synchronized boolean isEmpty() {
        return byId.isEmpty();
    }

    /**
     * Applies a server answer for the circle (lat, lon, radiusM).
     *
     * @param complete false when the server said the answer was cut short: then nothing can be concluded from
     *                 a zone's absence, so nothing is removed
     * @return the zones now known
     */
    public synchronized List<Hazard> merge(double lat, double lon, int radiusM, List<Hazard> fresh,
                                           boolean complete, long nowMs) {
        return merge(lat, lon, radiusM, fresh, complete, nowMs, Collections.<Integer>emptyList());
    }

    /**
     * @param removedIds zones the server says riders must not keep any more (hidden, rejected, merged into another
     *                   zone). They are dropped even when the answer was truncated or they sit near the edge of the
     *                   queried circle, and a zone listed here never comes back from {@code fresh}.
     */
    public synchronized List<Hazard> merge(double lat, double lon, int radiusM, List<Hazard> fresh,
                                           boolean complete, long nowMs, List<Integer> removedIds) {
        Map<Integer, Hazard> incoming = new HashMap<>();
        for (Hazard hazard : fresh) {
            if (hazard != null && hazard.hasValidPosition()) {
                hazard.cachedAtMs = nowMs;
                incoming.put(hazard.id, hazard);
            }
        }
        if (complete) {
            List<Integer> gone = new ArrayList<>();
            for (Hazard known : byId.values()) {
                double edge = GeoMath.distanceMeters(lat, lon, known.latitude, known.longitude) - known.effectiveRadiusM();
                if (edge <= radiusM - EDGE_SAFETY_M && !incoming.containsKey(known.id)) gone.add(known.id);
            }
            for (Integer id : gone) byId.remove(id);
        }
        byId.putAll(incoming);
        if (removedIds != null) {
            for (Integer id : removedIds) {
                if (id != null) byId.remove(id);
            }
        }
        prune(lat, lon, nowMs);
        persist();
        return snapshot();
    }

    /** Drops a zone everywhere (the rider withdrew their own report). */
    public synchronized void remove(int id) {
        if (byId.remove(id) != null) persist();
    }

    public synchronized void clear() {
        byId.clear();
        persist();
    }

    private void prune(final double lat, final double lon, long nowMs) {
        List<Integer> expired = new ArrayList<>();
        for (Hazard hazard : byId.values()) {
            if (nowMs - hazard.cachedAtMs > maxAgeMs) expired.add(hazard.id);
        }
        for (Integer id : expired) byId.remove(id);
        if (byId.size() <= maxEntries) return;
        List<Hazard> all = new ArrayList<>(byId.values());
        Collections.sort(all, new Comparator<Hazard>() {
            @Override
            public int compare(Hazard a, Hazard b) {
                return Double.compare(GeoMath.distanceMeters(lat, lon, a.latitude, a.longitude),
                        GeoMath.distanceMeters(lat, lon, b.latitude, b.longitude));
            }
        });
        for (int i = maxEntries; i < all.size(); i++) byId.remove(all.get(i).id);
    }

    private void persist() {
        try {
            storage.write(gson.toJson(new ArrayList<>(byId.values())));
        } catch (RuntimeException ignored) {
            // an unwritable cache only costs the offline copy; the ride goes on
        }
    }
}
