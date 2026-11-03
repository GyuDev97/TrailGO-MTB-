package com.bike.trailgo.ui.home;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns GPS points into pixel positions for a small route picture (pure maths, unit tested).
 *
 * <p>Longitude is scaled by cos(latitude) so the shape is not stretched; the route is fitted to
 * the picture with a margin, keeping its proportions and centred. Invalid points are skipped,
 * long tracks are thinned (first and last point always kept).</p>
 */
public final class RoutePath {
    public static final int MAX_POINTS = 300;

    /** Pixel positions; {@code x[i]}/{@code y[i]} of the i-th point. */
    public static final class Projected {
        public final float[] x;
        public final float[] y;
        /** True when all points lie at (practically) the same place: draw a dot, not a line. */
        public final boolean singlePlace;

        Projected(float[] x, float[] y, boolean singlePlace) {
            this.x = x;
            this.y = y;
            this.singlePlace = singlePlace;
        }

        public int size() {
            return x.length;
        }
    }

    private static final double SAME_PLACE_DEGREES = 2e-6; // ~20 cm

    private RoutePath() {
    }

    /** @return the projected route, or null when there is no valid point at all */
    @Nullable
    public static Projected project(@Nullable List<double[]> latLon, float width, float height, float margin) {
        List<double[]> valid = validPoints(latLon);
        if (valid.isEmpty() || width <= 0 || height <= 0) {
            return null;
        }
        valid = thin(valid, MAX_POINTS);

        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        for (double[] p : valid) {
            minLat = Math.min(minLat, p[0]);
            maxLat = Math.max(maxLat, p[0]);
            minLon = Math.min(minLon, p[1]);
            maxLon = Math.max(maxLon, p[1]);
        }
        double midLat = Math.toRadians((minLat + maxLat) / 2.0);
        double spanX = (maxLon - minLon) * Math.cos(midLat);
        double spanY = maxLat - minLat;

        float[] xs = new float[valid.size()];
        float[] ys = new float[valid.size()];
        float usableW = Math.max(1f, width - 2 * margin);
        float usableH = Math.max(1f, height - 2 * margin);

        if (spanX < SAME_PLACE_DEGREES && spanY < SAME_PLACE_DEGREES) {
            for (int i = 0; i < xs.length; i++) {
                xs[i] = width / 2f;
                ys[i] = height / 2f;
            }
            return new Projected(xs, ys, true);
        }

        double scale;
        if (spanX < SAME_PLACE_DEGREES) {
            scale = usableH / spanY;
        } else if (spanY < SAME_PLACE_DEGREES) {
            scale = usableW / spanX;
        } else {
            scale = Math.min(usableW / spanX, usableH / spanY);
        }
        double drawnW = spanX * scale;
        double drawnH = spanY * scale;
        double offsetX = (width - drawnW) / 2.0;
        double offsetY = (height - drawnH) / 2.0;
        for (int i = 0; i < xs.length; i++) {
            double[] p = valid.get(i);
            xs[i] = (float) (offsetX + (p[1] - minLon) * Math.cos(midLat) * scale);
            ys[i] = (float) (offsetY + (maxLat - p[0]) * scale); // north is up
        }
        return new Projected(xs, ys, false);
    }

    static List<double[]> validPoints(@Nullable List<double[]> points) {
        List<double[]> valid = new ArrayList<>();
        if (points == null) {
            return valid;
        }
        for (double[] p : points) {
            if (p == null || p.length < 2) {
                continue;
            }
            double lat = p[0];
            double lon = p[1];
            if (Double.isNaN(lat) || Double.isNaN(lon) || Double.isInfinite(lat) || Double.isInfinite(lon)
                    || Math.abs(lat) > 90 || Math.abs(lon) > 180 || (lat == 0.0 && lon == 0.0)) {
                continue;
            }
            valid.add(p);
        }
        return valid;
    }

    static List<double[]> thin(List<double[]> points, int max) {
        if (points.size() <= max) {
            return points;
        }
        List<double[]> thinned = new ArrayList<>(max);
        double step = (points.size() - 1) / (double) (max - 1);
        for (int i = 0; i < max; i++) {
            thinned.add(points.get((int) Math.round(i * step)));
        }
        return thinned;
    }
}
