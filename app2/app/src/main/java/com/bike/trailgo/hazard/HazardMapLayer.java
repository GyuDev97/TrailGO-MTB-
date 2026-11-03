package com.bike.trailgo.hazard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.bike.trailgo.gps.GeoMath;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelLayerOptions;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;
import com.kakao.vectormap.shape.DotPoints;
import com.kakao.vectormap.shape.Polygon;
import com.kakao.vectormap.shape.PolygonOptions;
import com.kakao.vectormap.shape.PolygonStyles;
import com.kakao.vectormap.shape.ShapeLayer;
import com.kakao.vectormap.shape.ShapeLayerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Draws hazard zones on a Kakao vector map: a warm-coloured marker per zone and a translucent circle of its
 * radius. {@link #render} diffs against what is already on the map, so repeated calls (a refresh, a camera
 * move) neither duplicate markers nor leak shapes, and zones that disappear are removed. Orange is an
 * unverified rider report, red an operator-approved one; the route line keeps its own green.
 */
public final class HazardMapLayer {

    public interface OnHazardClickListener {
        void onHazardClicked(Hazard hazard);
    }

    private static final String TAG = "HazardMapLayer";
    /** Below this zoom (smaller = further away) a 10-200 m circle is a speck, so only markers are drawn. */
    static final int MIN_CIRCLE_ZOOM = 13;
    /** Below this zoom only the nearest zones get a marker, to keep a country-wide view readable. */
    static final int MIN_FULL_MARKER_ZOOM = 12;
    static final int MAX_MARKERS_ZOOMED_OUT = 40;
    static final int MAX_MARKERS = 200;

    private static final int ORANGE_FILL = Color.argb(64, 255, 143, 0);
    private static final int ORANGE_STROKE = Color.rgb(230, 108, 0);
    private static final int RED_FILL = Color.argb(70, 229, 57, 53);
    private static final int RED_STROKE = Color.rgb(183, 28, 28);

    private static final class Entry {
        final Label label;
        final Polygon polygon;
        final String signature;

        Entry(Label label, Polygon polygon, String signature) {
            this.label = label;
            this.polygon = polygon;
            this.signature = signature;
        }
    }

    private final Context context;
    private final KakaoMap map;
    private final LabelLayer labelLayer;
    private final ShapeLayer shapeLayer;
    private final Map<Integer, Entry> shown = new HashMap<>();
    private final Map<String, LabelStyles> styleCache = new HashMap<>();
    private final Map<String, PolygonStyles> polygonStyleCache = new HashMap<>();
    private OnHazardClickListener clickListener;

    public HazardMapLayer(Context context, KakaoMap map) {
        this.context = context.getApplicationContext();
        this.map = map;
        this.labelLayer = map.getLabelManager().addLayer(LabelLayerOptions.from("hazard-labels").setZOrder(500));
        this.shapeLayer = map.getShapeManager().addLayer(ShapeLayerOptions.from("hazard-shapes", 5));
        map.setOnLabelClickListener((kakaoMap, layer, label) -> {
            Object tag = label.getTag();
            if (tag instanceof Hazard && clickListener != null) {
                clickListener.onHazardClicked((Hazard) tag);
                return true;
            }
            return false;
        });
    }

    public void setOnHazardClickListener(OnHazardClickListener listener) {
        clickListener = listener;
    }

    /**
     * @param center the point the view is centred on (zones nearest to it are kept when too many)
     * @param zoom   the map's zoom level
     */
    public void render(List<Hazard> hazards, LatLng center, int zoom) {
        try {
            doRender(hazards, center, zoom);
        } catch (RuntimeException e) {
            // an overlay problem must never take the riding screen down
            Log.w(TAG, "hazard overlay skipped", e);
        }
    }

    private void doRender(List<Hazard> hazards, LatLng center, int zoom) {
        List<Hazard> wanted = select(hazards, center, zoom);
        Set<Integer> keep = new HashSet<>();
        for (Hazard hazard : wanted) keep.add(hazard.id);

        List<Integer> stale = new ArrayList<>();
        for (Integer id : shown.keySet()) {
            if (!keep.contains(id)) stale.add(id);
        }
        for (Integer id : stale) remove(id);

        boolean circles = zoom >= MIN_CIRCLE_ZOOM;
        for (Hazard hazard : wanted) {
            String signature = signature(hazard, circles);
            Entry existing = shown.get(hazard.id);
            if (existing != null && existing.signature.equals(signature)) continue;
            if (existing != null) remove(hazard.id);
            add(hazard, circles, signature);
        }
    }

