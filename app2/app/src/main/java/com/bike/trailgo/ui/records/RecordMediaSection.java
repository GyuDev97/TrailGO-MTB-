package com.bike.trailgo.ui.records;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bike.trailgo.R;
import com.bike.trailgo.data.GuestRideStoreCore;
import com.bike.trailgo.data.LocalRideStore;
import com.bike.trailgo.dto.RecordPhoto;
import com.bike.trailgo.dto.RecordWeather;
import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.net.ServerApi;
import com.bike.trailgo.ui.photos.PhotoImporter;
import com.bike.trailgo.ui.photos.PhotoStripView;
import com.bike.trailgo.ui.photos.PhotoViewerDialog;
import com.bike.trailgo.ui.photos.RidePhotoPicker;
import com.bike.trailgo.ui.photos.RidePhotoUploader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Memo, photos and weather of one saved ride on the record detail screen.
 *
 * <p>Member records are read and changed through the TrailGo server (only the owner can); guest
 * records through {@link LocalRideStore}. Whatever the source, a failed change leaves what was
 * shown before untouched and says so. Nothing here shows made-up content: no memo, no photos and
 * no weather simply render as an empty state (or as nothing for the weather).</p>
 */
final class RecordMediaSection {
    private final Fragment fragment;
    private final ServerApi api;
    private final String riderId;
    private final String recordNumber;
    private final boolean local;
    private final int localId;
    private final Handler main = new Handler(Looper.getMainLooper());

    private final TextView memoText;
    private final TextView memoEdit;
    private final TextView photoTitle;
    private final TextView photoEdit;
    private final TextView photoEmpty;
    private final PhotoStripView photoStrip;
    private final View weatherCard;
    private final ImageView weatherIcon;
    private final TextView weatherText;
    private final TextView weatherNote;

    private String memo;
    private final List<RecordPhoto> photos = new ArrayList<>();
    private boolean loaded;
    private boolean editingPhotos;
    private Call<?> pendingCall;
    private RidePhotoUploader uploader;
    private AlertDialog progressDialog;

    RecordMediaSection(Fragment fragment, View root, ServerApi api, String riderId, String recordNumber,
                       boolean local) {
        this.fragment = fragment;
        this.api = api;
        this.riderId = riderId == null ? "" : riderId;
        this.recordNumber = recordNumber;
        this.local = local;
        int parsed;
        try {
            parsed = Integer.parseInt(recordNumber);
        } catch (NumberFormatException e) {
            parsed = 0;
        }
        this.localId = parsed;

        memoText = root.findViewById(R.id.detail_memo_text);
        memoEdit = root.findViewById(R.id.detail_memo_edit);
        photoTitle = root.findViewById(R.id.detail_photo_title);
        photoEdit = root.findViewById(R.id.detail_photo_edit);
        photoEmpty = root.findViewById(R.id.detail_photo_empty);
        photoStrip = root.findViewById(R.id.detail_photo_strip);
        weatherCard = root.findViewById(R.id.detail_weather_card);
        weatherIcon = root.findViewById(R.id.detail_weather_icon);
        weatherText = root.findViewById(R.id.detail_weather);
        weatherNote = root.findViewById(R.id.detail_weather_note);

        memoEdit.setOnClickListener(v -> showMemoDialog());
        photoEdit.setOnClickListener(v -> {
            editingPhotos = !editingPhotos;
            renderPhotos();
        });
        photoStrip.setListener(new PhotoStripView.Listener() {
            @Override
            public void onAddClicked() {
                addPhotosClicked();
            }

            @Override
            public void onPhotoClicked(int index) {
                PhotoViewerDialog.show(context(), stripItems(), index, deleteIndex -> confirmDelete(deleteIndex));
            }

            @Override
            public void onRemoveClicked(int index) {
                confirmDelete(index);
            }
        });
        photoStrip.setAddEnabled(false);
    }

    // ---- loading ------------------------------------------------------------------------------

