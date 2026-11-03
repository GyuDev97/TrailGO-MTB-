package com.bike.trailgo.ui.photos;

/** Size maths for preparing ride photos (pure Java, unit tested). */
public final class PhotoSizing {
    /** Longest edge of a stored ride photo: sharp on a phone, ~0.3-1 MB as JPEG. */
    public static final int MAX_EDGE = 1600;
    public static final int JPEG_QUALITY = 85;

    private PhotoSizing() {
    }

    /**
     * {@code BitmapFactory.Options.inSampleSize} for decoding: the largest power of two that still
     * leaves the longest edge at or above {@code maxEdge}, so the full original is never loaded.
     */
    public static int sampleSize(int width, int height, int maxEdge) {
        if (width <= 0 || height <= 0 || maxEdge <= 0) {
            return 1;
        }
        int longest = Math.max(width, height);
        int sample = 1;
        while (longest / (sample * 2) >= maxEdge) {
            sample *= 2;
        }
        return sample;
    }

    /** Target size keeping the aspect ratio; never enlarges a small photo. */
    public static int[] scaledSize(int width, int height, int maxEdge) {
        int longest = Math.max(width, height);
        if (longest <= maxEdge || longest <= 0) {
            return new int[]{width, height};
        }
        double scale = (double) maxEdge / longest;
        return new int[]{Math.max(1, (int) Math.round(width * scale)), Math.max(1, (int) Math.round(height * scale))};
    }

    /** Clockwise degrees for an EXIF orientation value (flips are ignored). */
    public static int rotationDegrees(int exifOrientation) {
        switch (exifOrientation) {
            case 3:
            case 4:
                return 180;
            case 5:
            case 6:
                return 90;
            case 7:
            case 8:
                return 270;
            default:
                return 0;
        }
    }
}
