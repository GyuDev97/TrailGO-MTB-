package com.bike.trailgo.ui.home;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Draws a ride's route as a small picture: a soft green tile with a faint grid, the route as a
 * green line with a white edge, a hollow green dot at the start and a hollow red dot at the end.
 *
 * <p>The tile is deliberately a stylised background, not map imagery, so it cannot be mistaken for
 * a real map. No network or map SDK is involved: the picture comes from the GPX points only.</p>
 */
final class RouteThumbnailRenderer {
    private static final int BACKGROUND = Color.parseColor("#E6EFE3");
    private static final int GRID = Color.parseColor("#D7E5D3");
    private static final int ROUTE = Color.parseColor("#1F7A2E");
    private static final int START = Color.parseColor("#1F7A2E");
    private static final int END = Color.parseColor("#D93025");

    private RouteThumbnailRenderer() {
    }

    /** @return the picture, or null when the points contain no valid position at all */
    @Nullable
    static Bitmap render(@Nullable List<double[]> latLon, int widthPx, int heightPx, float density) {
        float margin = 11f * density;
        RoutePath.Projected route = RoutePath.project(latLon, widthPx, heightPx, margin);
        if (route == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(BACKGROUND);

        Paint grid = new Paint();
        grid.setColor(GRID);
        grid.setStrokeWidth(Math.max(1f, density));
        float cell = 14f * density;
        for (float x = cell; x < widthPx; x += cell) {
            canvas.drawLine(x, 0, x, heightPx, grid);
        }
        for (float y = cell; y < heightPx; y += cell) {
            canvas.drawLine(0, y, widthPx, y, grid);
        }

        float startX = route.x[0];
        float startY = route.y[0];
        float endX = route.x[route.size() - 1];
        float endY = route.y[route.size() - 1];

        if (!route.singlePlace && route.size() >= 2) {
            Path path = new Path();
            path.moveTo(route.x[0], route.y[0]);
            for (int i = 1; i < route.size(); i++) {
                path.lineTo(route.x[i], route.y[i]);
            }
            Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeJoin(Paint.Join.ROUND);
            line.setColor(Color.WHITE);
            line.setStrokeWidth(5.5f * density);
            canvas.drawPath(path, line);
            line.setColor(ROUTE);
            line.setStrokeWidth(3f * density);
            canvas.drawPath(path, line);
        }

        if (route.singlePlace) {
            drawDot(canvas, startX, startY, START, density);
        } else {
            drawDot(canvas, endX, endY, END, density);
            drawDot(canvas, startX, startY, START, density);
        }
        return bitmap;
    }

    private static void drawDot(Canvas canvas, float x, float y, int color, float density) {
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(Color.WHITE);
        canvas.drawCircle(x, y, 4.5f * density, fill);
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(color);
        ring.setStrokeWidth(2f * density);
        canvas.drawCircle(x, y, 4.5f * density, ring);
    }
}
