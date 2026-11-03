package com.bike.trailgo.ui.photos;

import android.util.Log;

import com.bike.trailgo.dto.RecordPhoto;
import com.bike.trailgo.net.ServerApi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Uploads prepared photos of a saved member record one after another. A failed photo never
 * blocks the others and never affects the record itself; the caller gets the photos that did not
 * make it (to retry or to tell the rider). Callbacks arrive on the main thread.
 */
public final class RidePhotoUploader {
    public interface Listener {
        void onProgress(int done, int total);

        /** @param uploaded the photos the server accepted, @param failed the files to retry or report */
        void onFinished(List<RecordPhoto> uploaded, List<File> failed, int lastFailureCode);
    }

    private final ServerApi api;
    private final String riderId;
    private final String rrNum;
    private final List<File> files;
    private final Listener listener;
    private final List<RecordPhoto> uploaded = new ArrayList<>();
    private final List<File> failed = new ArrayList<>();
    private int index;
    private int lastFailureCode;
    private Call<RecordPhoto> current;
    private boolean cancelled;

    public RidePhotoUploader(ServerApi api, String riderId, String rrNum, List<File> files, Listener listener) {
        this.api = api;
        this.riderId = riderId;
        this.rrNum = rrNum;
        this.files = new ArrayList<>(files);
        this.listener = listener;
    }

    public void start() {
        next();
    }

    public void cancel() {
        cancelled = true;
        if (current != null) {
            current.cancel();
        }
    }

    private void next() {
        if (cancelled) {
            return;
        }
        if (index >= files.size()) {
            listener.onFinished(uploaded, failed, lastFailureCode);
            return;
        }
        listener.onProgress(index, files.size());
        final File file = files.get(index);
        if (!file.isFile()) {
            failed.add(file);
            index++;
            next();
            return;
        }
        RequestBody body = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("photo", file.getName(), body);
        current = api.uploadPhoto(riderId, rrNum, part);
        current.enqueue(new Callback<RecordPhoto>() {
            @Override
            public void onResponse(Call<RecordPhoto> call, Response<RecordPhoto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    uploaded.add(response.body());
                } else {
                    lastFailureCode = response.code();
                    failed.add(file);
                    Log.w("RidePhotoUpload", "Photo rejected: HTTP " + response.code());
                    if (response.code() == 409) {
                        // The record already has the maximum number of photos: the rest cannot succeed.
                        for (int rest = index + 1; rest < files.size(); rest++) {
                            failed.add(files.get(rest));
                        }
                        index = files.size();
                        listener.onFinished(uploaded, failed, lastFailureCode);
                        return;
                    }
                }
                index++;
                next();
            }

            @Override
            public void onFailure(Call<RecordPhoto> call, Throwable error) {
                if (call.isCanceled()) {
                    return;
                }
                lastFailureCode = -1;
                failed.add(file);
                Log.w("RidePhotoUpload", "Photo upload failed: " + error.getClass().getSimpleName());
                index++;
                next();
            }
        });
    }

    /** User-facing reason for a failed upload. */
    public static String describeFailure(int code) {
        switch (code) {
            case 409:
                return "사진은 기록당 최대 10장까지 올릴 수 있습니다.";
            case 413:
                return "사진 용량이 너무 큽니다.";
            case 400:
                return "지원하지 않는 사진 형식입니다.";
            case 404:
                return "기록을 찾을 수 없어 사진을 올리지 못했습니다.";
            case -1:
                return "네트워크 연결을 확인해 주세요.";
            default:
                return "서버 오류로 사진을 올리지 못했습니다.";
        }
    }
}
