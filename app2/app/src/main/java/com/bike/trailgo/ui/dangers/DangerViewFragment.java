package com.bike.trailgo.ui.dangers;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bike.trailgo.Adapter.DangerAdapter;
import com.bike.trailgo.R;
import com.bike.trailgo.dto.DangerousArea;
import com.bike.trailgo.hazard.HazardDetailDialog;
import com.bike.trailgo.hazard.HazardMapLayer;
import com.bike.trailgo.hazard.HazardRefreshPolicy;
import com.bike.trailgo.hazard.HazardRepository;
import com.bike.trailgo.setting.RideSettings;
import com.bike.trailgo.ui.BaseFragment;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraPosition;
import com.kakao.vectormap.camera.CameraUpdateFactory;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Hazard zones on the map (tap a marker for its card) and the rider's own report history below. */
public class DangerViewFragment extends BaseFragment {
    private static final String TAG = "DangerView";
    private static final double DEFAULT_LAT = 35.89617906303501;
    private static final double DEFAULT_LON = 128.62171907592318;

    private MapView mapView;
    private KakaoMap kakaoMap;
    private HazardMapLayer hazardLayer;
    private HazardRepository repository;
    private final HazardRefreshPolicy mapPolicy = new HazardRefreshPolicy();
    private TextView mapStatus;
    private RecyclerView recyclerView;
    private Call<List<DangerousArea>> getDanger;
    private final HazardRepository.Listener listener = hazards -> render();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_danger_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = HazardRepository.get(requireContext());
        mapView = view.findViewById(R.id.hazard_view_map);
        mapStatus = view.findViewById(R.id.hazard_view_status);
        recyclerView = view.findViewById(R.id.recycle_danger);
        Button registerButton = view.findViewById(R.id.button3);

        mapView.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            v.getParent().requestDisallowInterceptTouchEvent(
                    action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL);
            return false;
        });
        startMap();
        loadMyReports();
        registerButton.setOnClickListener(v ->
                controller.navigate(R.id.action_nav_danger_view_to_dangerRegisterFragment));
    }

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
                    mapStatus.setText("지도를 불러오지 못했습니다.");
                }
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap map) {
                kakaoMap = map;
                mapStatus.setVisibility(View.GONE);
                hazardLayer = new HazardMapLayer(requireContext(), map);
                hazardLayer.setOnHazardClickListener(hazard ->
                        HazardDetailDialog.show(requireContext(), hazard, DangerViewFragment.this::render));
                map.setOnCameraMoveEndListener((m, position, gestureType) -> {
                    refreshAround(position);
                    render();
                });
                repository.addListener(listener);
                render();
                refreshAround(map.getCameraPosition());
            }

            @Override
            public LatLng getPosition() {
                return LatLng.from(DEFAULT_LAT, DEFAULT_LON);
            }

            @Override
            public int getZoomLevel() {
                return 15;
            }
        });
    }

    /** Moving the map never asks the server more often than the shared refresh policy allows. */
    private void refreshAround(CameraPosition position) {
        if (position == null || position.getPosition() == null) return;
        repository.refreshIfDue(position.getPosition().getLatitude(), position.getPosition().getLongitude(),
                HazardRefreshPolicy.QUERY_RADIUS_M, mapPolicy, null);
    }

    private void render() {
        if (hazardLayer == null || kakaoMap == null || !isAdded()) return;
        if (!RideSettings.areHazardsShownOnMap(requireContext())) {
            hazardLayer.clear();
            return;
        }
        CameraPosition camera = kakaoMap.getCameraPosition();
        if (camera == null) return;
        hazardLayer.render(repository.cached(), camera.getPosition(), camera.getZoomLevel());
    }

    private void loadMyReports() {
        String riderId = model.r_Id.getValue();
        if (riderId == null || riderId.isEmpty()) return; // guests have no report history
        getDanger = serverApi.getDanger(riderId);
        getDanger.enqueue(new Callback<List<DangerousArea>>() {
            @Override
            public void onResponse(Call<List<DangerousArea>> call, Response<List<DangerousArea>> response) {
                List<DangerousArea> body = response.body();
                if (!isAdded() || !response.isSuccessful() || body == null) return;
                DangerAdapter adapter = new DangerAdapter(requireContext().getApplicationContext(),
                        new ArrayList<>(body), (lat, lon) -> {
                    if (kakaoMap != null) {
                        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lon)));
                    }
                });
                recyclerView.setLayoutManager(new LinearLayoutManager(
                        requireContext().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Call<List<DangerousArea>> call, Throwable t) {
                // the map still works; the history list just stays empty
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.resume();
        render();
    }

    @Override
    public void onPause() {
        if (mapView != null) mapView.pause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (getDanger != null) getDanger.cancel();
        if (repository != null) repository.removeListener(listener);
        if (hazardLayer != null) {
            hazardLayer.clear();
            hazardLayer = null;
        }
        if (mapView != null) {
            mapView.finish();
            mapView = null;
        }
        kakaoMap = null;
        super.onDestroyView();
    }
}
