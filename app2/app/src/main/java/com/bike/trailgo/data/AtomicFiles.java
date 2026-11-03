package com.bike.trailgo.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Crash-safe file writes using only java.io (so they also run in JVM unit tests): data goes to a
 * sibling temp file, is synced, and only then replaces the target. A failure leaves the previous
 * target untouched.
 */
public final class AtomicFiles {
    private AtomicFiles() {
    }

    public static void write(File target, byte[] bytes) throws IOException {
        File temp = tempFor(target);
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(bytes);
            output.flush();
            output.getFD().sync();
        } catch (IOException error) {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
            throw error;
        }
        replace(temp, target);
    }

    public static void copy(File source, File target) throws IOException {
        File temp = tempFor(target);
        try (InputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(temp)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
            output.getFD().sync();
        } catch (IOException error) {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
            throw error;
        }
        replace(temp, target);
    }

    public static byte[] read(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int offset = 0;
            while (offset < bytes.length) {
                int read = input.read(bytes, offset, bytes.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            return offset == bytes.length ? bytes : java.util.Arrays.copyOf(bytes, offset);
        }
    }

    private static File tempFor(File target) {
        return new File(target.getParentFile(), target.getName() + ".tmp");
    }

    private static void replace(File temp, File target) throws IOException {
        // renameTo replaces the target atomically on Android/Linux; the fallback covers platforms
        // (e.g. Windows JVM tests) that refuse to rename over an existing file.
        if (temp.renameTo(target)) {
            return;
        }
        if (target.exists() && !target.delete()) {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
            throw new IOException("Unable to replace " + target.getName());
        }
        if (!temp.renameTo(target)) {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
            throw new IOException("Unable to move " + temp.getName() + " into place");
        }
    }
}
