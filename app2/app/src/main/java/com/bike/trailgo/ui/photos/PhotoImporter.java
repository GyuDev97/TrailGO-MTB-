package com.bike.trailgo.ui.photos;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Prepares picked photos off the main thread; the callback runs on the main thread. */
public final class PhotoImporter {
    public interface Callback {
        /** @param failed how many of the picked images could not be read */
        void onImported(List<File> files, int failed);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private PhotoImporter() {
    }

    public static void importAsync(final Context context, final List<Uri> uris, final File outputDirectory,
                                   final Callback callback) {
        final Context appContext = context.getApplicationContext();
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                final List<File> files = new ArrayList<>();
                int failures = 0;
                for (Uri uri : uris) {
                    try {
                        files.add(PhotoImageProcessor.prepare(appContext, uri, outputDirectory));
                    } catch (Exception error) {
                        failures++;
                        Log.w("PhotoImporter", "Skipping an unreadable photo: " + error.getClass().getSimpleName());
                    }
                }
                final int failed = failures;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onImported(files, failed);
                    }
                });
            }
        });
    }

    /** Deletes every prepared-but-unsaved photo in the folder. */
    public static void clearDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
