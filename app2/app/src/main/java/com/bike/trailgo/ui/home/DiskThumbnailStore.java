package com.bike.trailgo.ui.home;

import androidx.annotation.Nullable;

import com.bike.trailgo.data.AtomicFiles;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Small on-disk cache of rendered route pictures (PNG bytes), bounded by file count, total size
 * and age. Files are named {@code <refHash>_<w>x<h>.png}: all pictures of one GPX reference share
 * a prefix, so they can be removed together when the record is deleted. Pure java.io.
 */
public final class DiskThumbnailStore {
    private final File directory;
    private final int maxFiles;
    private final long maxBytes;
    private final long maxAgeMillis;

    public DiskThumbnailStore(File directory, int maxFiles, long maxBytes, long maxAgeMillis) {
        this.directory = directory;
        this.maxFiles = maxFiles;
        this.maxBytes = maxBytes;
        this.maxAgeMillis = maxAgeMillis;
    }

    /** Stable short id of a GPX reference (file name or "local:ride-N.gpx"). */
    public static String refHash(String gpxReference) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(gpxReference.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                hex.append(String.format("%02x", bytes[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException error) {
            throw new IllegalStateException(error);
        }
    }

    public static String key(String gpxReference, int widthPx, int heightPx) {
        return refHash(gpxReference) + "_" + widthPx + "x" + heightPx;
    }

    @Nullable
    public synchronized byte[] get(String key, long nowMillis) {
        File file = fileFor(key);
        if (!file.isFile()) {
            return null;
        }
        if (nowMillis - file.lastModified() > maxAgeMillis) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            return null;
        }
        try {
            byte[] bytes = AtomicFiles.read(file);
            //noinspection ResultOfMethodCallIgnored
            file.setLastModified(nowMillis); // recently used
            return bytes;
        } catch (IOException error) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            return null;
        }
    }

    public synchronized void put(String key, byte[] bytes, long nowMillis) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create the thumbnail folder");
        }
        File file = fileFor(key);
        AtomicFiles.write(file, bytes);
        //noinspection ResultOfMethodCallIgnored
        file.setLastModified(nowMillis);
        prune(nowMillis);
    }

    /** Removes every picture of one GPX reference (the record was deleted). */
    public synchronized int removeReference(String gpxReference) {
        String prefix = refHash(gpxReference) + "_";
        int removed = 0;
        for (File file : files()) {
            if (file.getName().startsWith(prefix) && file.delete()) {
                removed++;
            }
        }
        return removed;
    }

    /** Keeps only pictures of the given references (everything else belongs to records that are gone). */
    public synchronized int retainOnly(Set<String> gpxReferences) {
        List<String> prefixes = new ArrayList<>();
        for (String reference : gpxReferences) {
            prefixes.add(refHash(reference) + "_");
        }
        int removed = 0;
        for (File file : files()) {
            boolean keep = false;
            for (String prefix : prefixes) {
                if (file.getName().startsWith(prefix)) {
                    keep = true;
                    break;
                }
            }
            if (!keep && file.delete()) {
                removed++;
            }
        }
        return removed;
    }

    /** Drops too old pictures, then the least recently used until count and size are within limits. */
    public synchronized int prune(long nowMillis) {
        List<File> files = new ArrayList<>(Arrays.asList(files()));
        int removed = 0;
        for (File file : new ArrayList<>(files)) {
            if (nowMillis - file.lastModified() > maxAgeMillis && file.delete()) {
                files.remove(file);
                removed++;
            }
        }
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(a.lastModified(), b.lastModified());
            }
        });
        long total = 0;
        for (File file : files) {
            total += file.length();
        }
        while (!files.isEmpty() && (files.size() > maxFiles || total > maxBytes)) {
            File oldest = files.remove(0);
            total -= oldest.length();
            if (oldest.delete()) {
                removed++;
            }
        }
        return removed;
    }

    public synchronized int count() {
        return files().length;
    }

    private File[] files() {
        File[] files = directory.listFiles();
        if (files == null) {
            return new File[0];
        }
        List<File> pngs = new ArrayList<>();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".png")) {
                pngs.add(file);
            }
        }
        return pngs.toArray(new File[0]);
    }

    private File fileFor(String key) {
        if (!key.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid thumbnail key");
        }
        return new File(directory, key + ".png");
    }
}
