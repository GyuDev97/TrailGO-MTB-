package com.bike.trailgo.rideassistant;

import com.bike.trailgo.dto.DangerousArea;

import org.junit.Test;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RideAssistantTest {
    @Test
    public void detectsLongClimbAfterSustainedGainWithoutPaceDrop() {
        RideAssistantPolicy policy = new RideAssistantPolicy();
        RideContextEngine engine = new RideContextEngine(policy);
        RideContext context = null;
        long start = 1_700_000_000_000L;
        for (int i = 0; i <= 22; i++) {
            context = engine.add(new RideSample(start + i * 30_000L, 37.0d + i * 0.0001d,
                    127.0d, 16.0d, 100.0d + i * 5.0d, 5.0d));
        }
        assertEquals(RideState.LONG_CLIMB, new RideStateDetector(policy).detect(context));
    }

    @Test
    public void dangerThresholdsFireOncePerDangerAndThreshold() {
        RideAssistantPolicy policy = new RideAssistantPolicy();
        DangerAreaMonitor monitor = new DangerAreaMonitor(policy);
        DangerousArea area = new DangerousArea();
        area.setDa_num(42);
        area.setDa_latitude("37.0");
        area.setDa_longitude("127.0");
        area.setDa_content("낙석 주의");
        monitor.setAreas(Collections.singletonList(area));

        RideEvent approach = monitor.evaluate(contextAt(36.9982d, 127.0d));
        assertNotNull(approach);
        assertEquals(RideEventType.DANGER_APPROACH, approach.type);
        assertNull(monitor.evaluate(contextAt(36.9982d, 127.0d)));
        assertNotNull(monitor.evaluate(contextAt(36.99955d, 127.0d)));
    }

    @Test
    public void testModeUsesFifteenSecondLongClimbWindow() {
        RideAssistantPolicy policy = new RideAssistantPolicy(true);
        assertEquals(15L, policy.longClimbMinimumSeconds);
        assertEquals(5L, policy.windowSeconds[0]);
        assertEquals(10L, policy.windowSeconds[1]);
        assertEquals(15L, policy.windowSeconds[2]);
    }

    @Test
    public void paceDropRequiresSustainedClimbContext() {
        RideAssistantPolicy policy = new RideAssistantPolicy(true);
        RideEventDetector detector = new RideEventDetector(policy);
        RideContext flat = contextAt(1_700_000_000_000L, RideState.FLAT, 10.0d, 15.0d, 5.0d);
        assertTrue(detector.detect(flat).isEmpty());

        long start = 1_700_000_010_000L;
        RideContext initialClimb = contextAt(start, RideState.CLIMB, 10.0d, 15.0d, 5.0d);
        assertTrue(detector.detect(initialClimb).isEmpty());
        RideContext sustainedClimb = contextAt(start + 5_000L, RideState.CLIMB, 10.0d, 15.0d, 5.0d);
        assertEquals(RideEventType.PACE_DROPPED, detector.detect(sustainedClimb).get(0).type);
    }

    @Test
    public void gyuvisRequestIncludesIdsGradeAndDangerContext() {
        RideContext context = contextAt(1_700_000_000_000L, RideState.CLIMB, 12.0d, 14.0d, 6.5d);
        RideEvent event = new RideEvent(RideEventType.DANGER_APPROACH, context.timestampMs, context,
                "42", 87.0d, "위험 지역");
        GyuvisRequest request = GyuvisRequest.from("ride-123", event);

        assertEquals("ride-123", request.rideId);
        assertEquals(event.eventId, request.eventId);
        assertEquals(6.5d, request.rideContext.estimatedGrade, 0.0d);
        assertEquals(87.0d, request.rideContext.nearestDangerDistance, 0.0d);
        assertEquals("42", request.rideContext.dangerId);
    }

    @Test
    public void longClimbWithPaceDropScenarioEmitsEventsInOrder() {
        RideAssistantPolicy policy = new RideAssistantPolicy();
        RideContextEngine engine = new RideContextEngine(policy);
        RideStateDetector stateDetector = new RideStateDetector(policy);
        RideEventDetector eventDetector = new RideEventDetector(policy);
        List<RideEventType> events = new ArrayList<>();
        long start = 1_700_000_000_000L;

        for (int index = 0; index < 26; index++) {
            double speed = index <= 20 ? 9.0d : Math.max(4.0d, 9.0d - (index - 20));
            RideContext raw = engine.add(new RideSample(start + index * 30_000L,
                    37.0d, 127.0d + index * 0.00085d, speed, 100.0d + index * 5.0d, 5.0d));
            RideContext context = raw.withState(stateDetector.detect(raw));
            for (RideEvent event : eventDetector.detect(context)) events.add(event.type);
        }

        assertTrue(events.contains(RideEventType.LONG_CLIMB_STARTED));
        assertTrue(events.contains(RideEventType.PACE_DROPPED));
        assertTrue(events.indexOf(RideEventType.LONG_CLIMB_STARTED)
                < events.indexOf(RideEventType.PACE_DROPPED));
    }

    private RideContext contextAt(double latitude, double longitude) {
        return contextAt(1_700_000_000_000L, RideState.FLAT, 12.0d, 12.0d, 0.0d, latitude, longitude);
    }

    private RideContext contextAt(long timestamp, RideState state, double speed, double avgSpeed5m, double grade) {
        return contextAt(timestamp, state, speed, avgSpeed5m, grade, 37.0d, 127.0d);
    }

    private RideContext contextAt(long timestamp, RideState state, double speed, double avgSpeed5m,
                                  double grade, double latitude, double longitude) {
        RideSample sample = new RideSample(timestamp, latitude, longitude, speed, 100.0d, 5.0d);
        return new RideContext(sample.timestampMs, sample, speed, avgSpeed5m, avgSpeed5m,
                100.0d, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                100, 0, 0, grade, state);
    }
}
