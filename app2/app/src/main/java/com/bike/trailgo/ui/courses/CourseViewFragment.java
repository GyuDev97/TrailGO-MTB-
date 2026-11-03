package com.bike.trailgo.ui.courses;

import android.graphics.Color;
import android.graphics.Typeface;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bike.trailgo.R;
import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.gpx.GPXParser;
import com.bike.trailgo.gpx.Gpx;
import com.bike.trailgo.ui.BaseFragment;
import com.bike.trailgo.ui.recycler.CourseAdapter;
import com.bike.trailgo.ui.riding.CourseGpxAnalyzer;
import com.bike.trailgo.ui.riding.CourseRouteStore;
import com.bike.trailgo.ui.riding.FollowStart;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseViewFragment extends BaseFragment {
    public static final String ARG_OPEN_GPX_IMPORT = "open_gpx_import";
    private static final int REQUEST_IMPORT_GPX = 7401;
    private Call<ArrayList<RidingRecord>> request;
    private List<RidingRecord> items;
    private Call<List<RidingRecord>> getCourseList;

    private RecyclerView recyclerView;
    String LoginId, Nickname, r_image;
    ImageView userImage;

    CourseAdapter courseAdapter, likeAdapter, highAdapter, disAdapter;
    Button moreButton, importGpxButton;
    TextView textView1, textView2, textView3, textView4;
    ArrayList<RidingRecord> itemList = new ArrayList<>();
    ArrayList<RidingRecord> likeList = new ArrayList<>();
    ArrayList<RidingRecord> highList = new ArrayList<>();
    ArrayList<RidingRecord> disList = new ArrayList<>();


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // 최소한의 코드
        return inflater.inflate(R.layout.fragment_course_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        //request = serverApi.getRecord(model.r_Id.getValue());
        r_image = model.r_Image.getValue();
        moreButton = view.findViewById(R.id.more_button);
        importGpxButton = view.findViewById(R.id.gpx_import_button);
        textView1 = view.findViewById(R.id.textView17);
        textView2 = view.findViewById(R.id.textView18);
        textView3 = view.findViewById(R.id.textView19);
        textView4 = view.findViewById(R.id.textView20);

        getCourseList = serverApi.getCourseList();
        getCourseList.enqueue(new Callback<List<RidingRecord>>() {
            @Override
            public void onResponse(Call<List<RidingRecord>> call, Response<List<RidingRecord>> response) {

                List<RidingRecord> body = response.body();
                if (!isAdded() || !response.isSuccessful() || body == null) {
                    return;
                }

                    itemList.clear();
                    itemList.addAll(body);
                    if (itemList.isEmpty()) {
                        itemList.addAll(createPreviewCourses());
                    }
                    likeList.clear();
                    likeList.addAll(body);
                    highList.clear();
                    highList.addAll(body);
                    disList.clear();
                    disList.addAll(body);

                    Collections.sort(likeList, (left, right) -> Integer.compare(right.getRr_like(), left.getRr_like()));
                    Collections.sort(highList, (left, right) -> Integer.compare(right.getRr_high(), left.getRr_high()));
                    Collections.sort(disList, (left, right) -> Integer.compare(right.getRr_distance(), left.getRr_distance()));

                    courseAdapter = new CourseAdapter(requireContext().getApplicationContext(), itemList, r_image, controller, model);
                    likeAdapter = new CourseAdapter(requireContext().getApplicationContext(), likeList, r_image, controller, model);
                    highAdapter = new CourseAdapter(requireContext().getApplicationContext(), highList, r_image, controller, model);
                    disAdapter = new CourseAdapter(requireContext().getApplicationContext(), disList, r_image, controller, model);

                    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
                    recyclerView.setAdapter(courseAdapter);
                    moreButton.setVisibility(itemList.size() > courseAdapter.count ? View.VISIBLE : View.GONE);
                }

            @Override
            public void onFailure(Call<List<RidingRecord>> call, Throwable t) {
                bindCourseList(createPreviewCourses());
            }
        });


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int lastPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition() + 1;
                int totalCount = recyclerView.getAdapter().getItemCount();

                Log.e("끝포지션", String.valueOf(totalCount));
                Log.e("라스트포지션", String.valueOf(lastPosition));


                if(lastPosition == totalCount){
                    if ( courseAdapter.itemList.size() == courseAdapter.count){
                        moreButton.setVisibility(View.GONE);
                    }
                }

            }
        });

        moreButton.setOnClickListener(v -> {
            CourseAdapter activeAdapter = (CourseAdapter) recyclerView.getAdapter();
            if (activeAdapter == null) {
                return;
            }
            activeAdapter.count = Math.min(activeAdapter.itemList.size(), activeAdapter.count + 5);
            activeAdapter.notifyDataSetChanged();
            moreButton.setVisibility(activeAdapter.count >= activeAdapter.itemList.size() ? View.GONE : View.VISIBLE);
        });

        importGpxButton.setOnClickListener(v -> selectGpxDocument());
        // Settings > Linked services > GPX import opens this tab with the file picker already showing.
        Bundle arguments = getArguments();
        if (arguments != null && arguments.getBoolean(ARG_OPEN_GPX_IMPORT, false)) {
            arguments.remove(ARG_OPEN_GPX_IMPORT);
            selectGpxDocument();
        }

        textView1.setOnClickListener( v -> {
            recyclerView.setAdapter(courseAdapter);
            selectTab(textView1);
        });

        textView2.setOnClickListener( v -> {
            recyclerView.setAdapter(likeAdapter);
            selectTab(textView2);
        });

        textView3.setOnClickListener( v -> {
            recyclerView.setAdapter(highAdapter);
            selectTab(textView3);
        });

        textView4.setOnClickListener( v -> {
            recyclerView.setAdapter(disAdapter);
            selectTab(textView4);
        });
    }

    private void bindCourseList(ArrayList<RidingRecord> courses) {
        itemList.clear();
        itemList.addAll(courses);
        likeList.clear();
        likeList.addAll(courses);
        highList.clear();
        highList.addAll(courses);
        disList.clear();
        disList.addAll(courses);
        courseAdapter = new CourseAdapter(requireContext().getApplicationContext(), itemList, r_image, controller, model);
        likeAdapter = new CourseAdapter(requireContext().getApplicationContext(), likeList, r_image, controller, model);
        highAdapter = new CourseAdapter(requireContext().getApplicationContext(), highList, r_image, controller, model);
        disAdapter = new CourseAdapter(requireContext().getApplicationContext(), disList, r_image, controller, model);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext().getApplicationContext()));
        recyclerView.setAdapter(courseAdapter);
        moreButton.setVisibility(View.GONE);
    }

    private ArrayList<RidingRecord> createPreviewCourses() {
        ArrayList<RidingRecord> previews = new ArrayList<>();
        previews.add(previewCourse(1, "팔공산 순환 코스", 32450, 1250, 8100, 18));
        previews.add(previewCourse(2, "비슬산 임월 코스", 22170, 1250, 7200, 17));
        previews.add(previewCourse(3, "금호강 자전거길", 18320, 120, 4350, 22));
        previews.add(previewCourse(4, "대명저수지 둘레길", 9840, 210, 3900, 15));
        return previews;
    }

    private RidingRecord previewCourse(
            int id, String name, int distance, int height, int seconds, int averageSpeed
    ) {
        RidingRecord record = new RidingRecord();
        record.setRr_num(id);
        record.setRr_name(name);
        record.setRr_distance(distance);
        record.setRr_high(height);
        record.setRr_time(seconds);
        record.setRr_avgspeed(averageSpeed);
        record.setRr_rider("TrailGO");
        record.setRr_date("2026-05-16 09:41:00");
        record.setRr_area("대구광역시");
        return record;
    }

    private void selectTab(TextView selected) {
        TextView[] tabs = {textView1, textView2, textView3, textView4};
        for (TextView tab : tabs) {
            boolean isSelected = tab == selected;
            tab.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
            tab.setTextColor(Color.parseColor(isSelected ? "#FFFFFF" : "#667069"));
            tab.setBackgroundResource(isSelected ? R.drawable.course_badge_bg : android.R.color.transparent);
        }
        CourseAdapter activeAdapter = (CourseAdapter) recyclerView.getAdapter();
        moreButton.setVisibility(activeAdapter != null && activeAdapter.count < activeAdapter.itemList.size()
                ? View.VISIBLE : View.GONE);
    }

    private void selectGpxDocument() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/gpx+xml", "application/xml", "text/xml", "application/octet-stream"
        });
        startActivityForResult(picker, REQUEST_IMPORT_GPX);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT_GPX || resultCode != android.app.Activity.RESULT_OK
                || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (SecurityException ignored) {
            // The copied private file below is the durable source, so providers need not support this grant.
        }
        importGpxOnWorker(uri);
    }

    private void importGpxOnWorker(Uri uri) {
        if (!isAdded()) return;
        final Context appContext = requireContext().getApplicationContext();
        final String displayName = getDisplayName(uri);
        Toast.makeText(appContext, "GPX 파일을 불러오는 중입니다…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String reference = null;
            CourseGpxAnalyzer.Summary summary = null;
            String failure = null;
            try {
                ContentResolver resolver = appContext.getContentResolver();
                try (InputStream source = resolver.openInputStream(uri)) {
                    if (source == null) throw new IOException("Unable to open the selected document");
                    reference = CourseRouteStore.importFrom(appContext, displayName, source);
                }
                try (InputStream copiedFile = CourseRouteStore.open(appContext, reference)) {
                    Gpx gpx = new GPXParser().parse(copiedFile);
                    summary = CourseGpxAnalyzer.analyze(gpx, removeGpxExtension(displayName));
                }
                if (summary.getPointCount() < 2 || summary.getTotalDistanceMeters() <= 1d) {
                    throw new IOException("No usable route points in GPX");
                }
            } catch (IOException | XmlPullParserException error) {
                Log.w("CourseImport", "Unable to import GPX", error);
                failure = error.getMessage();
            }

            final String importedReference = reference;
            final CourseGpxAnalyzer.Summary importedSummary = summary;
            final String importFailure = failure;
            if (!isAdded()) {
                if (importedReference != null) CourseRouteStore.deleteImported(appContext, importedReference);
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    if (importedReference != null) CourseRouteStore.deleteImported(appContext, importedReference);
                    return;
                }
                if (importFailure != null) {
                    if (importedReference != null) CourseRouteStore.deleteImported(appContext, importedReference);
                    String message = importFailure.contains("30 MB")
                            ? "30MB 이하의 GPX 파일을 선택해 주세요."
                            : "GPX 파일을 읽지 못했습니다. 경로 포인트가 있는 GPX 파일인지 확인해 주세요.";
                    new AlertDialog.Builder(requireContext())
                            .setTitle("GPX 불러오기 실패")
                            .setMessage(message)
                            .setPositiveButton("확인", null)
                            .show();
                    return;
                }
                showImportPreview(importedReference, importedSummary);
            });
        }, "gpx-import").start();
    }

    private void showImportPreview(String reference, CourseGpxAnalyzer.Summary summary) {
        StringBuilder preview = new StringBuilder();
        preview.append("코스명: ").append(summary.getCourseName());
        preview.append("\n총 거리: ").append(formatDistance(summary.getTotalDistanceMeters()));
        if (summary.getAscentMeters() != null) {
            preview.append("\n상승 고도: ").append(Math.round(summary.getAscentMeters())).append(" m");
        }
        preview.append("\n경로 포인트: ").append(summary.getPointCount()).append("개");
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("GPX 코스 미리보기")
                .setMessage(preview.toString())
                .setNegativeButton("취소", (ignored, which) -> CourseRouteStore.deleteImported(requireContext(), reference))
                .setPositiveButton("이 코스 따라가기", (ignored, which) -> {
                    Intent follow = new Intent(requireContext(), FollowStart.class);
                    follow.putExtra("gpx", reference);
                    follow.putExtra("c_name", summary.getCourseName());
                    startActivity(follow);
                })
                .create();
        dialog.setOnCancelListener(ignored -> CourseRouteStore.deleteImported(requireContext(), reference));
        dialog.show();
    }

    private String getDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = requireContext().getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                if (name != null && !name.trim().isEmpty()) return name.trim();
            }
        } catch (SecurityException ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return "imported-course.gpx";
    }

    private static String removeGpxExtension(String name) {
        return name != null && name.toLowerCase(java.util.Locale.US).endsWith(".gpx")
                ? name.substring(0, name.length() - 4) : name;
    }

    private static String formatDistance(double meters) {
        return meters >= 1000d
                ? String.format(java.util.Locale.KOREA, "%.1f km", meters / 1000d)
                : Math.round(meters) + " m";
    }

    @Override
    public void onResume() {
        super.onResume();
        setAppBarVisible(false);
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
