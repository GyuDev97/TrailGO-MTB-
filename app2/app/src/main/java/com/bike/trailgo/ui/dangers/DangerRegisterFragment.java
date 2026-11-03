package com.bike.trailgo.ui.dangers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.bike.trailgo.R;
import com.bike.trailgo.hazard.Hazard;
import com.bike.trailgo.hazard.HazardApiError;
import com.bike.trailgo.hazard.HazardReport;
import com.bike.trailgo.hazard.HazardRepository;
import com.bike.trailgo.hazard.HazardType;
import com.bike.trailgo.setting.AccountSession;
import com.bike.trailgo.ui.BaseFragment;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.shape.DotPoints;
import com.kakao.vectormap.shape.Polygon;
import com.kakao.vectormap.shape.PolygonOptions;
import com.kakao.vectormap.shape.PolygonStyles;
import com.kakao.vectormap.shape.ShapeLayer;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Report a hazard zone: move the map so the centre mark sits on the hazard, pick a type, give it a title and
 * a radius. Members only (the server knows who reports from the sign-in token). Nothing the rider typed is
 * cleared when the server refuses or the network fails.
 */
public class DangerRegisterFragment extends BaseFragment {
    private static final String TAG = "DangerRegister";
    private static final int RADIUS_STEP_M = 5;
    // default start (Daegu, as the riding map) when neither a location nor a cached zone is known
    private static final double DEFAULT_LAT = 35.89617906303501;
    private static final double DEFAULT_LON = 128.62171907592318;

