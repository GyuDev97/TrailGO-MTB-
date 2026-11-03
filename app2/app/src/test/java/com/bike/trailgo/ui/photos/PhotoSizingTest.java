package com.bike.trailgo.ui.photos;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PhotoSizingTest {
    @Test
    public void bigPhonePhotosAreDecodedAtAReducedSize() {
        // 4000x3000: halving once gives 2000 (>= 1600), halving twice would give 1000 (< 1600)
        assertEquals(2, PhotoSizing.sampleSize(4000, 3000, PhotoSizing.MAX_EDGE));
        // 12000x9000 (108 MP): 12000/4 = 3000 >= 1600, /8 = 1500 < 1600
        assertEquals(4, PhotoSizing.sampleSize(12000, 9000, PhotoSizing.MAX_EDGE));
    }

    @Test
    public void smallPhotosAreNotSubsampled() {
        assertEquals(1, PhotoSizing.sampleSize(1600, 1200, PhotoSizing.MAX_EDGE));
        assertEquals(1, PhotoSizing.sampleSize(800, 600, PhotoSizing.MAX_EDGE));
        assertEquals(1, PhotoSizing.sampleSize(0, 0, PhotoSizing.MAX_EDGE));
    }

    @Test
    public void scalingKeepsTheAspectRatioAndNeverEnlarges() {
        assertArrayEquals(new int[]{1600, 1200}, PhotoSizing.scaledSize(2000, 1500, 1600));
        assertArrayEquals(new int[]{1200, 1600}, PhotoSizing.scaledSize(1500, 2000, 1600));
        assertArrayEquals(new int[]{800, 600}, PhotoSizing.scaledSize(800, 600, 1600));
    }

    @Test
    public void exifOrientationMapsToRotation() {
        assertEquals(0, PhotoSizing.rotationDegrees(1));
        assertEquals(90, PhotoSizing.rotationDegrees(6));
        assertEquals(180, PhotoSizing.rotationDegrees(3));
        assertEquals(270, PhotoSizing.rotationDegrees(8));
        assertEquals(0, PhotoSizing.rotationDegrees(0));
    }
}
