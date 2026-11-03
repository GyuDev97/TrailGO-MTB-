package com.bike.trailgo.ui.riding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class RideStartPointTest {
    @Test
    public void usesTheFirstRecordedPosition() {
        RideStartPoint point = RideStartPoint.firstValid(
                Arrays.asList(35.8329, 35.84), Arrays.asList(128.565, 128.57));
        assertNotNull(point);
        assertEquals(35.8329, point.latitude, 0.0);
        assertEquals(128.565, point.longitude, 0.0);
    }

    @Test
    public void skipsUnusableLeadingPositions() {
        RideStartPoint point = RideStartPoint.firstValid(
                Arrays.asList(0.0, Double.NaN, 91.0, 35.8329), Arrays.asList(0.0, 128.0, 128.0, 128.565));
        assertNotNull(point);
        assertEquals(35.8329, point.latitude, 0.0);
    }

    @Test
    public void noUsablePositionMeansNoCoordinates() {
        assertNull(RideStartPoint.firstValid(null, null));
        assertNull(RideStartPoint.firstValid(Collections.<Double>emptyList(), Collections.<Double>emptyList()));
        assertNull(RideStartPoint.firstValid(Arrays.asList(0.0), Arrays.asList(0.0)));
        // mismatched list sizes never read past the shorter list
        assertNull(RideStartPoint.firstValid(Arrays.asList(35.0), Collections.<Double>emptyList()));
    }
}
