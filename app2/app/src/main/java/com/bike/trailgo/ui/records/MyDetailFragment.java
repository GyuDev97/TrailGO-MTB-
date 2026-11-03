package com.bike.trailgo.ui.records;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bike.trailgo.R;
import com.bike.trailgo.data.LocalRideStore;
import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.gpx.GPXParser;
import com.bike.trailgo.gpx.Gpx;
import com.bike.trailgo.gpx.Track;
import com.bike.trailgo.gpx.TrackPoint;
import com.bike.trailgo.gpx.TrackSegment;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.ui.BaseFragment;
import com.github.mikephil.charting.charts.LineChart;
import com.kakao.vectormap.GestureType;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;
import com.kakao.vectormap.route.RouteLine;
import com.kakao.vectormap.route.RouteLineLayer;
import com.kakao.vectormap.route.RouteLineOptions;
import com.kakao.vectormap.route.RouteLineSegment;
import com.kakao.vectormap.route.RouteLineStyle;
import com.kakao.vectormap.route.RouteLineStyles;
import com.kakao.vectormap.route.RouteLineStylesSet;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MyDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyDetailFragment extends BaseFragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    private String rr_num, LoginId;
    private int rr_open;
    private GPXParser mParser = new GPXParser();
    private Gpx parsedGpx = null;
    private com.kakao.vectormap.MapView mapView;
    private KakaoMap kakaoMap;
    private RouteLineLayer routeLineLayer;
    private RouteLineStyles routeLineStyles;
    private RouteLine routeLine;
    private LabelLayer markerLayer;
    private LabelStyles startMarkerStyles;
    private LabelStyles finishMarkerStyles;
    private Label startMarker;
    private Label finishMarker;
    private final ArrayList<LatLng> routePoints = new ArrayList<>();
    private boolean waitingForGpx;
    private boolean gpxRequestFailed;
    private TextView mapStatus;
    private ImageView mapFallback;
    private LinearLayout set_noopen, set_open;
    private TextView publicVisibilityAction, privateVisibilityAction;
    private ImageView userImage;
    private Call<Map<String, String>> record;
    List<Track> tracks;
    private TextView RidingTime, RidingRest, RidingDistance, RidingMax, RidingAvg, RidingGet, RidingAddr;
    private TextView detailTitle, detailDate, detailTime, detailGraphHigh, detailGraphLow,
            detailElevationEmpty, detailGraphCaption;
    private View detailGraphLegend;
    private LineChart elevationChart;
    // Memo, photos and weather of this ride (server or guest store).
    private RecordMediaSection mediaSection;
    // Built from GPX track points on the loader thread; null until the GPX load finishes.
    private ElevationProfile elevationProfile;

    public MyDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MyDetailFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MyDetailFragment newInstance(String param1, String param2) {
        MyDetailFragment fragment = new MyDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LoginId = model.r_Id.getValue();
        rr_num = firstNotEmpty(model.r_num.getValue(), "-1");
        String gpxFile = firstNotEmpty(model.my_rec_gpx.getValue(), "");

        waitingForGpx = gpxFile.length() > 0 && !"null".equalsIgnoreCase(gpxFile);
        gpxRequestFailed = false;
        elevationProfile = null;
        startNativeMap(view);
        if (waitingForGpx) {
            loadGpxRoute(gpxFile);
        }

        RidingTime = (TextView) view.findViewById(R.id.Riding_time);
        RidingRest = (TextView) view.findViewById(R.id.Riding_rest);
        RidingDistance = (TextView) view.findViewById(R.id.Riding_distance);
        RidingMax = (TextView) view.findViewById(R.id.Riding_max);
        RidingAvg = (TextView) view.findViewById(R.id.Riding_avg);
        RidingGet = (TextView) view.findViewById(R.id.Riding_godo);
        RidingAddr = (TextView) view.findViewById(R.id.Riding_addr);
        detailTitle = view.findViewById(R.id.detail_title);
        detailDate = view.findViewById(R.id.detail_date);
        detailTime = view.findViewById(R.id.detail_time);
        detailGraphHigh = view.findViewById(R.id.detail_graph_high);
        detailGraphLow = view.findViewById(R.id.detail_graph_low);
        detailGraphLegend = view.findViewById(R.id.detail_graph_legend);
        detailGraphCaption = view.findViewById(R.id.detail_graph_caption);
        detailElevationEmpty = view.findViewById(R.id.detail_elevation_empty);
        elevationChart = view.findViewById(R.id.detail_elevation_chart_view);
        renderElevationState();
        mediaSection = new RecordMediaSection(this, view, serverApi, LoginId, rr_num, isLocalRecord());
        mediaSection.load();

        View back = view.findViewById(R.id.detail_back);
        if (back != null) {
            back.setOnClickListener(v -> controller.popBackStack());
        }

        View shareButton = view.findViewById(R.id.detail_share_button);
        View shareTop = view.findViewById(R.id.detail_share_top);
        View.OnClickListener shareClick = v -> shareRecord();
        shareButton.setOnClickListener(shareClick);
        shareTop.setOnClickListener(shareClick);

        view.findViewById(R.id.detail_more).setOnClickListener(this::showMoreMenu);
        view.findViewById(R.id.detail_delete_button).setOnClickListener(v -> showDeleteConfirmation());
        view.findViewById(R.id.detail_gpx_export_button).setOnClickListener(v -> exportGpx());

        String recordName = firstNotEmpty(model.my_rec_name.getValue());
        String recordArea = firstNotEmpty(model.my_rec_adress.getValue());
        setTextIfPresent(detailTitle, firstNotEmpty(recordName, recordArea, "라이딩 기록"));
        setTextIfPresent(detailDate, formatDate(model.my_rec_date.getValue()));
        setTextIfPresent(detailTime, formatClock(model.my_rec_date.getValue()));
        // The address only gets its own line when the title is something else (a custom name).
        if (RidingAddr != null && recordArea.length() > 0 && recordName.length() > 0
                && !recordArea.equals(recordName)) {
            RidingAddr.setText(recordArea);
            RidingAddr.setVisibility(View.VISIBLE);
        }

        // Values arrive already formatted by RecordFormatter (records list and home both use it).
        int distanceMeters = RecordFormatter.parseDistanceMeters(model.my_rec_dis.getValue());
        int ridingSeconds = RecordFormatter.parseDurationSeconds(model.my_rec_time.getValue());
        setTextIfPresent(RidingDistance, firstNotEmpty(model.my_rec_dis.getValue(), RecordFormatter.EMPTY));
        setTextIfPresent(RidingTime, firstNotEmpty(model.my_rec_time.getValue(), RecordFormatter.EMPTY));
        setTextIfPresent(RidingAvg, firstNotEmpty(model.my_rec_avg.getValue(), RecordFormatter.EMPTY));
        setTextIfPresent(RidingMax, firstNotEmpty(model.my_rec_max.getValue(), RecordFormatter.EMPTY));
        // rr_high is accumulated climbing; it is never reused as altitude above sea level.
        setTextIfPresent(RidingGet, firstNotEmpty(model.my_rec_get.getValue(), RecordFormatter.EMPTY));
        setTextIfPresent(RidingRest, firstNotEmpty(model.my_rec_rest.getValue(), RecordFormatter.EMPTY));
        set_open = (LinearLayout) view.findViewById(R.id.set_open);
        set_noopen = (LinearLayout) view.findViewById(R.id.set_noopen);
        publicVisibilityAction = view.findViewById(R.id.detail_visibility_public_action);
        privateVisibilityAction = view.findViewById(R.id.detail_visibility_private_action);
        Integer openValue = model.my_rec_open.getValue();
        rr_open = openValue == null ? 0 : openValue;
        renderVisibilityControl();
    }

    private void renderVisibilityControl() {
        if (isLocalRecord()) {
            set_open.setVisibility(View.GONE);
            set_noopen.setVisibility(View.GONE);
            return;
        }
        boolean isPublic = rr_open != 0;
        set_open.setVisibility(isPublic ? View.VISIBLE : View.GONE);
        set_noopen.setVisibility(isPublic ? View.GONE : View.VISIBLE);
        publicVisibilityAction.setText("공개");
        privateVisibilityAction.setText("비공개");
        set_open.setContentDescription("공개 기록. 탭하면 비공개로 전환합니다.");
        set_noopen.setContentDescription("비공개 기록. 탭하면 전체 공개로 전환합니다.");
        set_open.setEnabled(true);
        set_noopen.setEnabled(true);
        set_open.setOnClickListener(v -> confirmVisibilityChange(0));
        set_noopen.setOnClickListener(v -> confirmVisibilityChange(1));
    }

    private void confirmVisibilityChange(int nextOpenValue) {
        boolean makePublic = nextOpenValue != 0;
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(makePublic ? "기록을 전체 공개할까요?" : "기록을 비공개로 전환할까요?")
                .setMessage(makePublic
                        ? "다른 사용자가 이 주행 기록을 볼 수 있습니다."
                        : "다른 사용자는 이 주행 기록을 볼 수 없게 됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton(makePublic ? "전체 공개" : "비공개 전환",
                        (dialog, which) -> updateRecordVisibility(nextOpenValue))
                .show();
    }

    private void updateRecordVisibility(int nextOpenValue) {
        if (isLocalRecord()) {
            Toast.makeText(requireContext(), "게스트 기록은 이 기기에만 비공개로 저장됩니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isPreviewRecord()) {
            Toast.makeText(requireContext(), "저장된 기록에서만 공개 설정을 변경할 수 있습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        set_open.setEnabled(false);
        set_noopen.setEnabled(false);
        HashMap<String, Object> input = new HashMap<>();
        input.put("rr_num", rr_num);
        input.put("rr_open", String.valueOf(nextOpenValue));
        record = serverApi.setOpen(input);
        record.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null
                        && "1".equals(response.body().get("updated"))) {
                    rr_open = nextOpenValue;
                    model.my_rec_open.setValue(nextOpenValue);
                    renderVisibilityControl();
                    Toast.makeText(requireContext(), nextOpenValue == 0
                            ? "기록을 비공개로 전환했습니다."
                            : "기록을 전체 공개했습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("RecordVisibility", "Visibility update rejected: HTTP " + response.code());
                    renderVisibilityControl();
                    Toast.makeText(requireContext(), "공개 설정을 변경하지 못했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Log.e("RecordVisibility", "Unable to update record visibility", t);
                renderVisibilityControl();
                Toast.makeText(requireContext(), "네트워크 연결을 확인한 뒤 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startNativeMap(View view) {
        mapView = view.findViewById(R.id.map_view);
        mapStatus = view.findViewById(R.id.detail_map_status);
        mapFallback = view.findViewById(R.id.detail_map_fallback);
        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                Log.d("RecordDetailMap", "Kakao map destroyed");
            }

            @Override
            public void onMapError(Exception error) {
                Log.e("RecordDetailMap", "Kakao map error", error);
                if (!isAdded()) {
                    return;
                }
                mapFallback.setVisibility(View.VISIBLE);
                mapStatus.setVisibility(View.VISIBLE);
                mapStatus.setText("지도를 불러오지 못했습니다");
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap readyMap) {
                kakaoMap = readyMap;
                kakaoMap.setGestureEnable(GestureType.Pan, true);
                kakaoMap.setGestureEnable(GestureType.Zoom, true);
                routeLineLayer = kakaoMap.getRouteLineManager().getLayer();
                routeLineStyles = RouteLineStyles.from(
                        RouteLineStyle.from(10, Color.rgb(30, 157, 70), 3, Color.WHITE)
                );
                markerLayer = kakaoMap.getLabelManager().getLayer();
                startMarkerStyles = kakaoMap.getLabelManager().addLabelStyles(
                        LabelStyles.from(LabelStyle.from(drawableToBitmap(R.drawable.riding_ic_play, 28)))
                );
                finishMarkerStyles = kakaoMap.getLabelManager().addLabelStyles(
                        LabelStyles.from(LabelStyle.from(drawableToBitmap(R.drawable.riding_ic_flag, 28)))
                );
                mapFallback.setVisibility(View.GONE);
                mapStatus.setVisibility(View.GONE);
                if (!waitingForGpx) {
                    renderRoute();
                }
            }

            @Override
            public LatLng getPosition() {
                return LatLng.from(35.8329, 128.5650);
            }

            @Override
            public int getZoomLevel() {
                return 13;
            }
        });
    }

    private void loadGpxRoute(String gpxFile) {
        new Thread(() -> {
            ArrayList<LatLng> loadedPoints = new ArrayList<>();
            ElevationProfile.Builder elevationBuilder = new ElevationProfile.Builder();
            HttpURLConnection connection = null;
            InputStream input = null;
            boolean loadFailed = false;
            try {
                if (LocalRideStore.isLocalGpxReference(gpxFile)) {
                    input = LocalRideStore.openGpx(requireContext(), gpxFile);
                } else {
                    connection = Server.openConnection("app/getGPX/gpx/" + gpxFile);
                    input = connection.getInputStream();
                }
                parsedGpx = mParser.parse(input);
                if (parsedGpx != null) {
                    tracks = parsedGpx.getTracks();
                    for (Track track : tracks) {
                        for (TrackSegment segment : track.getTrackSegments()) {
                            for (TrackPoint point : segment.getTrackPoints()) {
                                loadedPoints.add(LatLng.from(point.getLatitude(), point.getLongitude()));
                                elevationBuilder.add(point.getLatitude(), point.getLongitude(), point.getElevation());
                            }
                        }
                    }
                }
            } catch (IOException | XmlPullParserException error) {
                loadFailed = true;
                Log.e("RecordDetailMap", "Unable to load GPX route", error);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ignored) {
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }

            if (!isAdded()) {
                return;
            }
            final boolean didFail = loadFailed;
            final ElevationProfile loadedProfile = didFail ? null : elevationBuilder.build();
            requireActivity().runOnUiThread(() -> {
                gpxRequestFailed = didFail;
                routePoints.clear();
                routePoints.addAll(loadedPoints);
                waitingForGpx = false;
                elevationProfile = loadedProfile;
                renderRoute();
                renderElevationState();
            });
        }).start();
    }

    private void renderRoute() {
        if (kakaoMap == null || routeLineLayer == null || routeLineStyles == null) {
            return;
        }
        if (routePoints.size() < 2) {
            mapFallback.setVisibility(View.VISIBLE);
            mapStatus.setVisibility(View.VISIBLE);
            if (gpxRequestFailed) {
                mapStatus.setText("저장된 GPX 파일을 불러오지 못했습니다");
            } else if (routePoints.size() == 1) {
                mapStatus.setText("이 기록은 위치 포인트가 1개라 경로를 표시할 수 없습니다");
            } else {
                mapStatus.setText("이 기록에는 표시할 GPX 위치 정보가 없습니다");
            }
            return;
        }

        mapFallback.setVisibility(View.GONE);
        mapStatus.setVisibility(View.GONE);

        RouteLineSegment segment = RouteLineSegment.from(new ArrayList<>(routePoints), routeLineStyles);
        if (routeLine == null) {
            routeLine = routeLineLayer.addRouteLine(
                    RouteLineOptions.from(segment).setStylesSet(RouteLineStylesSet.from(routeLineStyles))
            );
        } else {
            routeLine.changeSegments(segment);
        }

        LatLng start = routePoints.get(0);
        LatLng finish = routePoints.get(routePoints.size() - 1);
        if (startMarker == null) {
            startMarker = markerLayer.addLabel(
                    LabelOptions.from("record-start", start).setStyles(startMarkerStyles)
            );
            finishMarker = markerLayer.addLabel(
                    LabelOptions.from("record-finish", finish).setStyles(finishMarkerStyles)
            );
        } else {
            startMarker.moveTo(start);
            finishMarker.moveTo(finish);
        }
        kakaoMap.moveCamera(
                com.kakao.vectormap.camera.CameraUpdateFactory.fitMapPoints(
                        routePoints.toArray(new LatLng[0]), 80
                )
        );
    }

    private Bitmap drawableToBitmap(int drawableId, int sizeDp) {
        int sizePx = Math.round(sizeDp * getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable drawable = AppCompatResources.getDrawable(requireContext(), drawableId);
        if (drawable != null) {
            drawable.setBounds(0, 0, sizePx, sizePx);
            drawable.draw(canvas);
        }
        return bitmap;
    }

    private static final int REQUEST_EXPORT_GPX = 7401;

    /** Lets the rider pick where the GPX file of this ride is saved (Files app, Drive, ...). */
    private void exportGpx() {
        if (!hasGpx()) {
            Toast.makeText(requireContext(), "이 기록에는 내보낼 경로(GPX)가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(GpxExport.MIME_TYPE);
        intent.putExtra(Intent.EXTRA_TITLE, GpxExport.fileName(
                firstNotEmpty(model.my_rec_name.getValue(), model.my_rec_adress.getValue()), model.my_rec_date.getValue()));
        try {
            startActivityForResult(intent, REQUEST_EXPORT_GPX);
        } catch (RuntimeException e) {
            Toast.makeText(requireContext(), "파일을 저장할 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasGpx() {
        String gpxFile = firstNotEmpty(model.my_rec_gpx.getValue(), "");
        return gpxFile.length() > 0 && !"null".equalsIgnoreCase(gpxFile);
    }

    /** Copies the ride's GPX (device store or server) byte for byte to the chosen file. */
    private void writeGpxTo(android.net.Uri target) {
        final android.content.Context appContext = requireContext().getApplicationContext();
        final String gpxFile = firstNotEmpty(model.my_rec_gpx.getValue(), "");
        new Thread(() -> {
            boolean ok = false;
            HttpURLConnection connection = null;
            try {
                InputStream source;
                if (LocalRideStore.isLocalGpxReference(gpxFile)) {
                    source = LocalRideStore.openGpx(appContext, gpxFile);
                } else {
                    connection = Server.openConnection("app/getGPX/gpx/" + gpxFile);
                    source = connection.getInputStream();
                }
                try (InputStream in = source;
                     java.io.OutputStream out = appContext.getContentResolver().openOutputStream(target, "wt")) {
                    if (out == null) throw new IOException("no output");
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                    ok = true;
                }
            } catch (IOException | RuntimeException error) {
                Log.w("RecordDetail", "GPX export failed", error);
            } finally {
                if (connection != null) connection.disconnect();
            }
            final boolean saved = ok;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> Toast.makeText(appContext,
                    saved ? "GPX 파일을 저장했습니다." : "GPX 파일을 저장하지 못했습니다.", Toast.LENGTH_SHORT).show());
        }, "gpx-export").start();
    }

    private void shareRecord() {
        String title = firstNotEmpty(model.my_rec_name.getValue(), "라이딩 기록");
        String summary = title + "\n"
                + firstNotEmpty(model.my_rec_dis.getValue(), "-") + " · "
                + firstNotEmpty(model.my_rec_time.getValue(), "-") + " · "
                + firstNotEmpty(model.my_rec_avg.getValue(), "-");
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, summary);
        startActivity(Intent.createChooser(intent, "라이딩 기록 공유"));
    }

    private boolean isPreviewRecord() {
        try {
            return Integer.parseInt(rr_num) < 0;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private void showDeleteConfirmation() {
        if (!isLocalRecord()) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("기록 삭제")
                    .setMessage("회원 기록 삭제는 서버 기능이 준비된 뒤 사용할 수 있습니다.")
                    .setPositiveButton("확인", null)
                    .show();
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("기기 기록 삭제")
                .setMessage("이 기기에 저장된 주행 요약과 GPX 경로를 삭제합니다. 되돌릴 수 없습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> deleteLocalRecord())
                .show();
    }

    private void deleteLocalRecord() {
        final int localId;
        try {
            localId = Integer.parseInt(rr_num);
        } catch (NumberFormatException error) {
            Toast.makeText(requireContext(), "기록 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                boolean deleted = LocalRideStore.delete(requireContext().getApplicationContext(), localId);
                if (deleted) {
                    // its route picture on the home screen is no longer needed
                    com.bike.trailgo.ui.home.RouteThumbnailLoader.get(requireContext().getApplicationContext())
                            .invalidate(model.my_rec_gpx.getValue());
                }
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (deleted) {
                        Toast.makeText(requireContext(), "기기 기록을 삭제했습니다.", Toast.LENGTH_SHORT).show();
                        controller.popBackStack();
                    } else {
                        Toast.makeText(requireContext(), "이미 삭제되었거나 찾을 수 없는 기록입니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException error) {
                Log.e("RecordDetail", "Unable to delete local ride", error);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "기록을 삭제하지 못했습니다. 저장공간을 확인해 주세요.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private boolean isLocalRecord() {
        return LocalRideStore.isLocalGpxReference(model.my_rec_gpx.getValue());
    }
    private String firstNotEmpty(String... values) {
        for (String value : values) {
            if (value != null && value.trim().length() > 0) {
                return value.trim();
            }
        }
        return "";
    }

    private void setTextIfPresent(TextView textView, String value) {
        if (textView != null) {
            textView.setText(value);
        }
    }

    private String formatDate(String value) {
        Date date = parseDate(value);
        if (date != null) {
            return new SimpleDateFormat("yyyy. MM. dd (E)", Locale.KOREA).format(date);
        }
        return value == null ? "-" : value;
    }

    private String formatClock(String value) {
        Date date = parseDate(value);
        if (date != null) {
            return new SimpleDateFormat("a h:mm", Locale.KOREA).format(date);
        }
        return "-";
    }

    private Date parseDate(String value) {
        if (value == null || value.length() < 10) {
            return null;
        }

        String normalized = value.length() >= 19 ? value.substring(0, 19) : value;
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd"};
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern, Locale.KOREA).parse(normalized);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private void showMoreMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, "공유하기");
        menu.getMenu().add(0, 3, 1, "GPX 내보내기");
        menu.getMenu().add(0, 2, 2, "삭제하기");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                shareRecord();
            } else if (item.getItemId() == 3) {
                exportGpx();
            } else {
                showDeleteConfirmation();
            }
            return true;
        });
        menu.show();
    }

    /** Shows the real GPX altitude graph, or an honest empty state saying why there is none. */
    private void renderElevationState() {
        if (detailElevationEmpty == null || elevationChart == null) {
            return;
        }
        String emptyMessage = null;
        if (elevationProfile != null && elevationProfile.isAvailable()) {
            showElevationChart(elevationProfile);
        } else if (elevationProfile == null && waitingForGpx) {
            emptyMessage = "고도 데이터를 불러오는 중입니다";
        } else if (gpxRequestFailed) {
            emptyMessage = "GPX를 불러오지 못해 고도 그래프를 표시할 수 없습니다";
        } else if (elevationProfile == null) {
            emptyMessage = "이 기록에는 GPX 고도 정보가 없습니다";
        } else {
            // Same wording as the save screen (ElevationProfile.Availability).
            emptyMessage = elevationProfile.getAvailability().message;
        }

        if (emptyMessage != null) {
            elevationChart.setVisibility(View.GONE);
            detailGraphLegend.setVisibility(View.GONE);
            detailGraphCaption.setVisibility(View.GONE);
            detailElevationEmpty.setText(emptyMessage);
            detailElevationEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void showElevationChart(ElevationProfile profile) {
        ElevationChartBinder.draw(elevationChart, profile);

        setTextIfPresent(detailGraphHigh, "▲ 최고 " + RecordFormatter.formatElevation(profile.getMaxElevation()));
        setTextIfPresent(detailGraphLow, "▼ 최저 " + RecordFormatter.formatElevation(profile.getMinElevation()));

        detailElevationEmpty.setVisibility(View.GONE);
        elevationChart.setVisibility(View.VISIBLE);
        detailGraphLegend.setVisibility(View.VISIBLE);
        detailGraphCaption.setVisibility(View.VISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mediaSection != null) {
            mediaSection.onActivityResult(requestCode, resultCode, data);
        }
        if (requestCode == REQUEST_EXPORT_GPX && resultCode == android.app.Activity.RESULT_OK
                && data != null && data.getData() != null) {
            writeGpxTo(data.getData());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.resume();
        }
        setAppBarVisible(false);
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.pause();
        }
        super.onPause();
        setAppBarVisible(true);
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            mapView.finish();
            mapView = null;
        }
        kakaoMap = null;
        routeLine = null;
        routeLineLayer = null;
        markerLayer = null;
        startMarker = null;
        finishMarker = null;
        routePoints.clear();
        elevationChart = null;
        detailElevationEmpty = null;
        if (mediaSection != null) {
            mediaSection.destroy();
            mediaSection = null;
        }
        super.onDestroyView();
    }

    private void setAppBarVisible(boolean visible) {
        View appBar = requireActivity().findViewById(R.id.app_bar_layout);
        if (appBar != null) {
            appBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

}
