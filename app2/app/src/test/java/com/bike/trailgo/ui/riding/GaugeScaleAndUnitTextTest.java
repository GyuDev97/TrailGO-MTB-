package com.bike.trailgo.ui.riding;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GaugeScaleAndUnitTextTest {
    @Test
    public void scaleStepsUpForFastRidesAndBackDownWithHysteresis() {
        assertEquals(0, GaugeScale.indexFor(0, 0f));
        assertEquals(0, GaugeScale.indexFor(0, 40f));
        assertEquals(1, GaugeScale.indexFor(0, 41f));
        assertEquals(3, GaugeScale.indexFor(0, 95f));
        // stays on the larger scale a bit below its predecessor, so the needle does not flicker
        assertEquals(1, GaugeScale.indexFor(1, 38f));
        assertEquals(0, GaugeScale.indexFor(1, 33f));
        assertEquals(0, GaugeScale.indexFor(3, 0f));
    }

    @Test
    public void ticksAreFourEqualWholeIntervalsOnEveryScale() {
        for (int index = 0; index < GaugeScale.STEPS.length; index++) {
            float interval = GaugeScale.tickValue(index, 1);
            assertEquals(interval, Math.round(interval), 0f);
            assertEquals(0f, GaugeScale.tickValue(index, 0), 0f);
            assertEquals(GaugeScale.STEPS[index], GaugeScale.tickValue(index, GaugeScale.INTERVALS), 0f);
            assertEquals(interval * 2, GaugeScale.tickValue(index, 2), 0f);
        }
    }

    @Test
    public void progressIsClampedToTheScale() {
        assertEquals(0f, GaugeScale.progress(0, 0f), 0f);
        assertEquals(0.5f, GaugeScale.progress(0, 20f), 0f);
        assertEquals(1f, GaugeScale.progress(0, 55f), 0f);
        assertEquals(0f, GaugeScale.progress(0, -3f), 0f);
    }

    @Test
    public void onlyRealNumberPlusUnitIsSplit() {
        assertEquals(5, RideUnitText.unitStart("14.2 km/h"));
        assertEquals(4, RideUnitText.unitStart("365 m"));
        assertEquals(3, RideUnitText.unitStart("-3 m"));
        assertEquals(-1, RideUnitText.unitStart("-"));
        assertEquals(-1, RideUnitText.unitStart("GPS 수신 대기"));
        assertEquals(-1, RideUnitText.unitStart("0.0"));
    }
}
