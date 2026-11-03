package com.bike.trailgo.ui.photos;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/** Full-screen photo viewer with swipe, a counter and an optional delete button. */
public final class PhotoViewerDialog {
    public interface DeleteListener {
        /** Called for the photo currently shown; the dialog closes itself afterwards. */
        void onDelete(int index);
    }

    private PhotoViewerDialog() {
    }

    public static void show(Context context, List<PhotoStripView.Item> items, int startIndex,
                            final DeleteListener deleteListener) {
        if (items.isEmpty()) {
            return;
        }
        final List<PhotoStripView.Item> photos = new ArrayList<>(items);
        final Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Color.BLACK);

        final ViewPager pager = new ViewPager(context);
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return photos.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                ImageView image = new ImageView(container.getContext());
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setContentDescription("라이딩 사진 " + (position + 1));
                PhotoStripView.Item item = photos.get(position);
                // Bounded decode size: a 10-photo ride must not hold ten full-size bitmaps.
                if (item.file != null) {
                    Picasso.get().load(item.file).resize(2048, 2048).onlyScaleDown().centerInside().into(image);
                } else if (item.url != null) {
                    Picasso.get().load(item.url).resize(2048, 2048).onlyScaleDown().centerInside().into(image);
                }
                container.addView(image);
                return image;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        });
        root.addView(pager, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final TextView counter = new TextView(context);
        counter.setTextColor(Color.WHITE);
        counter.setTextSize(15);
        FrameLayout.LayoutParams counterParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        counterParams.topMargin = dp(context, 40);
        root.addView(counter, counterParams);
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                counter.setText((position + 1) + " / " + photos.size());
            }
        });

        TextView close = button(context, "닫기");
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        closeParams.topMargin = dp(context, 32);
        closeParams.rightMargin = dp(context, 12);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        root.addView(close, closeParams);

        if (deleteListener != null) {
            TextView delete = button(context, "삭제");
            delete.setTextColor(0xFFFF8A80);
            FrameLayout.LayoutParams deleteParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            deleteParams.bottomMargin = dp(context, 40);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = pager.getCurrentItem();
                    dialog.dismiss();
                    deleteListener.onDelete(index);
                }
            });
            root.addView(delete, deleteParams);
        }

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        int start = Math.max(0, Math.min(startIndex, photos.size() - 1));
        pager.setCurrentItem(start, false);
        counter.setText((start + 1) + " / " + photos.size());
        dialog.show();
    }

    private static TextView button(Context context, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(15);
        view.setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10));
        view.setBackgroundColor(0x66000000);
        view.setClickable(true);
        return view;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
