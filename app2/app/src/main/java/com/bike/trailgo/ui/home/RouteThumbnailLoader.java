package com.bike.trailgo.ui.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.Nullable;

import com.bike.trailgo.data.LocalRideStore;
import com.bike.trailgo.gpx.GpxTrackPoints;
import com.bike.trailgo.net.Server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Route pictures for the ride list, from the GPX of each record.
 *
 * <p>Scrolling never parses a GPX: a picture is looked up in memory, then on disk, and only when
 * both miss is it made once, on a background thread (read GPX, project, draw, PNG to disk). Requests
 * for the same picture while one is running share it, failures are remembered for a while, and
 * downloads are size-limited. One Kakao MapView per row is never created.</p>
 */
public final class RouteThumbnailLoader {

    public enum Result { READY, NO_GPX, FAILED }

    public interface Callback {
        /** Called on the main thread; {@code bitmap} is non-null only for {@link Result#READY}. */
        void onResult(@Nullable Bitmap bitmap, Result result);
    }

    private static final String TAG = "RouteThumbs";
    private static final long NEGATIVE_TTL_MS = 2L * 60L * 1000L;
    private static final long MAX_GPX_BYTES = 8L * 1024L * 1024L;
    private static final int DISK_MAX_FILES = 120;
    private static final long DISK_MAX_BYTES = 12L * 1024L * 1024L;
    private static final long DISK_MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L;

    private static RouteThumbnailLoader instance;

    public static synchronized RouteThumbnailLoader get(Context context) {
        if (instance == null) {
            instance = new RouteThumbnailLoader(context.getApplicationContext());
        }
        return instance;
    }

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final DiskThumbnailStore disk;
    private final LruCache<String, Bitmap> memory;
    private final ThreadPoolExecutor executor;
    // main thread only
    private final Map<String, List<Callback>> inFlight = new HashMap<>();
    private final Map<String, Long> failedAt = new HashMap<>();
    private final Map<String, Result> failedResult = new HashMap<>();

    private RouteThumbnailLoader(Context context) {
        this.context = context;
        this.disk = new DiskThumbnailStore(new File(context.getCacheDir(), "route-thumbs"),
                DISK_MAX_FILES, DISK_MAX_BYTES, DISK_MAX_AGE_MS);
        int memoryBytes = (int) Math.min(6L * 1024L * 1024L, Runtime.getRuntime().maxMemory() / 16L);
        this.memory = new LruCache<String, Bitmap>(memoryBytes) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        this.executor = new ThreadPoolExecutor(1, 1, 30L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(64), new ThreadPoolExecutor.DiscardOldestPolicy());
        this.executor.allowCoreThreadTimeOut(true);
    }

    /** Must be called on the main thread. */
    public void load(@Nullable final String gpxReference, final int widthPx, final int heightPx, final float density,
                     final Callback callback) {
        if (gpxReference == null || gpxReference.trim().isEmpty() || "null".equalsIgnoreCase(gpxReference.trim())) {
            callback.onResult(null, Result.NO_GPX);
            return;
        }
        final String key = DiskThumbnailStore.key(gpxReference, widthPx, heightPx);
        Bitmap cached = memory.get(key);
        if (cached != null) {
            callback.onResult(cached, Result.READY);
            return;
        }
        Long failed = failedAt.get(key);
        if (failed != null && System.currentTimeMillis() - failed < NEGATIVE_TTL_MS) {
            callback.onResult(null, failedResult.get(key));
            return;
        }
        List<Callback> waiting = inFlight.get(key);
        if (waiting != null) {
            waiting.add(callback); // the same picture is already being made
            return;
        }
        waiting = new ArrayList<>();
        waiting.add(callback);
        inFlight.put(key, waiting);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                Result result;
                try {
                    bitmap = loadOrCreate(gpxReference, key, widthPx, heightPx, density);
                    result = bitmap == null ? Result.NO_GPX : Result.READY;
                } catch (Exception error) {
                    Log.w(TAG, "No route picture: " + error.getClass().getSimpleName());
                    result = Result.FAILED;
                }
                final Bitmap finalBitmap = bitmap;
                final Result finalResult = result;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalBitmap != null) {
                            memory.put(key, finalBitmap);
                            failedAt.remove(key);
                        } else {
                            failedAt.put(key, System.currentTimeMillis());
                            failedResult.put(key, finalResult);
                        }
                        List<Callback> callbacks = inFlight.remove(key);
                        if (callbacks != null) {
                            for (Callback waiter : callbacks) {
                                waiter.onResult(finalBitmap, finalResult);
                            }
                        }
                    }
                });
            }
        });
    }

    /** The record was deleted: forget its pictures in memory and on disk. */
    public void invalidate(final String gpxReference) {
        if (gpxReference == null) {
            return;
        }
        final String prefix = DiskThumbnailStore.refHash(gpxReference) + "_";
        for (String key : new ArrayList<>(memory.snapshot().keySet())) {
            if (key.startsWith(prefix)) {
                memory.remove(key);
            }
        }
        main.post(new Runnable() {
            @Override
            public void run() {
                failedAt.clear(); // maps are touched on the main thread only
                failedResult.clear();
            }
        });
        executor.execute(new Runnable() {
            @Override
            public void run() {
                disk.removeReference(gpxReference);
            }
        });
    }

    /** Keep only the pictures of records that still exist (guest records are all known locally). */
    public void retainOnly(final Set<String> gpxReferences) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                disk.retainOnly(gpxReferences);
                disk.prune(System.currentTimeMillis());
            }
        });
    }

    @Nullable
    private Bitmap loadOrCreate(String gpxReference, String key, int widthPx, int heightPx, float density) throws Exception {
        byte[] stored = disk.get(key, System.currentTimeMillis());
        if (stored != null) {
            Bitmap decoded = BitmapFactory.decodeByteArray(stored, 0, stored.length);
            if (decoded != null) {
                return decoded;
            }
        }
        List<double[]> points;
        InputStream input = null;
        HttpURLConnection connection = null;
        try {
            if (LocalRideStore.isLocalGpxReference(gpxReference)) {
                input = LocalRideStore.openGpx(context, gpxReference);
            } else {
                connection = Server.openConnection("app/getGPX/gpx/" + gpxReference);
                if (connection.getResponseCode() != 200) {
                    throw new IOException("GPX download failed: HTTP " + connection.getResponseCode());
                }
                input = connection.getInputStream();
            }
            points = GpxTrackPoints.read(new LimitedInputStream(input, MAX_GPX_BYTES), Xml.newPullParser());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                    // nothing to do
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        Bitmap bitmap = RouteThumbnailRenderer.render(points, widthPx, heightPx, density);
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, png);
        try {
            disk.put(key, png.toByteArray(), System.currentTimeMillis());
        } catch (IOException error) {
            Log.w(TAG, "Route picture not cached: " + error.getClass().getSimpleName());
        }
        return bitmap;
    }

    /** Stops reading after {@code limit} bytes so a broken or huge file cannot fill memory. */
    private static final class LimitedInputStream extends FilterInputStream {
        private long remaining;

        LimitedInputStream(InputStream in, long limit) {
            super(in);
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                throw new IOException("GPX is too large");
            }
            int value = super.read();
            if (value >= 0) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining <= 0) {
                throw new IOException("GPX is too large");
            }
            int read = super.read(buffer, offset, (int) Math.min(length, remaining));
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }
    }
}
