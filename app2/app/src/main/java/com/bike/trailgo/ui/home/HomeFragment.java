package com.bike.trailgo.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.Navigation;

import com.bike.trailgo.R;
import com.bike.trailgo.data.LocalRideStore;
import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.setting.AccountSession;
import com.bike.trailgo.ui.BaseFragment;
import com.bike.trailgo.ui.records.RecordFormatter;
import com.bike.trailgo.ui.riding.StartActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Home: this month's rides (count, distance, climbing, time) and the latest rides.
 *
 * <p>Members read their rides from the server, guests from the device; both end up as the same
 * list of {@link RidingRecord}. Nothing is invented: with no rides the screen says so, and while
 * loading or after a failure the numbers show "-" instead of zeros.</p>
 */
public class HomeFragment extends BaseFragment {
    private static final int RECENT_COUNT = 3;
    private static final float UNIT_SCALE = 0.62f;

    private enum State { LOADING, READY, ERROR }

    private Call<List<RidingRecord>> myInfo;
    private final Handler main = new Handler(Looper.getMainLooper());
    private int loadGeneration;
    private boolean hasData;

    private TextView monthLabel;
    private TextView monthCount;
    private TextView statCount;
    private TextView statDistance;
    private TextView statHeight;
    private TextView statTime;
    private TextView sourceNote;
    private LinearLayout recentRecordList;

    private int savedStatusBarColor;
    private int savedSystemUiVisibility;
    private boolean statusBarStyled;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_redesign, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        monthLabel = view.findViewById(R.id.home_month_label);
        monthCount = view.findViewById(R.id.home_month_count);
        statCount = view.findViewById(R.id.home_stat_count);
        statDistance = view.findViewById(R.id.main_dis);
        statHeight = view.findViewById(R.id.home_total_height);
        statTime = view.findViewById(R.id.home_total_time);
        sourceNote = view.findViewById(R.id.home_source_note);
        recentRecordList = view.findViewById(R.id.home_recent_list);

