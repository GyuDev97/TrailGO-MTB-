package com.bike.trailgo.hazard;

import java.util.Collections;
import java.util.List;

/** Response of GET /app/hazards/nearby. */
public class NearbyHazards {
    public List<Hazard> items;
    public int count;
    public int radiusM;
    /** More zones matched than were returned: absence from the list proves nothing then. */
    public boolean truncated;
    public long serverTime;
    /**
     * Zones in the queried area that riders must not keep (hidden, rejected, expired, or merged into another zone).
     * Unlike "absent from the list" this is explicit, so it also works when the answer is truncated.
     */
    public List<Integer> removedIds;

    public List<Integer> removedIdsOrEmpty() {
        return removedIds == null ? Collections.<Integer>emptyList() : removedIds;
    }

    public List<Hazard> itemsOrEmpty() {
        return items == null ? Collections.<Hazard>emptyList() : items;
    }
}
