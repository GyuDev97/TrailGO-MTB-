package com.bike.trailgo.dto;

import androidx.annotation.Nullable;

/**
 * A photo attached to a riding record.
 *
 * <p>Member records: {@code id} is the server's token and the image is loaded from the server.
 * Guest records: {@code file} is the file name inside the app's private guest-ride photo folder
 * (and {@code id} repeats it). Photo bytes are never stored in JSON or the database.</p>
 */
public class RecordPhoto {
    private String id;
    private int sortOrder;
    private String file;

    public RecordPhoto() {
    }

    public RecordPhoto(String id, int sortOrder, @Nullable String file) {
        this.id = id;
        this.sortOrder = sortOrder;
        this.file = file;
    }

    public String getId() {
        return id;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    /** Local file name for guest records, null for server photos. */
    @Nullable
    public String getFile() {
        return file;
    }

    public boolean isLocal() {
        return file != null;
    }
}
