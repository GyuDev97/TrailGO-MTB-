package com.bike.trailgo.ui.riding;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;

import com.bike.trailgo.dto.Competition_Status;
import com.bike.trailgo.dto.Mission;
import com.bike.trailgo.dto.Mission_Status;
import com.bike.trailgo.gpx.GpxInfo;
import com.bike.trailgo.R;
import com.bike.trailgo.SendGPXFile;
import com.bike.trailgo.databinding.FragmentRidingEndBinding;
import com.bike.trailgo.data.LocalRideStore;
import com.bike.trailgo.dto.RidingRecord;
import android.text.Editable;
import android.text.TextWatcher;
import android.net.Uri;
import com.bike.trailgo.data.GuestRideStoreCore;
import com.bike.trailgo.dto.RecordPhoto;
import com.bike.trailgo.dto.RecordWeather;
import com.bike.trailgo.ui.photos.PhotoImporter;
import com.bike.trailgo.ui.photos.PhotoStripView;
import com.bike.trailgo.ui.photos.PhotoViewerDialog;
import com.bike.trailgo.ui.photos.RidePhotoPicker;
import com.bike.trailgo.ui.photos.RidePhotoUploader;
import com.bike.trailgo.ui.records.RecordFormatter;
import com.bike.trailgo.ui.records.WeatherIcons;
import java.util.Locale;
import java.util.TimeZone;
import com.bike.trailgo.dto.Tag_Status;
import com.bike.trailgo.net.HttpClient;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.net.ServerApi;
import com.bike.trailgo.setting.RideSettings;
import com.bike.trailgo.setting.AccountSession;
import com.bike.trailgo.ui.records.ElevationChartBinder;
import com.bike.trailgo.ui.records.ElevationProfile;
import com.github.mikephil.charting.charts.LineChart;
import com.google.gson.Gson;

import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;

