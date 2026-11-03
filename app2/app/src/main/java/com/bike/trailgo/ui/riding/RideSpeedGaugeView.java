package com.bike.trailgo.ui.riding;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Half-circle speedometer. The green arc follows the current speed; there is no animation, so a
 * standing rider causes no redraws. The scale steps up for fast descents (see {@link GaugeScale})
 * and the ticks are labelled in whole km/h.
 */
public class RideSpeedGaugeView extends View {
    private final Paint trackPaint = createPaint("#E1E4E7", 16f);
    private final Paint progressPaint = createPaint("#237A35", 16f);
    private final Paint tickPaint = createPaint("#C9D0CB", 2f);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private float speedKmh;
    private int scaleIndex;

    public RideSpeedGaugeView(Context context) {
        super(context);
        init();
    }

    public RideSpeedGaugeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RideSpeedGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        labelPaint.setColor(Color.parseColor("#7A837D"));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        setContentDescription("현재 속도 0.0 킬로미터 매 시간");
    }

    public void setSpeedKmh(float speedKmh) {
        float safeSpeed = Math.max(0f, speedKmh);
        int newIndex = GaugeScale.indexFor(scaleIndex, safeSpeed);
        if (this.speedKmh != safeSpeed || newIndex != scaleIndex) {
            this.speedKmh = safeSpeed;
            this.scaleIndex = newIndex;
            setContentDescription(String.format(Locale.KOREA, "현재 속도 %.1f 킬로미터 매 시간", safeSpeed));
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float stroke = dp(16f);
        trackPaint.setStrokeWidth(stroke);
        progressPaint.setStrokeWidth(stroke);
        tickPaint.setStrokeWidth(dp(2f));
        labelPaint.setTextSize(dp(11f));

        float centerX = getWidth() / 2f;
        float bottom = getHeight() - dp(17f);
        // keep the whole arc (and its stroke) inside the view on narrow screens
        float radius = Math.min(centerX - dp(28f), bottom - dp(16f));
        if (radius <= 0f) {
            return;
        }

        arcBounds.set(centerX - radius, bottom - radius, centerX + radius, bottom + radius);
        canvas.drawArc(arcBounds, 180f, 180f, false, trackPaint);

        float progress = GaugeScale.progress(scaleIndex, speedKmh);
        if (progress > 0f) {
            canvas.drawArc(arcBounds, 180f, 180f * progress, false, progressPaint);
        }

        float outerRadius = radius - dp(16f);
        float innerRadius = outerRadius - dp(10f);
        float labelRadius = innerRadius - dp(12f);
        for (int tick = 0; tick <= GaugeScale.INTERVALS; tick++) {
            double angle = Math.PI + (Math.PI * tick / GaugeScale.INTERVALS);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            canvas.drawLine(centerX + cos * outerRadius, bottom + sin * outerRadius,
                    centerX + cos * innerRadius, bottom + sin * innerRadius, tickPaint);
            String label = String.valueOf(Math.round(GaugeScale.tickValue(scaleIndex, tick)));
            canvas.drawText(label, centerX + cos * labelRadius,
                    bottom + sin * labelRadius + dp(4f), labelPaint);
        }
    }

    private Paint createPaint(String color, float strokeWidthDp) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setColor(Color.parseColor(color));
        paint.setStrokeWidth(dp(strokeWidthDp));
        return paint;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
