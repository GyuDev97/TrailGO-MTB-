package com.bike.trailgo.ui.riding;

/**
 * Scale of the speedometer. The range steps up (40 -> 60 -> 80 -> 100 km/h) when the rider goes
 * faster, and steps back down only well below the previous step, so the needle does not flicker
 * between scales. Every scale has four equal intervals, so ticks are always whole, even numbers.
 */
final class GaugeScale {
    static final float[] STEPS = {40f, 60f, 80f, 100f};
    static final int INTERVALS = 4;
    private static final float STEP_DOWN_RATIO = 0.85f;

    private GaugeScale() {
    }

    /** @return the index into {@link #STEPS} to use for this speed, given the one used so far. */
    static int indexFor(int currentIndex, float speedKmh) {
        int index = Math.max(0, Math.min(currentIndex, STEPS.length - 1));
        while (index < STEPS.length - 1 && speedKmh > STEPS[index]) {
            index++;
        }
        while (index > 0 && speedKmh < STEPS[index - 1] * STEP_DOWN_RATIO) {
            index--;
        }
        return index;
    }

    static float tickValue(int index, int tick) {
        return STEPS[index] * tick / INTERVALS;
    }

    static float progress(int index, float speedKmh) {
        float safe = Math.max(0f, Math.min(STEPS[index], speedKmh));
        return safe / STEPS[index];
    }
}
