package com.bike.trailgo.ui.photos;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Opens the system photo picker (no storage permission needed) and reads its result.
 * Android 13+ uses the picker proper; older versions use the document picker with multi-select.
 */
public final class RidePhotoPicker {
    public static final int MAX_PHOTOS = 10;
    public static final int REQUEST_CODE = 4201;

    private RidePhotoPicker() {
    }

    static Intent[] candidates(int remaining) {
        int limit = Math.max(1, Math.min(remaining, MAX_PHOTOS));
        List<Intent> intents = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            Intent picker = new Intent(MediaStore.ACTION_PICK_IMAGES);
            if (limit > 1) {
                int platformMax = MediaStore.getPickImagesMaxLimit();
                picker.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, Math.min(limit, platformMax));
            }
            intents.add(picker);
        }
        Intent documents = new Intent(Intent.ACTION_GET_CONTENT);
        documents.setType("image/*");
        documents.addCategory(Intent.CATEGORY_OPENABLE);
        documents.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, limit > 1);
        intents.add(documents);
        return intents.toArray(new Intent[0]);
    }

    /** @return false when no picker app could be started */
    public static boolean launch(Activity activity, int remaining) {
        for (Intent intent : candidates(remaining)) {
            try {
                activity.startActivityForResult(intent, REQUEST_CODE);
                return true;
            } catch (ActivityNotFoundException ignored) {
                // try the next picker
            }
        }
        return false;
    }

    public static boolean launch(Fragment fragment, int remaining) {
        for (Intent intent : candidates(remaining)) {
            try {
                fragment.startActivityForResult(intent, REQUEST_CODE);
                return true;
            } catch (ActivityNotFoundException ignored) {
                // try the next picker
            }
        }
        return false;
    }

    /** The picked images, at most {@code limit}. Empty when the user cancelled. */
    public static List<Uri> parse(Intent data, int limit) {
        List<Uri> uris = new ArrayList<>();
        if (data == null || limit <= 0) {
            return uris;
        }
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount() && uris.size() < limit; i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null) {
                    uris.add(uri);
                }
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        return uris;
    }
}