    private MapView mapView;
    private KakaoMap kakaoMap;
    private ShapeLayer previewLayer;
    private Polygon previewCircle;
    private TextView mapStatus;
    private TextView radiusLabel;
    private SeekBar radiusBar;
    private EditText titleEdit;
    private EditText descriptionEdit;
    private ChipGroup typeGroup;
    private Button submit;
    private Call<Hazard> pending;
    private boolean submitting;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_danger_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!AccountSession.requireMember(requireActivity(), "위험지역 제보")) {
            controller.popBackStack();
            return;
        }
        mapView = view.findViewById(R.id.hazard_map);
        mapStatus = view.findViewById(R.id.hazard_map_status);
        radiusLabel = view.findViewById(R.id.hazard_radius_label);
        radiusBar = view.findViewById(R.id.hazard_radius);
        titleEdit = view.findViewById(R.id.hazard_title);
        descriptionEdit = view.findViewById(R.id.hazard_description);
        typeGroup = view.findViewById(R.id.hazard_type_group);
        submit = view.findViewById(R.id.hazard_submit);

        buildTypeChips();
        updateRadiusLabel();
        radiusBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateRadiusLabel();
                drawPreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        // the map pans inside a scrolling form: while a finger is on it the form must not scroll
        mapView.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            v.getParent().requestDisallowInterceptTouchEvent(
                    action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL);
            return false;
        });
        view.findViewById(R.id.hazard_my_location).setOnClickListener(v -> moveToMyLocation());
        submit.setOnClickListener(v -> submit());
        startMap();
    }

    private void buildTypeChips() {
        for (HazardType type : HazardType.values()) {
            Chip chip = new Chip(requireContext());
            chip.setText(type.glyph + " " + type.label);
            chip.setCheckable(true);
            chip.setTag(type);
            chip.setId(View.generateViewId());
            typeGroup.addView(chip);
        }
    }

    private HazardType selectedType() {
        int id = typeGroup.getCheckedChipId();
        if (id == View.NO_ID) return null;
        View chip = typeGroup.findViewById(id);
        return chip != null && chip.getTag() instanceof HazardType ? (HazardType) chip.getTag() : null;
    }

    private int radiusMeters() {
        return HazardReport.MIN_RADIUS_M + radiusBar.getProgress() * RADIUS_STEP_M;
    }

    private void updateRadiusLabel() {
        radiusLabel.setText("위험 반경 " + radiusMeters() + "m");
    }

    // ---- map ----------------------------------------------------------------------------------------------

    private void startMap() {
        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                Log.d(TAG, "map destroyed");
            }

            @Override
            public void onMapError(Exception error) {
                Log.e(TAG, "map error", error);
                if (mapStatus != null) {
                    mapStatus.setVisibility(View.VISIBLE);
                    mapStatus.setText("지도를 불러오지 못했습니다.\n카카오 앱 키와 키 해시를 확인해 주세요.");
                }
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap map) {
                kakaoMap = map;
                mapStatus.setVisibility(View.GONE);
                previewLayer = map.getShapeManager().getLayer();
                map.setOnCameraMoveEndListener((m, position, gestureType) -> drawPreview());
                drawPreview();
                if (hasLocationPermission()) moveToMyLocation();
            }

            @Override
            public LatLng getPosition() {
                return startPosition();
            }

            @Override
            public int getZoomLevel() {
                return 16;
            }
        });
    }

    /** A fixed starting city; the rider's real position replaces it as soon as it is known. */
    private LatLng startPosition() {
        return LatLng.from(DEFAULT_LAT, DEFAULT_LON);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void moveToMyLocation() {
        if (!hasLocationPermission()) {
            Toast.makeText(requireContext(), "현재 위치를 쓰려면 위치 권한이 필요합니다. 지도를 직접 움직여 주세요.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            LocationServices.getFusedLocationProviderClient(requireContext()).getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location == null || kakaoMap == null || !isAdded()) {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "현재 위치를 아직 알 수 없습니다. 지도를 직접 움직여 주세요.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            return;
                        }
                        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(
                                LatLng.from(location.getLatitude(), location.getLongitude())));
                    })
                    .addOnFailureListener(error -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "현재 위치를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (SecurityException error) {
            Log.w(TAG, "location permission revoked", error);
        }
    }

    /** The radius preview is redrawn around the map centre; the previous circle is removed, never left behind. */
    private void drawPreview() {
        if (kakaoMap == null || previewLayer == null) return;
        try {
            if (previewCircle != null) {
                previewLayer.remove(previewCircle);
                previewCircle = null;
            }
            LatLng center = kakaoMap.getCameraPosition().getPosition();
            previewCircle = previewLayer.addPolygon(PolygonOptions.from(
                    DotPoints.fromCircle(center, radiusMeters()),
                    PolygonStyles.from(0x40FF8F00, 2f, 0xFFE65100)));
        } catch (RuntimeException error) {
            Log.w(TAG, "radius preview skipped", error);
        }
    }

    // ---- submitting ---------------------------------------------------------------------------------------

    private void submit() {
        if (submitting) return; // a second tap while the first request is running
        LatLng center = kakaoMap == null ? null : kakaoMap.getCameraPosition().getPosition();
        HazardType type = selectedType();
        String title = titleEdit.getText().toString().trim();
        String description = descriptionEdit.getText().toString().trim();
        String problem = HazardReport.validate(
                center == null ? null : center.getLatitude(), center == null ? null : center.getLongitude(),
                type, title, description, radiusMeters());
        if (problem != null) {
            Toast.makeText(requireContext(), problem, Toast.LENGTH_SHORT).show();
            return;
        }
        HazardReport report = new HazardReport();
        report.latitude = center.getLatitude();
        report.longitude = center.getLongitude();
        report.radiusM = radiusMeters();
        report.type = type.code();
        report.title = title;
        report.description = description;

        setSubmitting(true);
        pending = serverApi.reportHazard(report);
        pending.enqueue(new Callback<Hazard>() {
            @Override
            public void onResponse(Call<Hazard> call, Response<Hazard> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    HazardRepository.get(requireContext()).onReported(response.body());
                    Toast.makeText(requireContext(),
                            "제보가 접수되었습니다. 운영자가 확인하기 전까지 '미검증'으로 표시됩니다.", Toast.LENGTH_LONG).show();
                    controller.popBackStack();
                    return;
                }
                showFailure(HazardApiError.parse(response.code(), errorBody(response)));
            }

            @Override
            public void onFailure(Call<Hazard> call, Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                showFailure(HazardApiError.parse(0, null));
                Log.w(TAG, "report failed", t);
            }
        });
    }

    private static String errorBody(Response<?> response) {
        try {
            return response.errorBody() == null ? null : response.errorBody().string();
        } catch (IOException e) {
            return null;
        }
    }

    /** The form stays exactly as typed so the rider can fix a problem and send again. */
    private void showFailure(HazardApiError error) {
        setSubmitting(false);
        new AlertDialog.Builder(requireContext())
                .setTitle("제보하지 못했습니다")
                .setMessage(error.httpStatus == 0
                        ? "서버에 연결할 수 없습니다. 입력한 내용은 그대로 두었으니 연결을 확인한 뒤 다시 시도해 주세요."
                        : error.userMessage())
                .setPositiveButton("확인", null)
                .show();
    }

    private void setSubmitting(boolean value) {
        submitting = value;
        submit.setEnabled(!value);
        submit.setText(value ? "제보하는 중…" : "제보하기");
    }

    // ---- map lifecycle ---------------------------------------------------------------------------------------

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.resume();
    }

    @Override
    public void onPause() {
        if (mapView != null) mapView.pause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (pending != null) pending.cancel();
        if (mapView != null) {
            mapView.finish();
            mapView = null;
        }
        kakaoMap = null;
        previewLayer = null;
        previewCircle = null;
        super.onDestroyView();
    }
}
