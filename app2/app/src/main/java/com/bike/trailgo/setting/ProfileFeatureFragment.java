package com.bike.trailgo.setting;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bike.trailgo.R;
import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.ui.BaseFragment;
import com.bike.trailgo.ui.riding.StartActivity;

import java.util.ArrayList;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFeatureFragment extends BaseFragment {
    private static final int GREEN = Color.rgb(35, 139, 58);
    private static final int DARK_GREEN = Color.rgb(18, 89, 48);
    private static final int TEXT = Color.rgb(20, 26, 22);
    private static final int MUTED = Color.rgb(105, 113, 108);
    private LinearLayout content;
    private SharedPreferences prefs;
    private Call<ArrayList<RidingRecord>> recordsCall;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(R.layout.fragment_profile_feature, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        content = view.findViewById(R.id.profile_feature_content);
        prefs = requireContext().getSharedPreferences("profile_features", Activity.MODE_PRIVATE);
        String screen = getArguments() == null ? "stats" : getArguments().getString("screen", "stats");
        TextView title = view.findViewById(R.id.profile_feature_title);
        ImageButton action = view.findViewById(R.id.profile_feature_action);
        view.findViewById(R.id.profile_feature_back).setOnClickListener(v -> controller.navigateUp());

        View bottom = view.findViewById(R.id.profile_feature_bottom_nav);
        bottom.setVisibility("stats".equals(screen) ? View.VISIBLE : View.GONE);
        if ("stats".equals(screen)) bindBottomNavigation(view);

        switch (screen) {
            case "courses": title.setText("나의 코스"); buildCourses(); break;
            case "favorites": title.setText("즐겨찾기"); buildFavorites(); break;
            case "badges": title.setText("배지"); buildBadges(); break;
            case "gear": title.setText("장비 관리"); buildGear(); break;
            default:
                title.setText("활동 통계");
                action.setVisibility(View.VISIBLE);
                action.setOnClickListener(v -> Toast.makeText(requireContext(), "기간 선택은 준비 중입니다.", Toast.LENGTH_SHORT).show());
                buildStats();
                break;
        }
    }

    private void buildStats() {
        LinearLayout summary = card(0);
        summary.setOrientation(LinearLayout.HORIZONTAL);
        TextView rides = stat("총 라이딩", "42 회");
        TextView distance = stat("총 거리", "258.7 km");
        TextView elevation = stat("총 상승고도", "3,214 m");
        TextView time = stat("총 라이딩 시간", "18:42:30");
        summary.addView(rides, weighted());
        summary.addView(distance, weighted());
        summary.addView(elevation, weighted());
        summary.addView(time, weighted());
        addCard(summary);

        addChartCard("주간 라이딩", "5월 12일 - 5월 18일", false);
        addChartCard("월간 추이", "2025년 5월", true);
        addHeading("인사이트", null);
        LinearLayout insights = new LinearLayout(requireContext());
        insights.setOrientation(LinearLayout.HORIZONTAL);
        insights.addView(insight("평균 속도", "21.6 km/h", "전체 평균"), weightedWithMargins(0, 5));
        insights.addView(insight("최장 라이딩", "68.4 km", "2025.05.24"), weightedWithMargins(5, 5));
        insights.addView(insight("최다 라이딩 요일", "토요일", "총 9회"), weightedWithMargins(5, 0));
        content.addView(insights);
        loadStats(rides, distance, elevation, time);
    }

    private void loadStats(TextView rides, TextView distance, TextView elevation, TextView time) {
        String loginId = requireActivity().getSharedPreferences("auto", Activity.MODE_PRIVATE).getString("LoginId", "admin");
        recordsCall = serverApi.getRecord(loginId);
        recordsCall.enqueue(new Callback<ArrayList<RidingRecord>>() {
            @Override public void onResponse(Call<ArrayList<RidingRecord>> call, Response<ArrayList<RidingRecord>> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                int meters = 0, high = 0, seconds = 0;
                for (RidingRecord record : response.body()) {
                    meters += Math.max(0, record.getRr_distance());
                    high += Math.max(0, record.getRr_high());
                    seconds += Math.max(0, record.getRr_time());
                }
                setStatValue(rides, response.body().size() + " 회");
                setStatValue(distance, String.format(Locale.KOREA, "%.1f km", meters / 1000.0));
                setStatValue(elevation, String.format(Locale.KOREA, "%,d m", high));
                setStatValue(time, duration(seconds));
            }
            @Override public void onFailure(Call<ArrayList<RidingRecord>> call, Throwable error) { }
        });
    }

    private void setStatValue(TextView view, String value) {
        view.setText(view.getText().toString().split("\\n")[0] + "\n" + value);
    }

    private void addChartCard(String heading, String sub, boolean line) {
        LinearLayout box = card(dp(246));
        addCardHeader(box, heading, sub, "거리(km)");
        ChartView chart = new ChartView(requireContext(), line);
        box.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(178)));
        addCard(box);
    }

    private void buildCourses() {
        addTabs(new String[]{"전체", "작성한 코스", "저장한 코스"}, 0);
        addCourse("앞산 업힐 코스", "9.8 km", "620 m", "2시간 15분", "대구 · 앞산", true, 0);
        addCourse("용문산 둘레길", "12.4 km", "350 m", "3시간 10분", "경기 양평 · 용문산", false, 1);
        addCourse("청계산 숲길 코스", "7.5 km", "290 m", "1시간 50분", "서울 서초구 · 청계산", false, 2);
        addCourse("한강 자전거길 (난지~여의도)", "18.6 km", "120 m", "1시간 30분", "서울 · 한강", false, 3);
        addCourse("관악산 능선 코스", "10.2 km", "680 m", "2시간 40분", "서울 관악구 · 관악산", false, 4);
        Button add = primaryButton("＋  코스 추가");
        add.setOnClickListener(v -> controller.navigate(R.id.nav_course_view));
        LinearLayout.LayoutParams ap = fullMargins(34, 8, 34, 18);
        content.addView(add, ap);
    }

    private void addCourse(String name, String km, String high, String time, String tags, boolean recent, int seed) {
        LinearLayout row = card(dp(142));
        row.setOrientation(LinearLayout.HORIZONTAL);
        if (recent) row.setBackgroundResource(R.drawable.profile_feature_primary_card);
        MapThumbView map = new MapThumbView(requireContext(), seed);
        row.addView(map, new LinearLayout.LayoutParams(dp(126), ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(14), dp(4), 0, 0);
        if (recent) info.addView(labelChip("최근 이용", true));
        TextView nameView = text(name, 17, TEXT, true);
        info.addView(nameView);
        info.addView(text("⌖  " + km + "     △  " + high + "     ◷  " + time, 12, TEXT, false));
        TextView tagsView = text(tags, 11, MUTED, false);
        tagsView.setPadding(0, dp(10), 0, 0);
        info.addView(tagsView);
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        TextView menu = text("⋮", 23, MUTED, true);
        menu.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        row.addView(menu, new LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.MATCH_PARENT));
        addCard(row);
    }

    private void buildFavorites() {
        addTabs(new String[]{"▧  코스", "♧  기록"}, 0);
        addHeading("⚑  고정된 즐겨찾기", null);
        TextView help = text("상단에 고정된 코스는 항상 먼저 보여요.", 12, MUTED, false);
        help.setPadding(dp(4), 0, 0, dp(8)); content.addView(help);
        addFavorite("북한산 둘레길 3구간", "서울특별시 은평구", "12.4 km  ·  530 m  ·  3시간 45분", true, 5);
        addHeading("즐겨찾기", "총 4개");
        addFavorite("관악산 연주대 코스", "서울특별시 관악구", "6.8 km  ·  739 m  ·  2시간 30분", false, 6);
        addFavorite("제주 올레길 7코스", "제주특별자치도 서귀포시", "13.7 km  ·  250 m  ·  4시간 10분", false, 7);
        addFavorite("설악산 권금성 코스", "강원특별자치도 속초시", "5.3 km  ·  620 m  ·  2시간 05분", false, 8);
        LinearLayout empty = card(dp(112));
        empty.setGravity(Gravity.CENTER_VERTICAL);
        BadgeIconView icon = new BadgeIconView(requireContext(), true, 9);
        empty.addView(icon, new LinearLayout.LayoutParams(dp(84), dp(84)));
        addLabels(empty, "아직 즐겨찾기한 코스가 없어요", "마음에 드는 코스는 북마크를 눌러 저장해 보세요.");
        addCard(empty);
    }

    private void addFavorite(String name, String area, String details, boolean pinned, int seed) {
        LinearLayout row = card(pinned ? dp(150) : dp(102));
        row.setOrientation(LinearLayout.HORIZONTAL);
        MapThumbView thumb = new MapThumbView(requireContext(), seed);
        row.addView(thumb, new LinearLayout.LayoutParams(pinned ? dp(150) : dp(118), ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout labels = new LinearLayout(requireContext()); labels.setOrientation(LinearLayout.VERTICAL); labels.setPadding(dp(12), dp(5), 0, 0);
        if (pinned) labels.addView(labelChip("⚑  고정됨", true));
        labels.addView(text(name, pinned ? 16 : 14, TEXT, true));
        labels.addView(text("⌖ " + area, 11, MUTED, false));
        labels.addView(text(details, 11, MUTED, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        TextView bookmark = text(prefs.getBoolean("fav_" + seed, true) ? "▮" : "▯", 23, pinned ? GREEN : MUTED, false);
        bookmark.setGravity(Gravity.CENTER);
        bookmark.setOnClickListener(v -> {
            boolean saved = !prefs.getBoolean("fav_" + seed, true);
            prefs.edit().putBoolean("fav_" + seed, saved).apply();
            bookmark.setText(saved ? "▮" : "▯");
        });
        row.addView(bookmark, new LinearLayout.LayoutParams(dp(38), ViewGroup.LayoutParams.MATCH_PARENT));
        addCard(row);
    }

    private void buildBadges() {
        LinearLayout summary = card(dp(138)); summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.addView(stat("획득한 배지", "6 / 20"), weighted());
        summary.addView(stat("완료율", "30%"), weighted());
        LinearLayout level = new LinearLayout(requireContext()); level.setOrientation(LinearLayout.VERTICAL); level.setGravity(Gravity.CENTER);
        level.addView(text("TrailGo Rider", 14, TEXT, true)); level.addView(text("Lv.2", 18, GREEN, true)); level.addView(text("다음 레벨까지 240 EXP", 10, MUTED, false));
        summary.addView(level, weighted()); addCard(summary);
        addHeading("내 배지", "전체⌄");
        LinearLayout grid = new LinearLayout(requireContext()); grid.setOrientation(LinearLayout.VERTICAL);
        String[] names = {"첫 라이딩", "100 km 달성", "업힐 챌린저", "주말 라이더", "안전 라이더", "장거리 라이더"};
        String[] subs = {"첫 라이딩을 완료했어요", "누적 거리 100 km 달성", "상승 고도 1,000 m 달성", "주말에 5회 라이딩", "안전 수칙 10회 준수", "한 번에 50 km 달성"};
        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(requireContext());
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                line.addView(badgeCard(names[index], subs[index], index < 3, index), weightedWithMargins(col == 0 ? 0 : 4, col == 2 ? 0 : 4));
            }
            grid.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(250)));
        }
        content.addView(grid);
    }

    private View badgeCard(String name, String sub, boolean earned, int seed) {
        LinearLayout box = card(0); box.setGravity(Gravity.CENTER); box.setOrientation(LinearLayout.VERTICAL);
        box.addView(new BadgeIconView(requireContext(), earned, seed), new LinearLayout.LayoutParams(dp(98), dp(98)));
        TextView title = text(name, 14, TEXT, true); title.setGravity(Gravity.CENTER); box.addView(title);
        TextView desc = text(sub, 11, MUTED, false); desc.setGravity(Gravity.CENTER); desc.setPadding(dp(5), dp(5), dp(5), 0); box.addView(desc);
        TextView progress = text(earned ? "완료" : (seed == 3 ? "2 / 5" : seed == 4 ? "3 / 10" : "12 / 50"), 11, earned ? GREEN : DARK_GREEN, false); progress.setPadding(0, dp(8), 0, 0); box.addView(progress);
        return box;
    }

    private void buildGear() {
        LinearLayout bike = card(dp(210)); bike.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout visual = new LinearLayout(requireContext()); visual.setOrientation(LinearLayout.VERTICAL);
        visual.addView(labelChip("★ 주 장비", false));
        ImageView bikeImage = new ImageView(requireContext());
        bikeImage.setImageResource(R.drawable.profile_gear_bike);
        bikeImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        visual.addView(bikeImage, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        bike.addView(visual, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.1f));
        LinearLayout info = new LinearLayout(requireContext()); info.setOrientation(LinearLayout.VERTICAL); info.setGravity(Gravity.CENTER_VERTICAL);
        info.addView(text("TrailGo 27.5", 19, TEXT, true)); info.addView(text("TrailGo Pro 27.5", 12, MUTED, false));
        TextView specs = text("\n휠 사이즈        누적 거리\n27.5 인치        312.6 km", 13, TEXT, false); info.addView(specs);
        bike.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1)); addCard(bike);
        addHeading("정비 관리", null);
        LinearLayout maintenance = card(0);
        addMaintenance(maintenance, "체인 점검", "체인 청소 및 마모 확인", "양호", "100 km", GREEN);
        addMaintenance(maintenance, "브레이크 패드 점검", "패드 마모 상태 확인", "주의", "60 km", Color.rgb(243, 134, 27));
        addMaintenance(maintenance, "서스펜션 서비스", "포크 오일 및 실링 점검", "예정", "300 km", MUTED);
        addCard(maintenance);
        addHeading("액세서리", "3개");
        LinearLayout accessories = card(0);
        addAccessory(accessories, "TrailGo 헬멧", "M 사이즈 / 매트 블랙", R.drawable.profile_ic_lock);
        addAccessory(accessories, "TrailGo 물통", "750ml / 그린", R.drawable.profile_ic_data);
        addAccessory(accessories, "TrailGo 라이트", "전면 LED / 800루멘", R.drawable.records_ic_sun);
        addCard(accessories);
        Button add = primaryButton("＋  장비 추가"); add.setOnClickListener(v -> Toast.makeText(requireContext(), "장비 추가 화면은 준비 중입니다.", Toast.LENGTH_SHORT).show()); content.addView(add, fullMargins(0, 10, 0, 16));
    }

    private void addMaintenance(LinearLayout parent, String name, String sub, String status, String next, int color) {
        LinearLayout row = baseRow(dp(74));
        ImageView icon = new ImageView(requireContext()); icon.setImageResource(R.drawable.ic_setting_home); icon.setColorFilter(DARK_GREEN); row.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(34)));
        addLabels(row, name, sub);
        TextView value = text(status + "\n" + next, 12, color, true); value.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); row.addView(value, new LinearLayout.LayoutParams(dp(78), ViewGroup.LayoutParams.MATCH_PARENT));
        parent.addView(row); parent.addView(divider());
    }

    private void addAccessory(LinearLayout parent, String name, String sub, int iconRes) {
        LinearLayout row = baseRow(dp(66)); ImageView icon = new ImageView(requireContext()); icon.setImageResource(iconRes); icon.setColorFilter(DARK_GREEN); icon.setPadding(dp(8), dp(8), dp(8), dp(8)); row.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(52))); addLabels(row, name, sub); row.addView(text("›", 24, MUTED, false)); parent.addView(row); parent.addView(divider());
    }

    private LinearLayout card(int minHeight) { LinearLayout v = new LinearLayout(requireContext()); v.setMinimumHeight(minHeight); v.setOrientation(LinearLayout.VERTICAL); v.setBackgroundResource(R.drawable.profile_feature_card); v.setElevation(dp(2)); return v; }
    private LinearLayout baseRow(int height) { LinearLayout v = new LinearLayout(requireContext()); v.setMinimumHeight(height); v.setGravity(Gravity.CENTER_VERTICAL); v.setOrientation(LinearLayout.HORIZONTAL); return v; }
    private void addCard(View card) { content.addView(card, fullMargins(0, 0, 0, 12)); }
    private LinearLayout.LayoutParams fullMargins(int l, int t, int r, int b) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p; }
    private LinearLayout.LayoutParams weighted() { return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1); }
    private LinearLayout.LayoutParams weightedWithMargins(int l, int r) { LinearLayout.LayoutParams p = weighted(); p.setMargins(dp(l), 0, dp(r), 0); return p; }
    private TextView text(String value, int size, int color, boolean bold) { TextView v = new TextView(requireContext()); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setLineSpacing(0, 1.12f); if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD); return v; }
    private TextView stat(String label, String value) { TextView v = text(label + "\n" + value, 13, TEXT, false); v.setGravity(Gravity.CENTER); v.setLineSpacing(dp(7), 1f); return v; }
    private LinearLayout insight(String label, String value, String sub) { LinearLayout v = card(dp(112)); v.setGravity(Gravity.CENTER); v.addView(text(label, 12, TEXT, false)); v.addView(text(value, 17, TEXT, true)); v.addView(text(sub, 10, MUTED, false)); return v; }
    private void addLabels(LinearLayout row, String title, String sub) { LinearLayout l = new LinearLayout(requireContext()); l.setOrientation(LinearLayout.VERTICAL); l.setGravity(Gravity.CENTER_VERTICAL); l.addView(text(title, 14, TEXT, true)); if (sub != null) l.addView(text(sub, 11, MUTED, false)); row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); }
    private void addHeading(String heading, String value) { LinearLayout r = new LinearLayout(requireContext()); r.setGravity(Gravity.CENTER_VERTICAL); TextView h = text(heading, 18, TEXT, true); h.setPadding(dp(4), dp(14), 0, dp(10)); r.addView(h, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); if (value != null) r.addView(text(value, 12, MUTED, false)); content.addView(r); }
    private void addCardHeader(LinearLayout box, String heading, String sub, String right) { LinearLayout r = new LinearLayout(requireContext()); TextView left = text(heading + "\n" + sub, 16, TEXT, false); r.addView(left, weighted()); TextView rv = text(right, 11, MUTED, false); rv.setGravity(Gravity.END); r.addView(rv); box.addView(r, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54))); }
    private TextView labelChip(String label, boolean filled) { TextView c = text(label, 11, filled ? Color.WHITE : DARK_GREEN, true); c.setGravity(Gravity.CENTER); c.setBackgroundResource(filled ? R.drawable.profile_feature_chip_selected : R.drawable.profile_feature_chip); c.setPadding(dp(9), dp(3), dp(9), dp(3)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(28)); p.setMargins(0, 0, 0, dp(5)); c.setLayoutParams(p); return c; }
    private void addTabs(String[] tabs, int selected) { LinearLayout row = new LinearLayout(requireContext()); row.setPadding(0, dp(10), 0, dp(18)); for (int i = 0; i < tabs.length; i++) { TextView tab = text(tabs[i], 14, i == selected ? Color.WHITE : TEXT, false); tab.setGravity(Gravity.CENTER); tab.setBackgroundResource(i == selected ? R.drawable.profile_feature_chip_selected : R.drawable.profile_feature_chip); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)); p.setMargins(0, 0, dp(12), 0); row.addView(tab, p); final int index = i; tab.setOnClickListener(v -> Toast.makeText(requireContext(), tabs[index] + " 보기", Toast.LENGTH_SHORT).show()); } content.addView(row); }
    private Button primaryButton(String label) { Button b = new Button(requireContext()); b.setText(label); b.setTextSize(15); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setBackgroundResource(R.drawable.settings_segment_selected); b.setMinHeight(dp(52)); return b; }
    private View divider() { View d = new View(requireContext()); d.setBackgroundColor(Color.rgb(229, 233, 230)); d.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)); return d; }
    private String duration(int seconds) { int h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60; return String.format(Locale.KOREA, "%02d:%02d:%02d", h, m, s); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void bindBottomNavigation(View root) {
        TextView profile = root.findViewById(R.id.bottom_nav_profile); profile.setTextColor(GREEN); profile.setSelected(true); profile.setTypeface(null, android.graphics.Typeface.BOLD);
        root.findViewById(R.id.bottom_nav_home).setOnClickListener(v -> controller.navigate(R.id.nav_home));
        root.findViewById(R.id.bottom_nav_records).setOnClickListener(v -> controller.navigate(R.id.nav_records));
        root.findViewById(R.id.bottom_nav_course).setOnClickListener(v -> controller.navigate(R.id.nav_course_view));
        root.findViewById(R.id.bottom_nav_ride).setOnClickListener(v -> startActivity(new Intent(requireContext(), StartActivity.class)));
        profile.setOnClickListener(v -> controller.navigate(R.id.setting));
    }

    @Override public void onResume() { super.onResume(); View appBar = requireActivity().findViewById(R.id.app_bar_layout); if (appBar != null) appBar.setVisibility(View.GONE); }
    @Override public void onDestroyView() { if (recordsCall != null) recordsCall.cancel(); super.onDestroyView(); }

    private static class ChartView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); private final boolean line;
        private final float[] values = {18.4f, 22.7f, 0f, 35.1f, 12.6f, 41.8f, 20.3f};
        ChartView(android.content.Context c, boolean line) { super(c); this.line = line; }
        @Override protected void onDraw(Canvas c) { super.onDraw(c); float w=getWidth(), h=getHeight(), left=32, right=w-10, top=15, bottom=h-26; paint.setStrokeWidth(1); paint.setColor(Color.rgb(225,230,226)); for(int i=0;i<5;i++){float y=top+(bottom-top)*i/4;c.drawLine(left,y,right,y,paint);} paint.setTextSize(10*getResources().getDisplayMetrics().scaledDensity); paint.setColor(Color.rgb(95,105,99)); if(line){Path p=new Path(); for(int i=0;i<31;i++){float x=left+(right-left)*i/30;float v=18+(float)(22*Math.sin(i*.55)+18*Math.abs(Math.sin(i*.17)));float y=bottom-(bottom-top)*Math.min(80,v)/80;if(i==0)p.moveTo(x,y);else p.lineTo(x,y);} paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setColor(GREEN);c.drawPath(p,paint);paint.setStyle(Paint.Style.FILL); String[] xs={"5/1","5/5","5/10","5/15","5/20","5/25","5/30"};for(int i=0;i<7;i++)c.drawText(xs[i],left+(right-left)*i/6-8,bottom+20,paint);}else{String[] days={"월","화","수","목","금","토","일"};float unit=(right-left)/7;for(int i=0;i<7;i++){float bh=(bottom-top)*values[i]/50;paint.setColor(GREEN);c.drawRoundRect(left+i*unit+unit*.33f,bottom-bh,left+i*unit+unit*.67f,bottom,8,8,paint);paint.setColor(TEXT);c.drawText(String.valueOf(values[i]),left+i*unit+unit*.28f,bottom-bh-7,paint);paint.setColor(MUTED);c.drawText(days[i],left+i*unit+unit*.43f,bottom+20,paint);}} }
    }

    private static class MapThumbView extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final int seed;
        MapThumbView(android.content.Context c,int seed){super(c);this.seed=seed;setBackgroundColor(Color.rgb(234,243,232));}
        @Override protected void onDraw(Canvas c){super.onDraw(c);p.setStrokeWidth(2);p.setColor(Color.rgb(210,222,211));for(int i=-2;i<7;i++){c.drawLine(i*38+seed*7,0,i*38-30+seed*7,getHeight(),p);c.drawLine(0,i*31+seed*5,getWidth(),i*31-22+seed*5,p);}Path route=new Path();route.moveTo(18,getHeight()*.75f);route.cubicTo(getWidth()*.35f,getHeight()*.2f,getWidth()*.55f,getHeight()*.82f,getWidth()-18,getHeight()*.25f);p.setColor(GREEN);p.setStrokeWidth(5);p.setStyle(Paint.Style.STROKE);c.drawPath(route,p);p.setStyle(Paint.Style.FILL);c.drawCircle(18,getHeight()*.75f,8,p);c.drawCircle(getWidth()-18,getHeight()*.25f,7,p);p.setColor(Color.WHITE);c.drawCircle(getWidth()-18,getHeight()*.25f,3,p);}
    }

    private static class BadgeIconView extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final boolean earned; private final int seed;
        BadgeIconView(android.content.Context c,boolean earned,int seed){super(c);this.earned=earned;this.seed=seed;}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-5;Path h=new Path();for(int i=0;i<6;i++){double a=Math.PI/3*i-Math.PI/2;float x=cx+(float)Math.cos(a)*r,y=cy+(float)Math.sin(a)*r;if(i==0)h.moveTo(x,y);else h.lineTo(x,y);}h.close();p.setColor(earned?(seed%2==0?GREEN:Color.rgb(31,170,77)):Color.rgb(174,178,175));c.drawPath(h,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(Color.WHITE);c.drawPath(h,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(r*.8f);p.setFakeBoldText(true);p.setColor(Color.WHITE);c.drawText(seed==1?"100":seed==4?"✓":"★",cx,cy+r*.25f,p);}
    }

}
