package com.bike.trailgo.ui.photos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Turns a picked image (any Content URI) into a small private JPEG file. Doing this right away
 * means the app never depends on the temporary URI permission again, and neither uploads nor
 * thumbnails ever need the multi-megapixel original in memory. Call from a background thread.
 */
public final class PhotoImageProcessor {
    private PhotoImageProcessor() {
    }

    public static File prepare(Context context, Uri source, File outputDirectory) throws IOException {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IOException("Unable to create the photo folder");
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = open(context, source)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Not a readable image");
        }

        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = PhotoSizing.sampleSize(bounds.outWidth, bounds.outHeight, PhotoSizing.MAX_EDGE);
        Bitmap bitmap;
        try (InputStream input = open(context, source)) {
            bitmap = BitmapFactory.decodeStream(input, null, decode);
        }
        if (bitmap == null) {
            throw new IOException("Unable to decode the image");
        }

        Bitmap result = bitmap;
        try {
            int[] size = PhotoSizing.scaledSize(bitmap.getWidth(), bitmap.getHeight(), PhotoSizing.MAX_EDGE);
            if (size[0] != bitmap.getWidth() || size[1] != bitmap.getHeight()) {
                result = Bitmap.createScaledBitmap(bitmap, size[0], size[1], true);
            }
            int rotation = readRotation(context, source);
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotated = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, true);
                if (rotated != result) {
                    if (result != bitmap) {
                        result.recycle();
                    }
                    result = rotated;
                }
            }

            File target = new File(outputDirectory, "photo-" + UUID.randomUUID().toString().replace("-", "") + ".jpg");
            try (FileOutputStream output = new FileOutputStream(target)) {
                if (!result.compress(Bitmap.CompressFormat.JPEG, PhotoSizing.JPEG_QUALITY, output)) {
                    throw new IOException("Unable to compress the image");
                }
                output.flush();
                output.getFD().sync();
            } catch (IOException error) {
                //noinspection ResultOfMethodCallIgnored
                target.delete();
                throw error;
            }
            return target;
        } finally {
            if (result != bitmap) {
                result.recycle();
            }
            bitmap.recycle();
        }
    }

    private static InputStream open(Context context, Uri uri) throws IOException {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) {
                throw new IOException("Unable to open the image");
            }
            return input;
        } catch (SecurityException error) {
            // The picker's temporary permission is gone, or access was withdrawn.
            throw new IOException("No permission to read the image", error);
        }
    }

    private static int readRotation(Context context, Uri uri) {
        try (InputStream input = open(context, uri)) {
            ExifInterface exif = new ExifInterface(input);
            return PhotoSizing.rotationDegrees(
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
        } catch (IOException | RuntimeException ignored) {
            return 0;
        }
    }
}
