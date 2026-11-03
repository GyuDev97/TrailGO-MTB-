package com.bike.trailgo.ui.riding;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CourseGuidanceEngineTest {
    private CourseGuidanceEngine newEngine() {
        CourseGuidanceEngine engine = new CourseGuidanceEngine();
        engine.setRoute(Arrays.asList(
                new CourseGuidanceEngine.Point(37.000000, 127.000000),
                new CourseGuidanceEngine.Point(37.000000, 127.010000)
        ));
        return engine;
    }

    @Test
    public void requiresTwoAccurateOffRouteSamplesBeforeWarning() {
        CourseGuidanceEngine engine = newEngine();
        engine.update(37.000000, 127.005000, true, 8f);
        CourseGuidanceEngine.Update first = engine.update(37.001000, 127.005000, true, 8f);
        CourseGuidanceEngine.Update second = engine.update(37.001000, 127.005000, true, 8f);

        assertFalse(first.offRoute);
        assertEquals(CourseGuidanceEngine.Event.NONE, first.event);
        assertTrue(second.offRoute);
        assertEquals(CourseGuidanceEngine.Event.OFF_ROUTE, second.event);
    }

    @Test
    public void returnsToNormalOnlyAfterReturningInsideHysteresisDistance() {
        CourseGuidanceEngine engine = newEngine();
        engine.update(37.001000, 127.005000, true, 8f);
        engine.update(37.001000, 127.005000, true, 8f);
        CourseGuidanceEngine.Update recovered = engine.update(37.000000, 127.006000, true, 8f);

        assertFalse(recovered.offRoute);
        assertEquals(CourseGuidanceEngine.Event.BACK_ON_ROUTE, recovered.event);
        assertTrue(recovered.progressPercent() >= 50);
    }

    @Test
    public void ignoresPoorAccuracyForOffRouteDecision() {
        CourseGuidanceEngine engine = newEngine();
        CourseGuidanceEngine.Update update = engine.update(37.002000, 127.005000, true, 80f);

        assertFalse(update.usableLocation);
        assertFalse(update.offRoute);
        assertEquals(CourseGuidanceEngine.Event.LOW_ACCURACY, update.event);
    }
}
