package com.bike.trailgo.gps;

/**
 * What the "GPS" chip on the riding screen says. It reflects the real state of location delivery,
 * not just whether the permission was granted: a fresh position must actually be arriving.
 */
public final class GpsStatusPolicy {

    public enum State {
        PERMISSION_NEEDED("GPS 권한 필요", Tone.BAD),
        PROVIDER_OFF("GPS 꺼짐", Tone.BAD),
        SEARCHING("GPS 신호 찾는 중", Tone.NEUTRAL),
        LOST("GPS 신호 끊김", Tone.BAD),
        WEAK("GPS 신호 약함", Tone.WARN),
        CONNECTED("GPS 연결됨", Tone.GOOD);

        public final String label;
        public final Tone tone;

        State(String label, Tone tone) {
            this.label = label;
            this.tone = tone;
        }
    }

    public enum Tone { GOOD, WARN, BAD, NEUTRAL }

    private GpsStatusPolicy() {
    }

    /**
     * @param hasPermission          precise location permission granted
     * @param providerEnabled        the device's GPS location provider is switched on
     * @param millisSinceFreshFix    time since the last position that was not stale, or -1 when none yet
     * @param lastAccuracyMeters     horizontal accuracy of that position (NaN when unknown)
     * @param lostAfterMillis        no fresh position for this long counts as lost
     * @param weakAccuracyMeters     worse accuracy than this counts as a weak signal
     */
    public static State evaluate(boolean hasPermission, boolean providerEnabled, long millisSinceFreshFix,
                                 double lastAccuracyMeters, long lostAfterMillis, double weakAccuracyMeters) {
        if (!hasPermission) {
            return State.PERMISSION_NEEDED;
        }
        if (!providerEnabled) {
            return State.PROVIDER_OFF;
        }
        if (millisSinceFreshFix < 0) {
            return State.SEARCHING;
        }
        if (millisSinceFreshFix > lostAfterMillis) {
            return State.LOST;
        }
        if (!Double.isNaN(lastAccuracyMeters) && lastAccuracyMeters > weakAccuracyMeters) {
            return State.WEAK;
        }
        return State.CONNECTED;
    }
}