    /** Which zones to draw at this zoom: all up to a cap, the nearest few when zoomed far out. */
    static List<Hazard> select(List<Hazard> hazards, final LatLng center, int zoom) {
        List<Hazard> valid = new ArrayList<>();
        if (hazards != null) {
            for (Hazard hazard : hazards) {
                if (hazard != null && hazard.hasValidPosition()) valid.add(hazard);
            }
        }
        int limit = zoom < MIN_FULL_MARKER_ZOOM ? MAX_MARKERS_ZOOMED_OUT : MAX_MARKERS;
        if (valid.size() <= limit) return valid;
        Collections.sort(valid, new Comparator<Hazard>() {
            @Override
            public int compare(Hazard a, Hazard b) {
                return Double.compare(
                        GeoMath.distanceMeters(center.getLatitude(), center.getLongitude(), a.latitude, a.longitude),
                        GeoMath.distanceMeters(center.getLatitude(), center.getLongitude(), b.latitude, b.longitude));
            }
        });
        return new ArrayList<>(valid.subList(0, limit));
    }

    private void add(Hazard hazard, boolean circle, String signature) {
        LatLng position = LatLng.from(hazard.latitude, hazard.longitude);
        Polygon polygon = null;
        if (circle) {
            polygon = shapeLayer.addPolygon(PolygonOptions.from(
                    DotPoints.fromCircle(position, hazard.effectiveRadiusM()),
                    polygonStyles(hazard.verified)));
        }
        Label label = labelLayer.addLabel(LabelOptions.from("hazard-" + hazard.id, position)
                .setStyles(labelStyles(hazard.hazardType(), hazard.verified))
                .setTag(hazard)
                .setRank(100));
        shown.put(hazard.id, new Entry(label, polygon, signature));
    }

    private void remove(int id) {
        Entry entry = shown.remove(id);
        if (entry == null) return;
        if (entry.label != null) labelLayer.remove(entry.label);
        if (entry.polygon != null) shapeLayer.remove(entry.polygon);
    }

    /** Removes every marker and circle (display switched off, or the screen is going away). */
    public void clear() {
        for (Integer id : new ArrayList<>(shown.keySet())) remove(id);
    }

    private static String signature(Hazard hazard, boolean circle) {
        return hazard.type + "|" + hazard.verified + "|" + hazard.effectiveRadiusM() + "|" + hazard.latitude + "|"
                + hazard.longitude + "|" + circle + "|" + hazard.title;
    }

    private PolygonStyles polygonStyles(boolean verified) {
        String key = verified ? "v" : "u";
        PolygonStyles styles = polygonStyleCache.get(key);
        if (styles == null) {
            styles = PolygonStyles.from(verified ? RED_FILL : ORANGE_FILL, 2f, verified ? RED_STROKE : ORANGE_STROKE);
            polygonStyleCache.put(key, styles);
        }
        return styles;
    }

    private LabelStyles labelStyles(HazardType type, boolean verified) {
        String key = type.name() + verified;
        LabelStyles styles = styleCache.get(key);
        if (styles == null) {
            styles = map.getLabelManager().addLabelStyles(LabelStyles.from(
                    LabelStyle.from(markerBitmap(type, verified)).setAnchorPoint(0.5f, 0.5f)));
            styleCache.put(key, styles);
        }
        return styles;
    }

    /** A round warm badge with the type's symbol; drawn in code so no new image assets are needed. */
    private Bitmap markerBitmap(HazardType type, boolean verified) {
        float density = context.getResources().getDisplayMetrics().density;
        int size = Math.round(34 * density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float center = size / 2f;
        paint.setColor(Color.WHITE);
        canvas.drawCircle(center, center, center, paint);
        paint.setColor(verified ? RED_STROKE : ORANGE_STROKE);
        canvas.drawCircle(center, center, center - 2 * density, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(18 * density);
        Rect bounds = new Rect();
        paint.getTextBounds(type.glyph, 0, type.glyph.length(), bounds);
        canvas.drawText(type.glyph, center, center - bounds.exactCenterY(), paint);
        return bitmap;
    }
}
