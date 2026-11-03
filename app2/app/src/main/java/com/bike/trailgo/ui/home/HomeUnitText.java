package com.bike.trailgo.ui.home;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.widget.TextView;

/** Shows a number large and its unit small ("49" + "m") in one TextView. */
final class HomeUnitText {
    private static final int UNIT_COLOR = 0xFF5B665F;

    private HomeUnitText() {
    }

    static void set(TextView view, HomeFormat.Value value, float unitScale) {
        if (value.unit.isEmpty()) {
            view.setText(value.number);
            return;
        }
        String text = value.number + " " + value.unit;
        SpannableString styled = new SpannableString(text);
        int start = value.number.length() + 1;
        styled.setSpan(new RelativeSizeSpan(unitScale), start, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new ForegroundColorSpan(UNIT_COLOR), start, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.setText(styled);
        view.setContentDescription(null);
    }

    static void clear(TextView view) {
        view.setText("-");
    }
}
