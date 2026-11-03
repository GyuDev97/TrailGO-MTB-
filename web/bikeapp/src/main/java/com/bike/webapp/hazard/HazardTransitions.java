package com.bike.webapp.hazard;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which review status may follow which. One table for the service (which enforces it) and the admin
 * screens (which only offer buttons the table allows), so a button can never be shown for a move the
 * server would refuse.
 *
 * <pre>
 * PENDING / REVIEWING -> APPROVED, REJECTED, hidden (INACTIVE), and between the two
 * APPROVED           -> hidden only (to withdraw an approval, hide it)
 * REJECTED / EXPIRED -> back to PENDING (review again), hidden
 * INACTIVE (hidden)  -> PENDING (shown again, to be reviewed)
 * MERGED             -> nothing: a merged zone only lives on as part of its representative zone
 * </pre>
 */
public final class HazardTransitions {

    public static final String PENDING = "PENDING";
    public static final String REVIEWING = "REVIEWING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String INACTIVE = "INACTIVE";
    public static final String EXPIRED = "EXPIRED";
    public static final String MERGED = "MERGED";

    private static final Map<String, Set<String>> NEXT = new HashMap<String, Set<String>>();

    static {
        NEXT.put(PENDING, set(REVIEWING, APPROVED, REJECTED, INACTIVE));
        NEXT.put(REVIEWING, set(PENDING, APPROVED, REJECTED, INACTIVE));
        NEXT.put(APPROVED, set(INACTIVE));
        NEXT.put(REJECTED, set(PENDING, INACTIVE));
        NEXT.put(EXPIRED, set(PENDING, INACTIVE));
        NEXT.put(INACTIVE, set(PENDING));
        NEXT.put(MERGED, Collections.<String>emptySet());
    }

    private HazardTransitions() {
    }

    public static boolean isKnown(String status) {
        return NEXT.containsKey(status);
    }

    public static boolean canMove(String from, String to) {
        Set<String> allowed = NEXT.get(from);
        return allowed != null && allowed.contains(to);
    }

    /** Whether riders can currently see a zone in this status (see HazardApp.xml). */
    public static boolean isVisibleToRiders(String status) {
        return PENDING.equals(status) || REVIEWING.equals(status) || APPROVED.equals(status);
    }

    private static Set<String> set(String... values) {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(values)));
    }
}