    void load() {
        if (local) {
            new Thread(() -> {
                final RidingRecord record = LocalRideStore.find(context().getApplicationContext(), localId);
                main.post(() -> {
                    if (alive()) {
                        render(record, true);
                    }
                });
            }).start();
            return;
        }
        if (riderId.isEmpty() || localId < 0) {
            renderUnavailable();
            return;
        }
        Call<RidingRecord> call = api.getMyRecordDetail(riderId, recordNumber);
        pendingCall = call;
        call.enqueue(new Callback<RidingRecord>() {
            @Override
            public void onResponse(Call<RidingRecord> call, Response<RidingRecord> response) {
                if (!alive()) {
                    return;
                }
                RidingRecord body = response.isSuccessful() ? response.body() : null;
                if (body == null) {
                    renderUnavailable();
                } else {
                    render(body, false);
                }
            }

            @Override
            public void onFailure(Call<RidingRecord> call, Throwable error) {
                if (!call.isCanceled() && alive()) {
                    Log.w("RecordMedia", "Record detail unavailable: " + error.getClass().getSimpleName());
                    renderUnavailable();
                }
            }
        });
    }

    /** The server (or local file) did not give the record: say so, and offer no editing. */
    private void renderUnavailable() {
        loaded = false;
        memoText.setText("메모를 불러오지 못했습니다.");
        memoEdit.setVisibility(View.GONE);
        photoTitle.setText("사진");
        photoEdit.setVisibility(View.GONE);
        photoEmpty.setText("사진을 불러오지 못했습니다.");
        photoEmpty.setVisibility(View.VISIBLE);
        photoStrip.setItems(new ArrayList<PhotoStripView.Item>());
        photoStrip.setAddEnabled(false);
        weatherCard.setVisibility(View.GONE);
    }

    private void render(RidingRecord record, boolean fromLocalStore) {
        if (record == null) {
            renderUnavailable();
            return;
        }
        loaded = true;
        memo = record.getRr_memo();
        photos.clear();
        photos.addAll(record.getPhotos());
        renderMemo();
        renderPhotos();
        renderWeather(record.getWeather());
    }

    // ---- weather ------------------------------------------------------------------------------

    private void renderWeather(RecordWeather weather) {
        String text = weather == null ? null
                : RecordFormatter.formatWeather(weather.isAvailable(), weather.getLabel(), weather.getTemperatureC());
        if (text == null) {
            weatherCard.setVisibility(View.GONE); // never a placeholder, never today's weather
            return;
        }
        weatherText.setText(text);
        weatherIcon.setImageResource(WeatherIcons.resource(weather));
        weatherNote.setText(RecordFormatter.formatWeatherNote(weather.getObservedAt(), weather.getPrecipitationMm()));
        weatherCard.setVisibility(View.VISIBLE);
    }

    // ---- memo ---------------------------------------------------------------------------------

    private void renderMemo() {
        boolean empty = memo == null || memo.trim().isEmpty();
        memoText.setText(empty ? "작성된 메모가 없습니다." : memo);
        memoText.setTextColor(empty ? 0xFF9A907F : 0xFF4A4033);
        memoEdit.setText(empty ? "✎ 작성" : "✎ 수정");
        memoEdit.setVisibility(View.VISIBLE);
    }

