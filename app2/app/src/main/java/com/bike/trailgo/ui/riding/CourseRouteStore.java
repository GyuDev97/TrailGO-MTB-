package com.bike.trailgo.ui.riding;

import android.content.Context;
import android.util.AtomicFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/** Private GPX cache for a course that has already been opened successfully. */
public final class CourseRouteStore {
    private static final String DIRECTORY = "course-routes";
    private static final String LOCAL_PREFIX = "local:";
    private static final long MAX_IMPORTED_GPX_BYTES = 30L * 1024L * 1024L;

    private CourseRouteStore() {
    }

    static File get(Context context, String courseGpxName) {
        if (isLocalReference(courseGpxName)) {
            String importedName = courseGpxName.substring(LOCAL_PREFIX.length());
            if (!importedName.matches("import-[A-Za-z0-9._-]+\\.gpx")) {
                return new File(directory(context), "invalid.gpx");
            }
            return new File(directory(context), importedName);
        }
        return new File(directory(context), "course-" + Integer.toHexString(courseGpxName.hashCode()) + ".gpx");
    }

    public static boolean exists(Context context, String courseGpxName) {
        File file = get(context, courseGpxName);
        return file.isFile() && file.length() > 0L;
    }

    public static InputStream open(Context context, String courseGpxName) throws IOException {
        File file = get(context, courseGpxName);
        if (!file.isFile() || file.length() == 0L) throw new IOException("Cached course GPX not found");
        return new FileInputStream(file);
    }

    static void save(Context context, String courseGpxName, InputStream input) throws IOException {
        File directory = directory(context);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create course route cache directory");
        }
        AtomicFile destination = new AtomicFile(get(context, courseGpxName));
        FileOutputStream output = null;
        try {
            output = destination.startWrite();
            byte[] buffer = new byte[8192];
            int read;
            long total = 0L;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                total += read;
            }
            if (total == 0L) throw new IOException("Empty course GPX response");
            output.getFD().sync();
            destination.finishWrite(output);
        } catch (IOException error) {
            if (output != null) destination.failWrite(output);
            throw error;
        }
    }

    /**
     * Copies a document selected through the Storage Access Framework into the app-private cache.
     * The returned reference is safe to persist in a ride session and does not depend on the source URI.
     */
    public static String importFrom(Context context, String displayName, InputStream input) throws IOException {
        String safeName = sanitizeName(displayName);
        String reference = LOCAL_PREFIX + "import-" + UUID.randomUUID() + "-" + safeName + ".gpx";
        File destinationFile = get(context, reference);
        File directory = directory(context);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create course route cache directory");
        }

        AtomicFile destination = new AtomicFile(destinationFile);
        FileOutputStream output = null;
        try {
            output = destination.startWrite();
            byte[] buffer = new byte[8192];
            int read;
            long total = 0L;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_IMPORTED_GPX_BYTES) {
                    throw new IOException("GPX file is larger than 30 MB");
                }
                output.write(buffer, 0, read);
            }
            if (total == 0L) throw new IOException("Empty GPX file");
            output.getFD().sync();
            destination.finishWrite(output);
            return reference;
        } catch (IOException error) {
            if (output != null) destination.failWrite(output);
            throw error;
        }
    }

    static boolean isLocalReference(String reference) {
        return reference != null && reference.startsWith(LOCAL_PREFIX);
    }

    public static void deleteImported(Context context, String reference) {
        if (!isLocalReference(reference)) return;
        File file = get(context, reference);
        if (file.isFile()) file.delete();
    }

    private static String sanitizeName(String displayName) {
        String source = displayName == null ? "course" : displayName.trim();
        if (source.toLowerCase(java.util.Locale.US).endsWith(".gpx")) {
            source = source.substring(0, source.length() - 4);
        }
        source = source.replaceAll("[^A-Za-z0-9._-]", "_");
        if (source.isEmpty()) source = "course";
        return source.length() > 48 ? source.substring(0, 48) : source;
    }

    private static File directory(Context context) {
        return new File(context.getApplicationContext().getFilesDir(), DIRECTORY);
    }
}
