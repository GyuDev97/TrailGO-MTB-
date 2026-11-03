package com.bike.trailgo.gps;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GpsStatusPolicyTest {
    private static GpsStatusPolicy.State state(boolean permission, boolean provider, long since, double accuracy) {
        return GpsStatusPolicy.evaluate(permission, provider, since, accuracy, 10_000L, 40.0);
    }

    @Test
    public void permissionAloneIsNeverEnoughToSayConnected() {
        assertEquals(GpsStatusPolicy.State.SEARCHING, state(true, true, -1, Double.NaN));
        assertEquals(GpsStatusPolicy.State.CONNECTED, state(true, true, 1_500, 6.0));
    }

    @Test
    public void missingPermissionOrSwitchedOffGpsWinsOverOldFixes() {
        assertEquals(GpsStatusPolicy.State.PERMISSION_NEEDED, state(false, true, 500, 5.0));
        assertEquals(GpsStatusPolicy.State.PROVIDER_OFF, state(true, false, 500, 5.0));
    }

    @Test
    public void noFreshPositionForTooLongMeansLost() {
        assertEquals(GpsStatusPolicy.State.CONNECTED, state(true, true, 10_000, 5.0));
        assertEquals(GpsStatusPolicy.State.LOST, state(true, true, 10_001, 5.0));
    }

    @Test
    public void poorAccuracyIsReportedAsWeak() {
        assertEquals(GpsStatusPolicy.State.WEAK, state(true, true, 2_000, 55.0));
        assertEquals(GpsStatusPolicy.State.CONNECTED, state(true, true, 2_000, 40.0));
        assertEquals(GpsStatusPolicy.State.CONNECTED, state(true, true, 2_000, Double.NaN));
    }

    @Test
    public void everyStateHasALabelAndATone() {
        for (GpsStatusPolicy.State s : GpsStatusPolicy.State.values()) {
            assertEquals(false, s.label.isEmpty());
            assertEquals(false, s.tone == null);
        }
    }
}