    private void showMemoDialog() {
        if (!loaded) {
            return;
        }
        Context context = context();
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        container.setPadding(pad, dp(8), pad, 0);

        final EditText input = new EditText(context);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMinLines(5);
        input.setMaxLines(10);
        input.setHint("라이딩 소감이나 코스 정보를 남겨 보세요.");
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(GuestRideStoreCore.MAX_MEMO_LENGTH)});
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(memo == null ? "" : memo);
        input.setSelection(input.getText().length());
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        final TextView counter = new TextView(context);
        counter.setGravity(Gravity.END);
        counter.setTextSize(12);
        counter.setTextColor(0xFF7A837D);
        counter.setText(input.getText().length() + "/" + GuestRideStoreCore.MAX_MEMO_LENGTH);
        container.addView(counter);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                counter.setText(s.length() + "/" + GuestRideStoreCore.MAX_MEMO_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        new AlertDialog.Builder(context)
                .setTitle("라이딩 메모")
                .setView(container)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", (dialog, which) -> saveMemo(input.getText().toString()))
                .show();
    }

    private void saveMemo(String text) {
        final String normalized = GuestRideStoreCore.normalizeMemo(text);
        if (local) {
            new Thread(() -> {
                boolean ok;
                try {
                    ok = LocalRideStore.updateMemo(context().getApplicationContext(), localId, normalized) != null;
                } catch (IOException error) {
                    Log.w("RecordMedia", "Memo not saved: " + error.getClass().getSimpleName());
                    ok = false;
                }
                final boolean saved = ok;
                main.post(() -> memoSaved(saved, normalized));
            }).start();
            return;
        }
        api.updateMemo(riderId, recordNumber, normalized == null ? "" : normalized)
                .enqueue(new Callback<Map<String, String>>() {
                    @Override
                    public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                        memoSaved(response.isSuccessful(), normalized);
                    }

                    @Override
                    public void onFailure(Call<Map<String, String>> call, Throwable error) {
                        memoSaved(false, normalized);
                    }
                });
    }

    private void memoSaved(boolean ok, String normalized) {
        if (!alive()) {
            return;
        }
        if (ok) {
            memo = normalized;
            renderMemo();
            toast("메모를 저장했습니다.");
        } else {
            // memo and the text on screen are unchanged
            toast("메모를 저장하지 못했습니다. 기존 메모가 유지됩니다.");
        }
    }

    // ---- photos -------------------------------------------------------------------------------

    private void renderPhotos() {
        photoTitle.setText("사진 (" + photos.size() + "장)");
        photoEdit.setVisibility(photos.isEmpty() ? View.GONE : View.VISIBLE);
        if (photos.isEmpty()) {
            editingPhotos = false;
        }
        photoEdit.setText(editingPhotos ? "완료" : "편집");
        photoEmpty.setText("저장된 사진이 없습니다.");
        photoEmpty.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
        photoStrip.setEditable(editingPhotos);
        photoStrip.setAddEnabled(loaded);
        photoStrip.setItems(stripItems());
    }

    private List<PhotoStripView.Item> stripItems() {
        List<PhotoStripView.Item> items = new ArrayList<>();
        for (RecordPhoto photo : photos) {
            if (photo.isLocal()) {
                File file = null;
                try {
                    file = LocalRideStore.photoFile(context().getApplicationContext(), photo.getFile());
                } catch (IOException ignored) {
                    // invalid name: shown as a placeholder
                }
                items.add(new PhotoStripView.Item(file, null, photo));
            } else {
                items.add(new PhotoStripView.Item(null, serverPhotoUrl(photo.getId()), photo));
            }
        }
        return items;
    }

    private String serverPhotoUrl(String photoId) {
        return Server.getUrl("app/riding/photos/" + photoId);
    }

    private void addPhotosClicked() {
        int remaining = RidePhotoPicker.MAX_PHOTOS - photos.size();
        if (remaining <= 0) {
            toast("사진은 최대 10장까지 추가할 수 있습니다.");
            return;
        }
        if (!RidePhotoPicker.launch(fragment, remaining)) {
            toast("사진을 선택할 수 있는 앱을 찾지 못했습니다.");
        }
    }

    /** Result of the system picker. Cancelling changes nothing. */
    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != RidePhotoPicker.REQUEST_CODE || resultCode != Activity.RESULT_OK || data == null || !alive()) {
            return;
        }
        int remaining = RidePhotoPicker.MAX_PHOTOS - photos.size();
        List<Uri> uris = RidePhotoPicker.parse(data, remaining);
        if (uris.isEmpty()) {
            return;
        }
        if (data.getClipData() != null && data.getClipData().getItemCount() > remaining) {
            toast("사진은 최대 10장까지 추가할 수 있어 일부만 추가합니다.");
        }
        final File draftDirectory = new File(context().getCacheDir(), "record-photo-drafts");
        PhotoImporter.importAsync(context(), uris, draftDirectory, (files, failed) -> {
            if (failed > 0 && alive()) {
                toast(failed + "장은 불러오지 못했습니다. 사진 접근 권한이나 파일을 확인해 주세요.");
            }
            if (files.isEmpty()) {
                return;
            }
            if (!alive()) {
                PhotoImporter.clearDirectory(draftDirectory);
                return;
            }
            if (local) {
                storeLocalPhotos(files, draftDirectory);
            } else {
                uploadPhotos(files, draftDirectory);
            }
        });
    }

    private void storeLocalPhotos(final List<File> files, final File draftDirectory) {
        new Thread(() -> {
            RidingRecord updated = null;
            boolean ok = true;
            try {
                updated = LocalRideStore.addPhotos(context().getApplicationContext(), localId, files);
            } catch (IOException error) {
                Log.w("RecordMedia", "Photos not stored: " + error.getClass().getSimpleName());
                ok = false;
            }
            PhotoImporter.clearDirectory(draftDirectory);
            final RidingRecord result = updated;
            final boolean success = ok && updated != null;
            main.post(() -> {
                if (!alive()) {
                    return;
                }
                if (success) {
                    render(result, true);
                    toast("사진을 추가했습니다.");
                } else {
                    toast("사진을 저장하지 못했습니다. 저장공간을 확인해 주세요.");
                }
            });
        }).start();
    }

    private void uploadPhotos(final List<File> files, final File draftDirectory) {
        dismissProgress();
        progressDialog = new AlertDialog.Builder(context())
                .setTitle("사진 업로드")
                .setMessage("사진을 올리는 중입니다 (0/" + files.size() + ")")
                .setCancelable(false)
                .create();
        progressDialog.show();
        uploader = new RidePhotoUploader(api, riderId, recordNumber, files, new RidePhotoUploader.Listener() {
            @Override
            public void onProgress(int done, int total) {
                if (progressDialog != null) {
                    progressDialog.setMessage("사진을 올리는 중입니다 (" + (done + 1) + "/" + total + ")");
                }
            }

            @Override
            public void onFinished(List<RecordPhoto> uploaded, List<File> failed, int lastFailureCode) {
                dismissProgress();
                PhotoImporter.clearDirectory(draftDirectory);
                if (!alive()) {
                    return;
                }
                if (!uploaded.isEmpty()) {
                    load(); // show what the server really stored
                }
                if (failed.isEmpty()) {
                    toast("사진을 추가했습니다.");
                } else {
                    toast("사진 " + failed.size() + "장을 올리지 못했습니다. "
                            + RidePhotoUploader.describeFailure(lastFailureCode));
                }
            }
        });
        uploader.start();
    }

    private void confirmDelete(final int index) {
        if (index < 0 || index >= photos.size()) {
            return;
        }
        final RecordPhoto photo = photos.get(index);
        new AlertDialog.Builder(context())
                .setTitle("사진 삭제")
                .setMessage("이 사진을 삭제할까요? 되돌릴 수 없습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> deletePhoto(photo))
                .show();
    }

    private void deletePhoto(final RecordPhoto photo) {
        if (local) {
            new Thread(() -> {
                RidingRecord updated = null;
                try {
                    updated = LocalRideStore.removePhoto(context().getApplicationContext(), localId, photo.getFile());
                } catch (IOException error) {
                    Log.w("RecordMedia", "Photo not removed: " + error.getClass().getSimpleName());
                }
                final RidingRecord result = updated;
                main.post(() -> {
                    if (!alive()) {
                        return;
                    }
                    if (result != null) {
                        render(result, true);
                    } else {
                        toast("사진을 삭제하지 못했습니다.");
                    }
                });
            }).start();
            return;
        }
        api.deletePhoto(riderId, recordNumber, photo.getId()).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (!alive()) {
                    return;
                }
                if (response.isSuccessful() || response.code() == 404) {
                    // 404: already gone on the server, so the screen should not keep showing it either
                    photos.remove(photo);
                    renderPhotos();
                } else {
                    toast("사진을 삭제하지 못했습니다. 잠시 후 다시 시도해 주세요.");
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable error) {
                if (alive()) {
                    toast("네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
                }
            }
        });
    }

    // ---- lifecycle / helpers ------------------------------------------------------------------

    void destroy() {
        if (pendingCall != null) {
            pendingCall.cancel();
        }
        if (uploader != null) {
            uploader.cancel();
        }
        dismissProgress();
    }

    private void dismissProgress() {
        if (progressDialog != null) {
            try {
                progressDialog.dismiss();
            } catch (IllegalArgumentException ignored) {
                // window already gone
            }
            progressDialog = null;
        }
    }

    private boolean alive() {
        return fragment.isAdded() && fragment.getView() != null;
    }

    private Context context() {
        return fragment.requireContext();
    }

    private void toast(String message) {
        if (alive()) {
            Toast.makeText(context(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        return Math.round(value * context().getResources().getDisplayMetrics().density);
    }
}