        bindNavigation(view);
        bindHeader(view);
        bindStartButtons(view);
        hasData = false;
        showState(State.LOADING, null);
        // the records are (re)loaded in onResume, which always follows
    }

    // ---------------------------------------------------------------------------------------
    // Header / navigation (same actions as before)
    // ---------------------------------------------------------------------------------------

    private String getRiderId() {
        String riderId = model.r_Id.getValue();
        if (riderId != null && riderId.length() > 0) {
            return riderId;
        }
        SharedPreferences auto = requireContext().getSharedPreferences("auto", Activity.MODE_PRIVATE);
        return auto.getString("LoginId", "");
    }

    private void bindNavigation(View view) {
        setNavigationClick(view, R.id.home_recent_more, R.id.action_nav_home_to_nav_records);
        setNavigationClick(view, R.id.bottom_nav_records, R.id.action_nav_home_to_nav_records);
        setNavigationClick(view, R.id.bottom_nav_course, R.id.action_nav_home_to_nav_course_view);
        View profile = view.findViewById(R.id.bottom_nav_profile);
        if (profile != null) {
            profile.setOnClickListener(v -> {
                int target = AccountSession.isGuest(requireContext()) ? R.id.app_settings : R.id.setting;
                if (controller.getCurrentDestination() == null || controller.getCurrentDestination().getId() != target) {
                    controller.navigate(target);
                }
            });
        }
        TextView home = view.findViewById(R.id.bottom_nav_home);
        if (home != null) {
            home.setSelected(true);
            home.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void setNavigationClick(View root, int viewId, int actionId) {
        View item = root.findViewById(viewId);
        if (item != null) {
            item.setOnClickListener(v -> navigate(v, actionId));
        }
    }

    private void bindHeader(View view) {
        View.OnClickListener openDrawer = v -> {
            DrawerLayout drawerLayout = requireActivity().findViewById(R.id.drawer_layout);
            if (drawerLayout != null) {
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
            }
        };
        View icon = view.findViewById(R.id.home_menu_icon);
        if (icon != null) {
            icon.setOnClickListener(openDrawer);
        }
        View title = view.findViewById(R.id.home_menu_button);
        if (title != null) {
            title.setOnClickListener(openDrawer);
        }
    }

    /** The round button of the bottom bar and the quick-start card do exactly the same thing. */
    private void bindStartButtons(View view) {
        View.OnClickListener start = v -> startActivity(new Intent(getActivity(), StartActivity.class));
        View ride = view.findViewById(R.id.bottom_nav_ride);
        if (ride != null) {
            ride.setOnClickListener(start);
        }
        View quick = view.findViewById(R.id.home_quick_start);
        if (quick != null) {
            quick.setOnClickListener(start);
        }
    }

    private void navigate(View view, int actionId) {
        Navigation.findNavController(view).navigate(actionId);
    }

    // ---------------------------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------------------------

    private void loadRidingSummary() {
        final int generation = ++loadGeneration;
        if (myInfo != null) {
            myInfo.cancel();
            myInfo = null;
        }
        if (!hasData) {
            showState(State.LOADING, null);
        }

        if (AccountSession.isGuest(requireContext())) {
            final android.content.Context context = requireContext().getApplicationContext();
            new Thread(() -> {
                final ArrayList<RidingRecord> records = LocalRideStore.load(context);
                main.post(() -> {
                    if (generation == loadGeneration && isAdded() && getView() != null) {
                        onRecords(records, true);
                    }
                });
            }).start();
            return;
        }

        myInfo = serverApi.getMyRecord(getRiderId());
        myInfo.enqueue(new Callback<List<RidingRecord>>() {
            @Override
            public void onResponse(Call<List<RidingRecord>> call, Response<List<RidingRecord>> response) {
                if (generation != loadGeneration || !isAdded() || getView() == null) {
                    return;
                }
                if (response.code() == 200 && response.body() != null) {
                    onRecords(response.body(), false);
                } else {
                    onLoadFailed(false);
                }
            }

            @Override
            public void onFailure(Call<List<RidingRecord>> call, Throwable t) {
                if (!call.isCanceled() && generation == loadGeneration && isAdded() && getView() != null) {
                    onLoadFailed(true);
                }
            }
        });
    }

    private void onLoadFailed(boolean networkProblem) {
        if (hasData) {
            // keep showing what is already there, and say it may be out of date
            sourceNote.setText("최신 기록을 불러오지 못해 이전에 불러온 내용을 보여드립니다.");
            sourceNote.setVisibility(View.VISIBLE);
        } else {
            showState(State.ERROR, networkProblem ? "서버에 연결하지 못했습니다. 네트워크 연결을 확인해 주세요."
                    : "기록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    // ---------------------------------------------------------------------------------------
    // Showing
    // ---------------------------------------------------------------------------------------

    private void onRecords(List<RidingRecord> records, boolean guest) {
        hasData = true;
        // Guest rides carry the phone's local time, member rides the server's (Korea) time.
        TimeZone zone = guest ? TimeZone.getDefault() : RecordDates.SEOUL;
        int month = RecordDates.currentMonthKey(zone, System.currentTimeMillis());
        MonthlySummary summary = MonthlySummary.of(records, month, zone);

        monthLabel.setText(HomeFormat.monthLabel(month));
        HomeUnitText.set(monthCount, HomeFormat.count(summary.rideCount), 0.45f);
        HomeUnitText.set(statCount, HomeFormat.count(summary.rideCount), UNIT_SCALE);
        HomeUnitText.set(statDistance, HomeFormat.distance(summary.distanceMeters), UNIT_SCALE);
        HomeUnitText.set(statHeight, HomeFormat.elevation(summary.elevationGainMeters), UNIT_SCALE);
        HomeUnitText.set(statTime, HomeFormat.duration(summary.ridingSeconds), UNIT_SCALE);

        sourceNote.setVisibility(View.GONE);

        List<RidingRecord> sorted = sortNewestFirst(records, zone);
        bindRecentRecords(sorted, zone);
        if (guest) {
            Set<String> references = new HashSet<>();
            for (RidingRecord record : sorted) {
                if (record.getRr_gpx() != null) {
                    references.add(record.getRr_gpx());
                }
            }
            // pictures of deleted guest rides are no longer needed
            RouteThumbnailLoader.get(requireContext()).retainOnly(references);
        }
    }

    private void showState(State state, @Nullable String message) {
        if (state == State.LOADING) {
            TimeZone zone = AccountSession.isGuest(requireContext()) ? TimeZone.getDefault() : RecordDates.SEOUL;
            monthLabel.setText(HomeFormat.monthLabel(RecordDates.currentMonthKey(zone, System.currentTimeMillis())));
        }
        if (state != State.READY) {
            monthCount.setText("-");
            HomeUnitText.clear(statCount);
            HomeUnitText.clear(statDistance);
            HomeUnitText.clear(statHeight);
            HomeUnitText.clear(statTime);
            sourceNote.setVisibility(View.GONE);
            recentRecordList.removeAllViews();
            if (state == State.LOADING) {
                addMessage("기록을 불러오는 중입니다", null, false);
            } else {
                addMessage("기록을 불러오지 못했습니다", message, true);
            }
        }
    }

    private void addMessage(String title, @Nullable String detail, boolean retry) {
        View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_home_message, recentRecordList, false);
        ((TextView) card.findViewById(R.id.home_message_title)).setText(title);
        TextView detailView = card.findViewById(R.id.home_message_detail);
        if (detail == null || detail.isEmpty()) {
            detailView.setVisibility(View.GONE);
        } else {
            detailView.setText(detail);
        }
        View retryView = card.findViewById(R.id.home_message_retry);
        if (retry) {
            retryView.setVisibility(View.VISIBLE);
            retryView.setOnClickListener(v -> loadRidingSummary());
        }
        recentRecordList.addView(card);
    }

    private List<RidingRecord> sortNewestFirst(List<RidingRecord> records, final TimeZone zone) {
        ArrayList<RidingRecord> sorted = new ArrayList<>();
        if (records != null) {
            for (RidingRecord record : records) {
                if (record != null) {
                    sorted.add(record);
                }
            }
        }
        Collections.sort(sorted, new Comparator<RidingRecord>() {
            @Override
            public int compare(RidingRecord left, RidingRecord right) {
                return Long.compare(sortKey(right, zone), sortKey(left, zone));
            }
        });
        return sorted;
    }

    private static long sortKey(RidingRecord record, TimeZone zone) {
        RecordDates.Parts p = RecordDates.parse(record.getRr_date(), zone);
        if (p == null) {
            return Long.MIN_VALUE;
        }
        return (((p.year * 100L + p.month) * 100L + p.day) * 100L + Math.max(0, p.hour)) * 100L + p.minute;
    }

    private void bindRecentRecords(List<RidingRecord> records, TimeZone zone) {
        recentRecordList.removeAllViews();
        if (records.isEmpty()) {
            addMessage("아직 라이딩 기록이 없어요", "첫 라이딩을 저장하면 여기에 표시됩니다.", false);
            return;
        }
        int count = Math.min(RECENT_COUNT, records.size());
        for (int index = 0; index < count; index++) {
            addRecentRecordRow(records.get(index), zone);
        }
    }

    private void addRecentRecordRow(final RidingRecord record, TimeZone zone) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_home_recent_record, recentRecordList, false);
        ((TextView) row.findViewById(R.id.home_recent_item_course)).setText(HomeFormat.title(record));
        ((TextView) row.findViewById(R.id.home_recent_item_date)).setText(HomeFormat.dateAndTime(record.getRr_date(), zone));
        ((TextView) row.findViewById(R.id.home_recent_item_distance)).setText(HomeFormat.rowDistance(record));
        ((TextView) row.findViewById(R.id.home_recent_item_height)).setText(HomeFormat.rowElevation(record));
        ((TextView) row.findViewById(R.id.home_recent_item_duration)).setText(HomeFormat.rowDuration(record));
        row.setContentDescription(HomeFormat.title(record) + ", " + HomeFormat.dateAndTime(record.getRr_date(), zone)
                + ", 거리 " + HomeFormat.rowDistance(record) + ", 획득 고도 " + HomeFormat.rowElevation(record)
                + ", 시간 " + HomeFormat.rowDuration(record));
        row.setOnClickListener(v -> openRecordDetail(record));
        bindThumbnail((ImageView) row.findViewById(R.id.home_recent_item_thumb), record.getRr_gpx());
        recentRecordList.addView(row);
    }

    /** The route picture is made once from the ride's GPX, off the main thread, and cached. */
    private void bindThumbnail(final ImageView thumb, @Nullable final String gpxReference) {
        final float density = getResources().getDisplayMetrics().density;
        int widthPx = Math.round(84 * density);
        int heightPx = Math.round(64 * density);
        thumb.setImageResource(R.drawable.home_thumb_placeholder);
        thumb.setTag(gpxReference);
        RouteThumbnailLoader.get(requireContext()).load(gpxReference, widthPx, heightPx, density,
                (bitmap, result) -> {
                    boolean stillWanted = thumb.isAttachedToWindow() || thumb.getParent() != null;
                    String wanted = (String) thumb.getTag();
                    boolean same = wanted == null ? gpxReference == null : wanted.equals(gpxReference);
                    if (bitmap != null && stillWanted && same) {
                        thumb.setImageBitmap(bitmap);
                    }
                    // no GPX / failed download / unreadable file: the neutral placeholder stays
                });
    }

    private void openRecordDetail(RidingRecord record) {
        model.r_num.setValue(String.valueOf(record.getRr_num()));
        model.my_rec_name.setValue(record.getRr_name());
        model.my_rec_date.setValue(record.getRr_date());
        model.my_rec_adress.setValue(record.getRr_area());
        // Same display rules as the records list so both entrances show identical detail values.
        model.my_rec_dis.setValue(RecordFormatter.formatDistance(record.getRr_distance()));
        model.my_rec_get.setValue(RecordFormatter.formatElevationGain(record.getRr_high()));
        model.my_rec_time.setValue(RecordFormatter.formatDuration(record.getRr_time()));
        model.my_rec_rest.setValue(RecordFormatter.formatDuration(record.getRr_breaktime()));
        model.my_rec_open.setValue(record.getRr_open());
        model.my_rec_max.setValue(RecordFormatter.formatSpeed(
                record.getRr_topspeed(), record.getRr_distance(), record.getRr_time()));
        model.my_rec_avg.setValue(RecordFormatter.formatSpeed(
                record.getRr_avgspeed(), record.getRr_distance(), record.getRr_time()));
        model.my_rec_gpx.setValue(record.getRr_gpx());
        controller.navigate(R.id.action_nav_home_to_myDetailFragment);
    }

    // ---------------------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------------------

    @Override
    public void onResume() {
        super.onResume();
        setAppBarVisible(false);
        styleStatusBar(true);
        if (getView() != null) {
            loadRidingSummary();
        }
    }

    @Override
    public void onPause() {
        styleStatusBar(false);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        loadGeneration++;
        if (myInfo != null) {
            myInfo.cancel();
            myInfo = null;
        }
        super.onDestroyView();
    }

    /** The dark green header runs up under the status bar while Home is shown. */
    @SuppressWarnings("deprecation")
    private void styleStatusBar(boolean home) {
        android.view.Window window = requireActivity().getWindow();
        if (window == null) {
            return;
        }
        if (home && !statusBarStyled) {
            savedStatusBarColor = window.getStatusBarColor();
            savedSystemUiVisibility = window.getDecorView().getSystemUiVisibility();
            window.setStatusBarColor(0xFF123425);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(
                        savedSystemUiVisibility & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
            statusBarStyled = true;
        } else if (!home && statusBarStyled) {
            window.setStatusBarColor(savedStatusBarColor);
            window.getDecorView().setSystemUiVisibility(savedSystemUiVisibility);
            statusBarStyled = false;
        }
    }

    private void setAppBarVisible(boolean visible) {
        View appBar = requireActivity().findViewById(R.id.app_bar_layout);
        if (appBar != null) {
            appBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