import org.json.JSONArray;
import org.json.JSONObject;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EndActivity extends AppCompatActivity {
    private static final String STATE_PHOTOS = "draft_photos";
    private static final String STATE_GPX_STAMP = "gpx_stamp";

    private FragmentRidingEndBinding binding;

    private ServerApi serverApi = Server.getInstance().getApi();

    // Ride summary handed over by StartActivity. The legacy helper classes at the bottom of this
    // file also read some of these fields.
    int MaxSpeed=0;
    int AvgSpeed=0;
    int Getgodo=0;
    int RestTime=0;
    int IngTime=0;
    int Distence=0;
    int rr_num;
    String rr_comp="";
    String address_dong="";

    int check, clearCount=0;
    private RidePreviewMap ridePreviewMap;
    // "1" public / "0" private; read from the radio group when saving.
    String open = "1";

    SharedPreferences auto;
    String LoginId;
    int[] typeScore;

    int r_club;
    int dis;
    int riding_point=0;

    String comp_name, adress_value;
    Intent intent;
    private LineChart lineChart;
    private Object endActivity;

    // ---- save screen state ----
    /** Prepared (resized, private) copies of the photos picked so far; not yet part of any record. */
    private final ArrayList<File> draftPhotos = new ArrayList<>();
    private File draftDirectory;
    private RecordWeather rideWeather;
    private RideStartPoint startPoint;
    private String startedAtText;
    private boolean saving;
    private boolean mapExpanded;
    private RidePhotoUploader activeUploader;
    private Call<RecordWeather> weatherCall;
    private AlertDialog uploadDialog;
    private File gpxFile;
    private String gpxStamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentRidingEndBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        auto = getSharedPreferences("auto", Activity.MODE_PRIVATE);
        LoginId  = auto.getString("LoginId","");
        r_club = auto.getInt("r_club", 0);
        endActivity = this;
        draftDirectory = new File(getCacheDir(), "ride-photo-drafts");

        intent = new Intent(this.getIntent());

        Distence = intent.getIntExtra("distence",0);
        dis = Distence;
        MaxSpeed = intent.getIntExtra("maxspeed",0);
        AvgSpeed = intent.getIntExtra("avgspeed",0);
        Getgodo = intent.getIntExtra("getgodo",0);
        RestTime = intent.getIntExtra("resttime",0);
        IngTime = intent.getIntExtra("ingtime",0);
        check = intent.getIntExtra("check", 0);
        rr_comp = intent.getStringExtra("rr_comp");
        comp_name = intent.getStringExtra("comp_name");
        adress_value = intent.getStringExtra("addr");
        address_dong = intent.getStringExtra("address_dong");

        Log.d("End Activity", Distence + " " + MaxSpeed + " " + AvgSpeed + " " + Getgodo + " " + RestTime + " " + IngTime +" " +check + " " + rr_comp + " "+ comp_name+ " " + adress_value      );
        RideRouteCache.RouteData cachedRoute = RideRouteCache.load(
                this, intent.getStringExtra(RideRouteCache.EXTRA_CACHE_FILE)
        );
        ArrayList<Double> witch_lat = cachedRoute != null
                ? cachedRoute.latitudes : (ArrayList<Double>) intent.getSerializableExtra("witch_lat");
        ArrayList<Double> witch_lon = cachedRoute != null
                ? cachedRoute.longitudes : (ArrayList<Double>) intent.getSerializableExtra("witch_lon");
        ArrayList<Double> ele = cachedRoute != null
                ? cachedRoute.elevations : (ArrayList<Double>) intent.getSerializableExtra("ele");
        if (witch_lat == null) witch_lat = new ArrayList<>();
        if (witch_lon == null) witch_lon = new ArrayList<>();
        if (ele == null) ele = new ArrayList<>();
        Bundle extras = intent.getExtras();
        double maxLat = cachedRoute != null ? cachedRoute.maxLat : extras == null ? 0 : extras.getDouble("maxLat", 0);
        double maxLon = cachedRoute != null ? cachedRoute.maxLon : extras == null ? 0 : extras.getDouble("maxLon", 0);
        double minLat = cachedRoute != null ? cachedRoute.minLat : extras == null ? 0 : extras.getDouble("minLat", 0);
        double minLon = cachedRoute != null ? cachedRoute.minLon : extras == null ? 0 : extras.getDouble("minLon", 0);

        bindStats();
        bindDate();

        // Same data and same rules as the record detail: the track points that go into the GPX
        // (lat, lon, altitude). Unknown altitude is NaN and is skipped, never drawn as 0 m.
        lineChart = (LineChart) findViewById(R.id.chart);
        ElevationProfile.Builder profileBuilder = new ElevationProfile.Builder();
        int profilePoints = Math.min(Math.min(witch_lat.size(), witch_lon.size()), ele.size());
        for (int g = 0; g < profilePoints; g++) {
            Double altitude = ele.get(g);
            profileBuilder.add(witch_lat.get(g), witch_lon.get(g),
                    altitude == null || altitude.isNaN() ? null : altitude);
        }
        ElevationChartBinder.bind(lineChart, binding.chartEmpty, profileBuilder.build());
        binding.chartToggle.setOnClickListener(v -> {
            boolean show = binding.chartContainer.getVisibility() != View.VISIBLE;
            binding.chartContainer.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.chartArrow.setText(show ? "접기 ▴" : "펼치기 ▾");
            if (show) {
                lineChart.invalidate();
            }
        });

        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        ridePreviewMap = new RidePreviewMap(this, mapViewContainer, witch_lat, witch_lon);
        ridePreviewMap.start();
        binding.mapExpand.setOnClickListener(v -> toggleMapSize());

        bindVisibility();
        bindMemo();
        bindPhotos(savedInstanceState);

        startPoint = RideStartPoint.firstValid(witch_lat, witch_lon);
        startedAtText = formatStartedAt();
        loadWeather();

        findViewById(R.id.cancelriding).setOnClickListener(v -> confirmDiscardRide());
        findViewById(R.id.cancelriding_bottom).setOnClickListener(v -> confirmDiscardRide());

        String text = adress_value == null ? "" : adress_value.replace("대한민국", "").trim();
        binding.addr.setText(text);
        binding.addr.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);

        // ---- GPX file of the ride (same content as before; unknown altitude has no <ele>) ----
        gpxStamp = savedInstanceState != null ? savedInstanceState.getString(STATE_GPX_STAMP) : null;
        if (gpxStamp == null) {
            gpxStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date(System.currentTimeMillis()));
        }
        final String nowTime = gpxStamp;

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        GpxInfo gpxInfoObj = new GpxInfo();
        gpxInfoObj.setCreator("TrailGO");

        GpxInfo.metadata metaObj = new GpxInfo.metadata();
        gpxInfoObj.setMetadata(metaObj);
        metaObj.setDesc("TrailGO");

        GpxInfo.bounds boundsObj = new GpxInfo.bounds();
        metaObj.setBounds(boundsObj);
        boundsObj.setMaxLat(maxLat);
        boundsObj.setMaxLon(maxLon);
        boundsObj.setMinLat(minLat);
        boundsObj.setMinLon(minLon);

        GpxInfo.trk trkObj = new GpxInfo.trk();
        gpxInfoObj.setTrk(trkObj);
        GpxInfo.trkseg trksegObj = new GpxInfo.trkseg();
        trkObj.setTrkseg(trksegObj);

        List<GpxInfo.trkpt> trkptArray = new ArrayList<GpxInfo.trkpt>();
        int mapPointCount = Math.min(witch_lat.size(), witch_lon.size());
        int gpxPointCount = Math.min(mapPointCount, ele.size());
        for (int i = 0; i < gpxPointCount; i++) {
            GpxInfo.trkpt trkptObj = new GpxInfo.trkpt(witch_lat.get(i), witch_lon.get(i), ele.get(i));
            trkptArray.add(trkptObj);
        }
        trksegObj.setTrkptList(trkptArray);
        gpxFile = new File(getFilesDir().getPath() + "/gpx_" + nowTime + ".gpx");
        try {
            Serializer serializer = new Persister();
            serializer.write(gpxInfoObj, gpxFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.saveriding.setOnClickListener(v -> onSaveClicked(nowTime));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<String> paths = new ArrayList<>();
        for (File photo : draftPhotos) {
            paths.add(photo.getAbsolutePath());
        }
        outState.putStringArrayList(STATE_PHOTOS, paths);
        outState.putString(STATE_GPX_STAMP, gpxStamp);
    }

    // ---------------------------------------------------------------------------------------
    // Binding
    // ---------------------------------------------------------------------------------------

    /** Same units, digits and empty-value rules as the record detail screen. */
    private void bindStats() {
        binding.enddis.setText(RecordFormatter.formatDistance(Distence));
        binding.ending.setText(RecordFormatter.formatDuration(IngTime));
        binding.endresttime.setText(RecordFormatter.formatDuration(RestTime));
        binding.endavg.setText(RecordFormatter.formatSpeed(AvgSpeed, Distence, IngTime));
        binding.endmax.setText(RecordFormatter.formatSpeed(MaxSpeed, Distence, IngTime));
        binding.endget.setText(RecordFormatter.formatElevationGain(Getgodo));
    }

    private void bindDate() {
        Date now = new Date();
        binding.rideDate.setText(new SimpleDateFormat("yyyy. MM. dd (E)", Locale.KOREA).format(now));
        binding.rideTime.setText(new SimpleDateFormat("a h:mm", Locale.KOREA).format(now));
    }

    private void bindVisibility() {
        RideSettings.migrate(this);
        boolean guest = AccountSession.isGuest(this);
        open = !guest && RideSettings.isRecordPublic(this) ? "1" : "0";
        // Server records only know private (0) and public (1); there is no follower-only level.
        binding.selectgroup.check("0".equals(open) ? R.id.NoOpen : R.id.AllOpen);
        if (guest) {
            for (int i = 0; i < binding.selectgroup.getChildCount(); i++) {
                binding.selectgroup.getChildAt(i).setEnabled(false);
            }
            binding.visibilityHint.setText("비회원 기록은 이 기기에만 비공개로 저장됩니다.");
        } else {
            binding.visibilityHint.setText("전체 공개로 저장하면 다른 사용자가 경로, 메모, 사진을 볼 수 있습니다.");
        }
        binding.selectgroup.setOnCheckedChangeListener((group, checkedId) ->
                open = checkedId == R.id.NoOpen ? "0" : "1");
    }

    private void bindMemo() {
        updateMemoCounter(binding.rideMemo.getText().length());
        binding.rideMemo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateMemoCounter(s.length());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateMemoCounter(int length) {
        binding.memoCounter.setText(length + "/" + GuestRideStoreCore.MAX_MEMO_LENGTH);
    }

    private void bindPhotos(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            ArrayList<String> paths = savedInstanceState.getStringArrayList(STATE_PHOTOS);
            if (paths != null) {
                for (String path : paths) {
                    File file = new File(path);
                    if (file.isFile() && draftPhotos.size() < RidePhotoPicker.MAX_PHOTOS) {
                        draftPhotos.add(file);
                    }
                }
            }
        } else {
            // A fresh screen never inherits photos from an abandoned earlier save screen.
            PhotoImporter.clearDirectory(draftDirectory);
        }
        binding.photoStrip.setEditable(true);
        binding.photoStrip.setListener(new PhotoStripView.Listener() {
            @Override
            public void onAddClicked() {
                int remaining = RidePhotoPicker.MAX_PHOTOS - draftPhotos.size();
                if (remaining <= 0) {
                    android.widget.Toast.makeText(EndActivity.this, "사진은 최대 10장까지 추가할 수 있습니다.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!RidePhotoPicker.launch(EndActivity.this, remaining)) {
                    android.widget.Toast.makeText(EndActivity.this, "사진을 선택할 수 있는 앱을 찾지 못했습니다.", android.widget.Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onPhotoClicked(int index) {
                PhotoViewerDialog.show(EndActivity.this, draftItems(), index, null);
            }

            @Override
            public void onRemoveClicked(int index) {
                if (index >= 0 && index < draftPhotos.size()) {
                    File removed = draftPhotos.remove(index);
                    //noinspection ResultOfMethodCallIgnored
                    removed.delete();
                    refreshPhotos();
                }
            }
        });
        refreshPhotos();
    }

    private List<PhotoStripView.Item> draftItems() {
        List<PhotoStripView.Item> items = new ArrayList<>();
        for (File file : draftPhotos) {
            items.add(new PhotoStripView.Item(file, null, file));
        }
        return items;
    }

    private void refreshPhotos() {
        binding.photoStrip.setItems(draftItems());
        binding.photoCount.setText(draftPhotos.size() + "/" + RidePhotoPicker.MAX_PHOTOS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Cancelling the picker (or an empty result) leaves the photos chosen so far untouched.
        if (requestCode != RidePhotoPicker.REQUEST_CODE || resultCode != RESULT_OK || data == null) {
            return;
        }
        int remaining = RidePhotoPicker.MAX_PHOTOS - draftPhotos.size();
        List<Uri> uris = RidePhotoPicker.parse(data, remaining);
        if (uris.isEmpty()) {
            return;
        }
        if (data.getClipData() != null && data.getClipData().getItemCount() > remaining) {
            android.widget.Toast.makeText(this, "사진은 최대 10장까지 추가할 수 있어 일부만 추가했습니다.", android.widget.Toast.LENGTH_SHORT).show();
        }
        PhotoImporter.importAsync(this, uris, draftDirectory, (files, failed) -> {
            if (isFinishing() || isDestroyed()) {
                for (File file : files) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
                return;
            }
            for (File file : files) {
                if (draftPhotos.size() < RidePhotoPicker.MAX_PHOTOS) {
                    draftPhotos.add(file);
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
            refreshPhotos();
            if (failed > 0) {
                android.widget.Toast.makeText(this, failed + "장은 불러오지 못했습니다. 사진 접근 권한이나 파일을 확인해 주세요.", android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private void toggleMapSize() {
        mapExpanded = !mapExpanded;
        ViewGroup.LayoutParams params = binding.mapCard.getLayoutParams();
        params.height = Math.round((mapExpanded ? 420 : 190) * getResources().getDisplayMetrics().density);
        binding.mapCard.setLayoutParams(params);
        binding.mapExpand.setText(mapExpanded ? "⤡" : "⤢");
        binding.mapExpand.setContentDescription(mapExpanded ? "지도 축소" : "지도 확대");
        binding.mapCard.post(() -> {
            if (ridePreviewMap != null) {
                ridePreviewMap.refit();
            }
        });
    }

    // ---------------------------------------------------------------------------------------
    // Weather at the start of the ride (looked up by the TrailGo server, never by the app)
    // ---------------------------------------------------------------------------------------

    private String formatStartedAt() {
        long startedMillis = System.currentTimeMillis() - (long) Math.max(0, IngTime + RestTime) * 1000L;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        return format.format(new Date(startedMillis));
    }

    private void loadWeather() {
        // The weather looked up when the ride started is kept with the ride session: use that one.
        RecordWeather atStart = RideWeatherFile.load(getFilesDir());
        if (atStart != null) {
            showWeather(atStart, "날씨 정보 없음");
            return;
        }
        if (startPoint == null) {
            showWeather(null, "위치 정보가 없어 날씨를 확인할 수 없습니다");
            return;
        }
        String riderId = AccountSession.isGuest(this) || LoginId == null || LoginId.trim().isEmpty() ? null : LoginId;
        weatherCall = serverApi.getWeather(startPoint.latitude, startPoint.longitude, startedAtText, riderId);
        weatherCall.enqueue(new Callback<RecordWeather>() {
            @Override
            public void onResponse(Call<RecordWeather> call, Response<RecordWeather> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                RecordWeather body = response.isSuccessful() ? response.body() : null;
                showWeather(body != null && body.isAvailable() ? body : null, "날씨 정보 없음");
            }

            @Override
            public void onFailure(Call<RecordWeather> call, Throwable error) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }
                Log.w("RideWeather", "Weather unavailable: " + error.getClass().getSimpleName());
                showWeather(null, "날씨 정보 없음");
            }
        });
    }

    /** Shows the weather, or {@code emptyText} when there is none. Nothing is ever made up. */
    private void showWeather(RecordWeather weather, String emptyText) {
        String text = weather == null ? null
                : RecordFormatter.formatWeather(weather.isAvailable(), weather.getLabel(), weather.getTemperatureC());
        if (text == null) {
            rideWeather = null;
            binding.weatherIcon.setVisibility(View.GONE);
            binding.weatherText.setText(emptyText);
            binding.weatherNote.setText("");
            return;
        }
        rideWeather = weather;
        binding.weatherIcon.setImageResource(WeatherIcons.resource(weather));
        binding.weatherIcon.setVisibility(View.VISIBLE);
        binding.weatherText.setText(text);
        binding.weatherNote.setText(RecordFormatter.formatWeatherNote(weather.getObservedAt(), weather.getPrecipitationMm()));
    }

    // ---------------------------------------------------------------------------------------
    // Saving
    // ---------------------------------------------------------------------------------------

    private void setSaving(boolean value) {
        saving = value;
        binding.saveriding.setEnabled(!value);
        binding.saveriding.setText(value ? "저장 중..." : "기록 저장");
        binding.saveProgress.setVisibility(value ? View.VISIBLE : View.GONE);
        binding.cancelridingBottom.setEnabled(!value);
        // A rotation would recreate the screen while uploads still report back to it.
        setRequestedOrientation(value
                ? android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
                : android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void onSaveClicked(String nowTime) {
        if (saving) {
            return; // double tap
        }
        String rideName = binding.ridingNameinput.getText().toString().trim();
        if (rideName.isEmpty()) {
            rideName = "라이딩 기록";
        }
        String memo = GuestRideStoreCore.normalizeMemo(binding.rideMemo.getText().toString());

        if (AccountSession.isGuest(this)) {
            saveGuestRide(gpxFile, rideName, memo);
            return;
        }
        if (LoginId == null || LoginId.trim().isEmpty()) {
            android.widget.Toast.makeText(this, "로그인 정보를 확인할 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        String gpxFileName = "gpx_" + nowTime + ".gpx";
        MultipartBody.Part gpxPart = Server.getFilePart(gpxFile.getAbsolutePath(), gpxFileName);
        if (gpxPart == null) {
            android.widget.Toast.makeText(EndActivity.this, "GPX 파일을 만들지 못해 기록을 저장할 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        setSaving(true);
        final HashMap<String, Object> insert = new HashMap<>();
        insert.put("rr_rider", LoginId);
        insert.put("rr_distance", String.valueOf(Distence));
        insert.put("rr_topspeed", String.valueOf(MaxSpeed));
        insert.put("rr_avgspeed", String.valueOf(AvgSpeed));
        insert.put("rr_high", String.valueOf(Getgodo));
        insert.put("rr_gpx", gpxFileName);
        insert.put("rr_open", "0".equals(open) ? "0" : "1");
        insert.put("rr_breaktime", String.valueOf(RestTime));
        insert.put("rr_time", String.valueOf(IngTime));
        insert.put("rr_area", adress_value == null ? "" : adress_value);
        insert.put("rr_name", rideName);
        insert.put("rr_started_at", startedAtText);
        if (memo != null) {
            insert.put("rr_memo", memo);
        }
        if (rideWeather != null && rideWeather.getToken() != null) {
            // The server stores the snapshot it looked up itself; the app never sends weather values.
            insert.put("rr_weather_token", rideWeather.getToken());
        }
        if (startPoint != null) {
            // Only used by the server to look up the weather at the start; not stored as a field.
            insert.put("rr_start_lat", String.valueOf(startPoint.latitude));
            insert.put("rr_start_lon", String.valueOf(startPoint.longitude));
        }
        if (rr_comp != null && !rr_comp.trim().isEmpty() && !"null".equalsIgnoreCase(rr_comp.trim())) {
            insert.put("rr_comp", rr_comp);
        }

        serverApi.upload(gpxPart, "gpx", gpxFileName).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    setSaving(false);
                    android.widget.Toast.makeText(EndActivity.this, "GPX 업로드에 실패했습니다. 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                saveRideRecord(insert);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable error) {
                Log.e("RideSave", "Unable to upload GPX", error);
                setSaving(false);
                android.widget.Toast.makeText(EndActivity.this, "네트워크 연결을 확인한 뒤 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveGuestRide(File xmlFile, String rideName, String memo) {
        if (!xmlFile.isFile() || xmlFile.length() == 0L) {
            android.widget.Toast.makeText(this, "GPX 파일을 만들지 못해 이 기기에 저장할 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        setSaving(true);
        final ArrayList<File> photos = new ArrayList<>(draftPhotos);
        final RecordWeather weather = rideWeather == null ? null : rideWeather.withoutToken();
        new Thread(() -> {
            try {
                LocalRideStore.save(
                        EndActivity.this,
                        rideName,
                        Distence,
                        MaxSpeed,
                        AvgSpeed,
                        Getgodo,
                        RestTime,
                        IngTime,
                        adress_value,
                        xmlFile,
                        memo,
                        weather,
                        photos
                );
                runOnUiThread(() -> finishGuestSave());
            } catch (IOException error) {
                Log.e("GuestRideSave", "Unable to save local guest ride", error);
                runOnUiThread(() -> {
                    setSaving(false);
                    android.widget.Toast.makeText(EndActivity.this, "이 기기에 기록을 저장하지 못했습니다. 저장공간을 확인해 주세요.", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void finishGuestSave() {
        android.widget.Toast.makeText(this, "주행 기록을 이 기기에 저장했습니다.", android.widget.Toast.LENGTH_SHORT).show();
        RideSessionStore.clear(this);
        RideRouteCache.clear(this);
        goHome();
    }

    private void saveRideRecord(HashMap<String, Object> insert) {
        serverApi.InsertRecord(insert).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (!response.isSuccessful()) {
                    setSaving(false);
                    android.widget.Toast.makeText(EndActivity.this, "기록 저장에 실패했습니다. 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                // The ride itself is safely on the server now: the recovery checkpoint can go.
                // Photos are uploaded afterwards and can fail without losing the record.
                RideSessionStore.clear(EndActivity.this);
                RideRouteCache.clear(EndActivity.this);
                Map<String, String> body = response.body();
                String savedNumber = body == null ? null : body.get("rr_num");
                if (draftPhotos.isEmpty()) {
                    android.widget.Toast.makeText(EndActivity.this, "라이딩 기록을 저장했습니다.", android.widget.Toast.LENGTH_SHORT).show();
                    goHome();
                } else if (savedNumber == null) {
                    // An older server that does not report the new record number cannot take photos.
                    android.widget.Toast.makeText(EndActivity.this, "기록은 저장했지만 서버가 사진 업로드를 지원하지 않아 사진은 올리지 못했습니다.", android.widget.Toast.LENGTH_LONG).show();
                    goHome();
                } else {
                    uploadPhotos(savedNumber, new ArrayList<>(draftPhotos));
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable error) {
                Log.e("RideSave", "Unable to save ride", error);
                setSaving(false);
                android.widget.Toast.makeText(EndActivity.this, "네트워크 연결을 확인한 뒤 다시 시도해주세요.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadPhotos(final String recordNumber, final List<File> photos) {
        dismissUploadDialog();
        uploadDialog = new AlertDialog.Builder(this)
                .setTitle("사진 업로드")
                .setMessage("사진을 올리는 중입니다 (0/" + photos.size() + ")")
                .setCancelable(false)
                .create();
        uploadDialog.show();
        activeUploader = new RidePhotoUploader(serverApi, LoginId, recordNumber, photos, new RidePhotoUploader.Listener() {
            @Override
            public void onProgress(int done, int total) {
                if (uploadDialog != null) {
                    uploadDialog.setMessage("사진을 올리는 중입니다 (" + (done + 1) + "/" + total + ")");
                }
            }

            @Override
            public void onFinished(List<RecordPhoto> uploaded, final List<File> failed, int lastFailureCode) {
                dismissUploadDialog();
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (failed.isEmpty()) {
                    android.widget.Toast.makeText(EndActivity.this, "라이딩 기록과 사진을 저장했습니다.", android.widget.Toast.LENGTH_SHORT).show();
                    goHome();
                    return;
                }
                new AlertDialog.Builder(EndActivity.this)
                        .setTitle("사진 일부를 올리지 못했습니다")
                        .setMessage("라이딩 기록은 저장되었습니다.\n사진 " + failed.size() + "장: "
                                + RidePhotoUploader.describeFailure(lastFailureCode)
                                + "\n\n다시 시도하거나, 기록 상세 화면에서 나중에 추가할 수 있습니다.")
                        .setCancelable(false)
                        .setPositiveButton("다시 시도", (dialog, which) -> uploadPhotos(recordNumber, failed))
                        .setNegativeButton("나중에 추가", (dialog, which) -> goHome())
                        .show();
            }
        });
        activeUploader.start();
    }

    private void dismissUploadDialog() {
        if (uploadDialog != null) {
            try {
                uploadDialog.dismiss();
            } catch (IllegalArgumentException ignored) {
                // window already gone
            }
            uploadDialog = null;
        }
    }

    private void goHome() {
        PhotoImporter.clearDirectory(draftDirectory);
        Intent home = new Intent(this, com.bike.trailgo.MainActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ridePreviewMap != null) {
            ridePreviewMap.resume();
        }
    }

    @Override
    protected void onPause() {
        if (ridePreviewMap != null) {
            ridePreviewMap.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (weatherCall != null) {
            weatherCall.cancel();
        }
        if (activeUploader != null) {
            activeUploader.cancel();
        }
        dismissUploadDialog();
        if (ridePreviewMap != null) {
            ridePreviewMap.finish();
            ridePreviewMap = null;
        }
        super.onDestroy();
    }


    // ?濡ろ뜑??????ш낄援??????? ????낅묄 ????녿ぅ??熬곣뫀肄?
    public class UpdateCompScore extends AsyncTask<Map<String, String>, Integer, String> {
        @Override
        protected String doInBackground(Map<String, String>... maps) {
            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            //URL?? ??ш끽維?????????ш끽維쀩???⑥щ쾺?嶺? ????곸죷???⑤；???筌뤾퍓???
            HttpClient.Builder http = new HttpClient.Builder("PUT", "app/riding/updateCompScore");
            // Parameter ????ш끽維뽬땻??筌먲퐢??
            http.addAllParameters(maps[0]);
            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();
            return body;
        }
        @Override
        protected void onPostExecute(String s) {
        }
    }

    //?濡ろ뜑????癲ル슢?꾤땟?????????ш낄援????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
    public class getCompAllScore extends AsyncTask<Map<String, String>, Integer, String> {

        @Override
        protected String doInBackground(Map<String, String>... maps) {

            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            //URL?? ??ш끽維?????????ш끽維쀩???⑥щ쾺?嶺? ????곸죷???⑤；???筌뤾퍓???
            HttpClient.Builder http = new HttpClient.Builder("GET", "app/getCompAllScore/" + rr_comp);
            // Parameter ????ш끽維뽬땻??筌먲퐢??

            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();

            return body;
        }

        @Override
        protected void onPostExecute(String s) {
            try{
                String tempData = s;

                Gson gson = new Gson();
                ArrayList<Competition_Status> itemList = new ArrayList<>();
                Competition_Status[] items = gson.fromJson(tempData, Competition_Status[].class);

                for ( int i = 0; i<items.length; i++){
                    if ( items[i].getCs_club() == r_club)
                        items[i].setCs_score( items[i].getCs_score() + Integer.parseInt(String.valueOf(AvgSpeed)));
                    itemList.add(items[i]);
                }

                ArrayList<Integer> tempInt = new ArrayList<>();

                for ( int j = 0; j < itemList.size(); j++) {
                    tempInt.add(itemList.get(j).getCs_score());
                }

                // ??????嶺뚮㉡?ｈ??
                Collections.sort(tempInt);
                Collections.reverse(tempInt);

                ArrayList<Competition_Status> NewItemList = new ArrayList<>();

                for ( int l = 0; l < itemList.size(); l ++){
                    for ( int k = 0; k < itemList.size(); k++){
                        if ( tempInt.get(l) == itemList.get(k).getCs_score() )
                            NewItemList.add(itemList.get(k));
                    }
                }

                int count = NewItemList.size();

                updateRank[] UpdateRank = new updateRank[count];
                Map<String, String>[] params3 = new HashMap[count];

                for ( int k = 0; k < count; k++) {
                    UpdateRank[k] = new updateRank();
                    params3[k] = new HashMap<>();
                    params3[k].put("rr_comp", rr_comp);
                    params3[k].put("r_club", String.valueOf(NewItemList.get(k).getCs_club()));
                    params3[k].put("score", String.valueOf(NewItemList.get(k).getCs_score()));
                    params3[k].put("rank", String.valueOf(k+1));
                    UpdateRank[k].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params3[k]);
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    // ??筌믨퀡彛?????녿ぅ??熬곣뫀肄?
    public class updateRank extends AsyncTask<Map<String, String>, Integer, String> {
        @Override
        protected synchronized String doInBackground( Map<String, String>... maps) {
            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            //URL?? ??ш끽維?????????ш끽維쀩???⑥щ쾺?嶺? ????곸죷???⑤；???筌뤾퍓???
            HttpClient.Builder http = new HttpClient.Builder("PUT", "app/updateRank/");
            // Parameter ????ш끽維뽬땻??筌먲퐢??
            http.addAllParameters(maps[0]);
            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();
            return body;
        }
        @Override
        protected void onPostExecute(String s) {
        }
    }

    public class getMissionStatus extends AsyncTask<Map<String, String>, Integer, String> {

        @Override
        protected String doInBackground(Map<String, String>... maps) {

            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            //URL?? ??ш끽維?????????ш끽維쀩???⑥щ쾺?嶺? ????곸죷???⑤；???筌뤾퍓???
            HttpClient.Builder http = new HttpClient.Builder("GET", "app/getMissionStatus/" + LoginId);
            // Parameter ????ш끽維뽬땻??筌먲퐢??

            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();

            return body;
        }

        @Override
        protected void onPostExecute(String s) {
            try{
                String tempData = s;

                Gson gson = new Gson();
                ArrayList<Mission_Status> itemList = new ArrayList<>();
                Mission_Status[] items = gson.fromJson(tempData, Mission_Status[].class);

                for ( int i = 0; i<items.length; i++){
                    itemList.add(items[i]);
                }

                typeScore = new int[itemList.size()];

                for ( int j = 0; j<itemList.size(); j++) {
                    if ( itemList.get(j).getMs_type() == 1)
                        typeScore[0] = itemList.get(0).getMs_score() + Integer.parseInt(String.valueOf(Distence)); // ??癲꾧퀗??????節뚮쳮雅?
                    else if ( itemList.get(j).getMs_type() == 2) {
                        typeScore[1] = itemList.get(1).getMs_score(); // ?濡ろ뜑????癲ル슔?蹂?덫??????낅묄 ??節뚮쳮雅?
                        if (!rr_comp.equals("null"))
                            typeScore[1] += 1;
                    }
                    else if ( itemList.get(j).getMs_type() == 3) {
                        typeScore[2] = itemList.get(j).getMs_score();
                    }

                }

                new getNoClearMission().execute();

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public class getNoClearMission extends AsyncTask<Map<String, String>, Integer, String> {

        @Override
        protected String doInBackground(Map<String, String>... maps) {

            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            //URL?? ??ш끽維?????????ш끽維쀩???⑥щ쾺?嶺? ????곸죷???⑤；???筌뤾퍓???
            HttpClient.Builder http = new HttpClient.Builder("GET", "app/getNoClearMission/" + LoginId);
            // Parameter ????ш끽維뽬땻??筌먲퐢??

            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();

            return body;
        }

        @Override
        protected void onPostExecute(String s) {
            try{
                String tempData = s;

                Gson gson = new Gson();
                ArrayList<Mission> itemList = new ArrayList<>();
                ArrayList<Mission> clearList = new ArrayList<>();
                Mission[] items = gson.fromJson(tempData, Mission[].class);

                for ( int i = 0; i<items.length; i++){
                    itemList.add(items[i]);
                }

                for ( int j = 0; j<items.length; j++) {
                    if (itemList.get(j).getM_type() == 1) {
                        String tempStr = itemList.get(j).getM_content();
                        String changeStr = tempStr.replaceAll("[^0-9]", "");
                        if (typeScore[0] >= Integer.parseInt(changeStr)) {
                            clearList.add(itemList.get(j));
                            typeScore[2]++;
                            clearCount++;
                        }
                    }
                    else if ( itemList.get(j).getM_type() == 2){
                        String tempStr = itemList.get(j).getM_content();
                        String changeStr = tempStr.replaceAll("[^0-9]", "");
                        if ( typeScore[1] >= Integer.parseInt(changeStr)){
                            clearList.add(itemList.get(j));
                            typeScore[2]++;
                            clearCount++;
                        }
                    }

                    if ( itemList.get(j).getM_type() == 3){
                        String tempStr = itemList.get(j).getM_content();
                        String changeStr = tempStr.replaceAll("[^0-9]", "");
                        if ( typeScore[2] >= Integer.parseInt(changeStr)){
                            clearList.add(itemList.get(j));
                            typeScore[2]++;
                            clearCount++;
                        }
                    }

                    if ( itemList.get(j).getM_type() == 4){
                        String tempStr = itemList.get(j).getM_content();
                        String changeStr = tempStr.replaceAll("[^0-9]", "");
                        if ( Integer.parseInt(String.valueOf(AvgSpeed)) >= Integer.parseInt(changeStr)){
                            clearList.add(itemList.get(j));
                            typeScore[2]++;
                            clearCount++;
                        }
                    }
                }

                updateMissionStatus UMS = new updateMissionStatus();

                Map<String, String> params4 = new HashMap<>();

                params4.put("LoginId", LoginId);
                params4.put("typeScore1", String.valueOf(typeScore[0]));
                params4.put("typeScore2", String.valueOf(typeScore[2]));
                UMS.execute(params4);


                int count = clearList.size();
                if ( count >= 1 ){
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date time = new Date();
                    String time2 = format.format(time);
                    insertMissionCom[] IMC = new insertMissionCom[count];
                    Map<String, String>[] params5 = new HashMap[count];
                    for ( int k = 0; k < clearList.size(); k++){
                        IMC[k] = new insertMissionCom();
                        params5[k] = new HashMap<>();
                        params5[k].put("LoginId", LoginId);
                        params5[k].put("mc_mission", String.valueOf(clearList.get(k).getM_num()));
                        params5[k].put("mc_time", time2);
                        params5[k].put("m_point", String.valueOf(clearList.get(k).getM_point()));
                        riding_point += clearList.get(k).getM_point();
                        IMC[k].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params5[k]);
                    }
                }

                // ???源낇꼧???⑥????袁⑸즴????
                AlertDialog.Builder alert_confirm = new AlertDialog.Builder((Context) endActivity);
                // 癲ル슢??袁λ빝?壤?
                String text = "??繹먮끏?????れ삀??쎈뭄?????潁뺛깾逾???怨?????덊렡";
                String text2 = "";
                String text3 = riding_point + " ??繹먮끏????????????볥뜪 !";

                alert_confirm.setMessage(text + "\n" + text3);

                if ( clearList.size() >= 1){
                    for ( int l = 0; l < clearList.size(); l++){
                        text2 += clearList.get(l).getM_name() + " \uC644\uB8CC\n";
                    }
                    alert_confirm.setMessage( text + "\n" + text2 + "\n" + text3);
                }
                // ?嶺뚮Ĳ?됮??類?????域밸Ŧ遊얕짆??
                alert_confirm.setPositiveButton("\uD655\uC778", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                // ???源낇꼧???⑥?????獄쏅똻??
                AlertDialog alert = alert_confirm.create();
                // ???源낇꼧???⑥????????
                alert.setTitle("\uB77C\uC774\uB529 \uC800\uC7A5 \uC644\uB8CC");
                // ???源낇꼧???⑥????怨뚮옖??逾?
                alert.show();

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (saving) {
            android.widget.Toast.makeText(this, "저장 중입니다. 잠시만 기다려 주세요.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        confirmDiscardRide();
    }

    private void confirmDiscardRide() {
        new AlertDialog.Builder(this)
                .setTitle("저장하지 않고 나갈까요?")
                .setMessage("이번 주행 기록은 저장되지 않습니다.")
                .setNegativeButton("계속 작성", null)
                .setPositiveButton("나가기", (dialog, which) -> {
                    RideSessionStore.clear(EndActivity.this);
                    RideRouteCache.clear(EndActivity.this);
                    PhotoImporter.clearDirectory(draftDirectory);
                    finish();
                })
                .show();
    }

    public class updateMissionStatus extends AsyncTask<Map<String, String>, Integer, String> {
        @Override
        protected synchronized String doInBackground( Map<String, String>... maps) {
            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            //URL?? ??ш끽維?????????ш끽維쀩???⑥щ쾺?嶺? ????곸죷???⑤；???筌뤾퍓???
            HttpClient.Builder http = new HttpClient.Builder("PUT", "app/updateMissionStatus/");
            // Parameter ????ш끽維뽬땻??筌먲퐢??
            http.addAllParameters(maps[0]);
            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();
            return body;
        }
        @Override
        protected void onPostExecute(String s) {
        }
    }

    public class insertMissionCom extends AsyncTask<Map<String, String>, Integer, String> {
        @Override
        protected synchronized String doInBackground( Map<String, String>... maps) {
            // Http ??釉먯뒜??濚욌꼬裕뼘????????
            //URL?? ??ш끽維?????????ш끽維쀩???⑥щ쾺?嶺? ????곸죷???⑤；???筌뤾퍓???
            HttpClient.Builder http = new HttpClient.Builder("PUT", "app/insertMissionCom/");
            // Parameter ????ш끽維뽬땻??筌먲퐢??
            http.addAllParameters(maps[0]);
            //Http ??釉먯뒜????ш끽維뽬땻?
            HttpClient post = http.create();
            post.request();

            // ???쑩?젆????ㅺ컼??熬곣뫀?????좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            int statusCode = post.getHttpStatusCode();

            // ???쑩?젆??怨뚮옖筌?쑜猷???좊읈??嶺뚮ㅎ?닸쾮濡㏓섀?
            String body = post.getBody();
            return body;
        }
        @Override
        protected void onPostExecute(String s) {

        }
    }

}
