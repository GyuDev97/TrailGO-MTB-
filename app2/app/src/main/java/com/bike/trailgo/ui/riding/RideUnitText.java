package com.bike.trailgo.ui.riding;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.widget.TextView;

/** Shows "14.2 km/h" as a big number with a smaller, grey unit. Values without a unit stay plain. */
final class RideUnitText {
    private static final int UNIT_COLOR = 0xFF68716B;
    private static final float UNIT_SCALE = 0.62f;

    private RideUnitText() {
    }

    static void set(TextView view, CharSequence text) {
        String value = text == null ? "" : text.toString();
        int split = unitStart(value);
        if (split < 0) {
            view.setText(value);
            return;
        }
        SpannableString styled = new SpannableString(value);
        styled.setSpan(new RelativeSizeSpan(UNIT_SCALE), split, value.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new ForegroundColorSpan(UNIT_COLOR), split, value.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.setText(styled);
    }

    /** Index where the unit starts (after the last space of "number unit"), or -1 when there is none. */
    static int unitStart(String value) {
        int space = value.lastIndexOf(' ');
        if (space <= 0 || space >= value.length() - 1) {
            return -1;
        }
        // only treat it as "number unit" when the part before the space really is a number
        String number = value.substring(0, space);
        if (!number.matches("-?[0-9]+([.,][0-9]+)?")) {
            return -1;
        }
        return space + 1;
    }
}
