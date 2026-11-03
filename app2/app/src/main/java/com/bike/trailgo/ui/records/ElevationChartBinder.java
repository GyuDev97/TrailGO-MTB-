package com.bike.trailgo.ui.records;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

/**
 * Draws an {@link ElevationProfile} (x = distance along the track, y = GPS altitude). Used by both
 * the save screen and the record detail so the same data yields the same graph or the same message.
 */
public final class ElevationChartBinder {
    private ElevationChartBinder() {
    }

    /**
     * Shows the graph when the profile is available, otherwise hides it and shows the reason.
     *
     * @return true when the graph is shown
     */
    public static boolean bind(LineChart chart, TextView emptyMessage, ElevationProfile profile) {
        if (!profile.isAvailable()) {
            chart.setVisibility(View.GONE);
            emptyMessage.setText(profile.getAvailability().message);
            emptyMessage.setVisibility(View.VISIBLE);
            return false;
        }
        draw(chart, profile);
        emptyMessage.setVisibility(View.GONE);
        chart.setVisibility(View.VISIBLE);
        return true;
    }

    public static void draw(LineChart chart, ElevationProfile profile) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (ElevationProfile.Sample sample : profile.getSamples()) {
            entries.add(new Entry((float) (sample.distanceMeters / 1000.0), (float) sample.elevationMeters));
        }
        LineDataSet dataSet = new LineDataSet(entries, "Elevation");
        dataSet.setColor(Color.parseColor("#1F8A3B"));
        dataSet.setLineWidth(2.25f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E4F4E7"));

        chart.setData(new LineData(dataSet));
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setDrawLabels(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setDrawAxisLine(false);
        chart.getAxisLeft().setDrawLabels(false);
        chart.getAxisLeft().setDrawAxisLine(false);
        chart.getAxisLeft().setGridColor(Color.parseColor("#E7ECE8"));
        chart.getAxisRight().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.invalidate();
    }
}
