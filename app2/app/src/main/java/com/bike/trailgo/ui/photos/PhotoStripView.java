package com.bike.trailgo.ui.photos;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bike.trailgo.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A horizontal row of photo thumbnails with an optional "add" cell and (in edit mode) a remove
 * button on each thumbnail. Thumbnails are decoded at thumbnail size by Picasso (memory cached),
 * so ten photos do not mean ten full-size bitmaps.
 */
public class PhotoStripView extends HorizontalScrollView {

    /** One photo: either a local file or a URL (server). {@code tag} is handed back to listeners. */
    public static final class Item {
        @Nullable
        public final File file;
        @Nullable
        public final String url;
        public final Object tag;

        public Item(@Nullable File file, @Nullable String url, Object tag) {
            this.file = file;
            this.url = url;
            this.tag = tag;
        }
    }

    public interface Listener {
        void onAddClicked();

        void onPhotoClicked(int index);

        void onRemoveClicked(int index);
    }

    private static final int THUMB_DP = 84;

    private final LinearLayout row;
    private final List<Item> items = new ArrayList<>();
    private Listener listener;
    private boolean editable;
    private boolean addEnabled = true;
    private int maxPhotos = RidePhotoPicker.MAX_PHOTOS;

    public PhotoStripView(Context context) {
        this(context, null);
    }

    public PhotoStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        addView(row, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        render();
    }

    public void setAddEnabled(boolean addEnabled) {
        this.addEnabled = addEnabled;
        render();
    }

    public void setMaxPhotos(int maxPhotos) {
        this.maxPhotos = maxPhotos;
        render();
    }

    public void setItems(List<Item> newItems) {
        items.clear();
        items.addAll(newItems);
        render();
    }

    public int getCount() {
        return items.size();
    }

    private void render() {
        row.removeAllViews();
        for (int i = 0; i < items.size(); i++) {
            row.addView(createThumb(i, items.get(i)));
        }
        if (addEnabled && items.size() < maxPhotos) {
            row.addView(createAddCell());
        }
    }

    private View createThumb(final int index, Item item) {
        FrameLayout cell = new FrameLayout(getContext());
        cell.setLayoutParams(cellParams());

        ImageView image = new ImageView(getContext());
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundResource(R.drawable.riding_photo_thumb_bg);
        image.setContentDescription("라이딩 사진 " + (index + 1));
        roundCorners(image);
        cell.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        int size = dp(THUMB_DP);
        if (item.file != null) {
            Picasso.get().load(item.file).resize(size * 2, size * 2).onlyScaleDown().centerCrop()
                    .error(R.drawable.riding_photo_thumb_bg).into(image);
        } else if (item.url != null) {
            Picasso.get().load(item.url).resize(size * 2, size * 2).onlyScaleDown().centerCrop()
                    .error(R.drawable.riding_photo_thumb_bg).into(image);
        }
        image.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onPhotoClicked(index);
                }
            }
        });

        if (editable) {
            TextView remove = new TextView(getContext());
            remove.setText("✕");
            remove.setTextColor(0xFFFFFFFF);
            remove.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            remove.setGravity(Gravity.CENTER);
            remove.setBackgroundResource(R.drawable.riding_photo_remove_bg);
            remove.setContentDescription("사진 " + (index + 1) + " 삭제");
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.END | Gravity.TOP);
            params.setMargins(0, dp(4), dp(4), 0);
            remove.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onRemoveClicked(index);
                    }
                }
            });
            cell.addView(remove, params);
        }
        return cell;
    }

    private View createAddCell() {
        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setBackgroundResource(R.drawable.riding_photo_add_bg);
        cell.setLayoutParams(cellParams());
        cell.setContentDescription("사진 추가");

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(R.drawable.ic_photo_camera);
        cell.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView label = new TextView(getContext());
        label.setText("사진 추가");
        label.setTextColor(0xFF59615C);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setPadding(0, dp(4), 0, 0);
        cell.addView(label);
        cell.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onAddClicked();
                }
            }
        });
        return cell;
    }

    private LinearLayout.LayoutParams cellParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(THUMB_DP), dp(THUMB_DP));
        params.setMargins(0, 0, dp(8), 0);
        return params;
    }

    private void roundCorners(View view) {
        final float radius = dp(10);
        view.setClipToOutline(true);
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
                outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }
}
