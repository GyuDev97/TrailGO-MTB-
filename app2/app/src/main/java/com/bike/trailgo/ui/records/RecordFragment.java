package com.bike.trailgo.ui.records;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bike.trailgo.R;
import com.bike.trailgo.data.LocalRideStore;
import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.setting.AccountSession;
import com.bike.trailgo.ui.BaseFragment;
import com.bike.trailgo.ui.recycler.MyReportAdapter;
import com.bike.trailgo.ui.riding.StartActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecordFragment extends BaseFragment {
    private Call<ArrayList<RidingRecord>> request;
    private RecyclerView recyclerView;
    private TextView countText;
    private TextView totalDistanceText;
    private TextView totalHeightText;
    private TextView totalTimeText;
    private View emptyState;
    private TextView emptyTitle;
    private TextView emptyDescription;
    private TextView emptyAction;
    private boolean recordsUnavailable;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        countText = view.findViewById(R.id.records_count_text);
        totalDistanceText = view.findViewById(R.id.records_total_distance);
        totalHeightText = view.findViewById(R.id.records_total_height);
        totalTimeText = view.findViewById(R.id.records_total_time);
        emptyState = view.findViewById(R.id.records_empty_state);
        emptyTitle = view.findViewById(R.id.records_empty_title);
        emptyDescription = view.findViewById(R.id.records_empty_description);
        emptyAction = view.findViewById(R.id.records_empty_action);
        emptyAction.setOnClickListener(v -> {
            if (recordsUnavailable) {
                loadRecords();
            } else {
                startActivity(new Intent(requireContext(), StartActivity.class));
            }
        });

        bindHeader(view);
        bindBottomNavigation(view);
        loadRecords();
    }

    private void bindHeader(View view) {
        View menuButton = view.findViewById(R.id.records_menu_button);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> {
                DrawerLayout drawerLayout = requireActivity().findViewById(R.id.drawer_layout);
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }

    private void bindBottomNavigation(View view) {
        view.findViewById(R.id.bottom_nav_home).setOnClickListener(v -> navigateTo(R.id.nav_home));
        view.findViewById(R.id.bottom_nav_course).setOnClickListener(v -> navigateTo(R.id.nav_course_view));
        view.findViewById(R.id.bottom_nav_profile).setOnClickListener(v ->
                navigateTo(AccountSession.isGuest(requireContext()) ? R.id.app_settings : R.id.setting));
        view.findViewById(R.id.bottom_nav_ride).setOnClickListener(
                v -> startActivity(new Intent(requireContext(), StartActivity.class))
        );
        TextView records = view.findViewById(R.id.bottom_nav_records);
        records.setTextColor(android.graphics.Color.parseColor("#1F7A2E"));
        records.setSelected(true);
        records.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void navigateTo(int destinationId) {
        if (controller.getCurrentDestination() == null || controller.getCurrentDestination().getId() != destinationId) {
            controller.navigate(destinationId);
        }
    }

    private void loadRecords() {
        if (AccountSession.isGuest(requireContext())) {
            ArrayList<RidingRecord> localRecords = LocalRideStore.load(requireContext());
            if (localRecords.isEmpty()) {
                showEmptyState(false);
            } else {
                showRecords(localRecords);
            }
            return;
        }
        if (request != null) {
            request.cancel();
        }
        request = serverApi.getRecord(getRiderId());
        request.enqueue(new Callback<ArrayList<RidingRecord>>() {
            @Override
            public void onResponse(Call<ArrayList<RidingRecord>> call, Response<ArrayList<RidingRecord>> response) {
                if (!isAdded() || getView() == null || recyclerView == null) {
                    return;
                }

                if (response.code() == 200 && response.body() != null) {
                    ArrayList<RidingRecord> itemList = new ArrayList<>(response.body());
                    Collections.sort(itemList, new Comparator<RidingRecord>() {
                        @Override
                        public int compare(RidingRecord left, RidingRecord right) {
                            String leftDate = left.getRr_date() == null ? "" : left.getRr_date();
                            String rightDate = right.getRr_date() == null ? "" : right.getRr_date();
                            return rightDate.compareTo(leftDate);
                        }
                    });

                    if (itemList.isEmpty()) {
                        showEmptyState(false);
                    } else {
                        showRecords(itemList);
                    }
                } else {
                    Log.d("RecordFragment", "record response failed: " + response.code());
                    showEmptyState(true);
                }
            }

            @Override
            public void onFailure(Call<ArrayList<RidingRecord>> call, Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                t.printStackTrace();
                Log.d("RecordFragment", "record request failed");
                if (isAdded() && getView() != null && recyclerView != null) {
                    showEmptyState(true);
                }
            }
        });
    }

    private void showRecords(ArrayList<RidingRecord> records) {
        emptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        bindSummary(records);
        MyReportAdapter adapter = new MyReportAdapter(
                requireContext().getApplicationContext(),
                records,
                controller,
                model
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void showEmptyState(boolean unavailable) {
        recordsUnavailable = unavailable;
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        if (unavailable) {
            emptyTitle.setText("기록을 불러오지 못했습니다");
            emptyDescription.setText("네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
            emptyAction.setText("다시 시도");
        } else {
            emptyTitle.setText("저장된 주행 기록이 없습니다");
            emptyDescription.setText("첫 라이딩을 완료하고 저장하면 여기에 표시됩니다.");
            emptyAction.setText("주행 시작");
        }
        bindSummary(new ArrayList<>());
    }

    private String getRiderId() {
        String riderId = model.r_Id.getValue();
        if (riderId != null && riderId.length() > 0) {
            return riderId;
        }

        SharedPreferences auto = requireContext().getSharedPreferences("auto", Activity.MODE_PRIVATE);
        return auto.getString("LoginId", "");
    }

    private void bindSummary(ArrayList<RidingRecord> records) {
        int totalDistance = 0;
        int totalHeight = 0;
        int totalSeconds = 0;

        for (RidingRecord record : records) {
            totalDistance += record.getRr_distance();
            totalHeight += record.getRr_high();
            totalSeconds += record.getRr_time();
        }

        countText.setText("총 " + records.size() + "개의 라이딩 기록");
        totalDistanceText.setText(String.format(Locale.KOREA, "%.1f km", totalDistance / 1000.0));
        totalHeightText.setText(String.format(Locale.KOREA, "%,d m", totalHeight));
        totalTimeText.setText(formatTotalTime(totalSeconds));
    }

    private String formatTotalTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "시간 " + minutes + "분";
        }
        return minutes + "분";
    }

    @Override
    public void onResume() {
        super.onResume();
        setAppBarVisible(false);
        if (recyclerView != null) {
            loadRecords();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void setAppBarVisible(boolean visible) {
        View appBar = requireActivity().findViewById(R.id.app_bar_layout);
        if (appBar != null) {
            appBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
