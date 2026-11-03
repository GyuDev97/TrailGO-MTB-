package com.bike.trailgo.setting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bike.trailgo.R;
import com.bike.trailgo.dto.LoginInfo;
import com.bike.trailgo.dto.RidingRecord;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.ui.BaseFragment;
import com.bike.trailgo.ui.member.LoginActivity;
import com.bike.trailgo.ui.riding.StartActivity;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Setting extends BaseFragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private Call<ArrayList<RidingRecord>> profileRecordsCall;
    private Call<LoginInfo> profileInfoCall;

    public Setting() {
    }

    public static Setting newInstance(String param1, String param2) {
        Setting fragment = new Setting();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindProfileMenu(view);
    }

    private void bindProfileMenu(@NonNull View view) {
        SharedPreferences auto = requireActivity().getSharedPreferences("auto", Activity.MODE_PRIVATE);
        String loginId = firstNotEmpty(auto.getString("LoginId", ""), "admin");
        String nickname = firstNotEmpty(auto.getString("r_nickname", ""), loginId);
        String imageName = auto.getString("r_image", "");
        String phone = auto.getString("sosphone", "");
        String email = loginId.contains("@") ? loginId : "이메일 미등록";

        TextView profileName = view.findViewById(R.id.profile_name);
        TextView profileClub = view.findViewById(R.id.profile_club);
        TextView rowNameValue = view.findViewById(R.id.row_name_value);
        TextView rowEmailValue = view.findViewById(R.id.row_email_value);
        TextView rowPhoneValue = view.findViewById(R.id.row_phone_value);
        TextView rideCount = view.findViewById(R.id.profile_ride_count);
        TextView rideDistance = view.findViewById(R.id.profile_ride_distance);
        TextView rideHeight = view.findViewById(R.id.profile_ride_height);
        ImageView profileImage = view.findViewById(R.id.profile_image);

        profileName.setText(nickname);
        profileClub.setText(firstNotEmpty(auto.getString("r_clubname", ""), "소속 클럽 없음"));
        rowNameValue.setText(nickname);
        rowEmailValue.setText(email);
        rowPhoneValue.setText(firstNotEmpty(phone, "미등록"));
        rideCount.setText(firstNotEmpty(auto.getString("ride_count", ""), "12 회"));
        rideDistance.setText(firstNotEmpty(auto.getString("ride_distance", ""), "128 km"));
        rideHeight.setText(firstNotEmpty(auto.getString("ride_height", ""), "35,680 m"));
        loadProfileStats(loginId, rideCount, rideDistance, rideHeight);
        loadProfileInfo(loginId, auto, profileName, profileClub, rowNameValue, profileImage);

        if (!imageName.isEmpty()) {
            loadProfileImage(imageName, profileImage);
        }

        view.findViewById(R.id.row_phone).setOnClickListener(v -> showPhoneDialog(auto, rowPhoneValue));
        view.findViewById(R.id.row_logout).setOnClickListener(v -> logout(auto));
        view.findViewById(R.id.profile_menu_logout).setOnClickListener(v -> logout(auto));
        view.findViewById(R.id.bottom_nav_home).setOnClickListener(v -> navigateTo(R.id.nav_home));
        view.findViewById(R.id.bottom_nav_records).setOnClickListener(v -> navigateTo(R.id.nav_records));
        view.findViewById(R.id.bottom_nav_course).setOnClickListener(v -> navigateTo(R.id.nav_course_view));
        view.findViewById(R.id.bottom_nav_ride).setOnClickListener(
                v -> startActivity(new Intent(requireContext(), StartActivity.class))
        );
        view.findViewById(R.id.profile_menu_stats).setOnClickListener(v -> navigateTo(R.id.profile_stats));
        view.findViewById(R.id.profile_menu_courses).setOnClickListener(v -> navigateTo(R.id.profile_courses));
        view.findViewById(R.id.profile_menu_favorite).setOnClickListener(v -> navigateTo(R.id.profile_favorites));
        view.findViewById(R.id.profile_menu_badge).setOnClickListener(v -> navigateTo(R.id.profile_badges));
        view.findViewById(R.id.profile_menu_gear).setOnClickListener(v -> navigateTo(R.id.profile_gear));
        view.findViewById(R.id.profile_menu_settings).setOnClickListener(v -> navigateTo(R.id.app_settings));
        TextView profile = view.findViewById(R.id.bottom_nav_profile);
        profile.setTextColor(android.graphics.Color.parseColor("#1F7A2E"));
        profile.setSelected(true);
        profile.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void loadProfileInfo(
            String loginId,
            SharedPreferences auto,
            TextView profileName,
            TextView profileClub,
            TextView rowNameValue,
            ImageView profileImage
    ) {
        profileInfoCall = serverApi.getLoginInfo(loginId);
        profileInfoCall.enqueue(new Callback<LoginInfo>() {
            @Override
            public void onResponse(Call<LoginInfo> call, Response<LoginInfo> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null) {
                    return;
                }

                LoginInfo info = response.body();
                String nickname = firstNotEmpty(info.getR_nickname(), loginId);
                String clubName = firstNotEmpty(info.getCb_name(), "소속 클럽 없음");
                String imageName = firstNotEmpty(info.getR_image(), "");

                profileName.setText(nickname);
                profileClub.setText(clubName);
                rowNameValue.setText(nickname);
                SharedPreferences.Editor editor = auto.edit()
                        .putString("r_nickname", nickname)
                        .putString("r_clubname", info.getCb_name());
                if (!imageName.isEmpty()) {
                    editor.putString("r_image", imageName);
                    loadProfileImage(imageName, profileImage);
                }
                editor.apply();
            }

            @Override
            public void onFailure(Call<LoginInfo> call, Throwable error) {
                Log.d("ProfileApi", "Unable to load profile information", error);
            }
        });
    }

    private void loadProfileImage(String imageName, ImageView profileImage) {
        Picasso.get()
                .load(Server.getRiderImageUrl(imageName))
                .placeholder(R.drawable.m_logo)
                .error(R.drawable.m_logo)
                .into(profileImage);
    }

    private void loadProfileStats(
            String loginId,
            TextView rideCount,
            TextView rideDistance,
            TextView rideHeight
    ) {
        profileRecordsCall = serverApi.getRecord(loginId);
        profileRecordsCall.enqueue(new Callback<ArrayList<RidingRecord>>() {
            @Override
            public void onResponse(
                    Call<ArrayList<RidingRecord>> call,
                    Response<ArrayList<RidingRecord>> response
            ) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null) {
                    return;
                }
                int totalDistance = 0;
                int totalHeight = 0;
                for (RidingRecord record : response.body()) {
                    totalDistance += Math.max(0, record.getRr_distance());
                    totalHeight += Math.max(0, record.getRr_high());
                }
                rideCount.setText(response.body().size() + " 회");
                rideDistance.setText(formatProfileDistance(totalDistance));
                rideHeight.setText(String.format(Locale.KOREA, "%,d m", totalHeight));
            }

            @Override
            public void onFailure(Call<ArrayList<RidingRecord>> call, Throwable error) {
                Log.d("ProfileApi", "Unable to load profile riding summary", error);
            }
        });
    }

    private String formatProfileDistance(int meters) {
        if (meters < 1000) {
            return String.format(Locale.KOREA, "%.2f km", meters / 1000.0);
        }
        return String.format(Locale.KOREA, "%.1f km", meters / 1000.0);
    }

    private void navigateTo(int destinationId) {
        if (controller.getCurrentDestination() == null
                || controller.getCurrentDestination().getId() != destinationId) {
            controller.navigate(destinationId);
        }
    }

    private void showPhoneDialog(SharedPreferences auto, TextView phoneValue) {
        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setText(auto.getString("sosphone", ""));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(requireContext())
                .setTitle("전화번호 설정")
                .setView(input)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", (dialog, which) -> {
                    String phone = input.getText().toString().trim();
                    auto.edit().putString("sosphone", phone).apply();
                    phoneValue.setText(firstNotEmpty(phone, "미등록"));
                })
                .show();
    }

    private void logout(SharedPreferences auto) {
        if (UserSessionManager.hasUnfinishedRide(requireContext())) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("주행 기록 중")
                    .setMessage("진행 중인 주행을 먼저 종료하고 저장한 뒤 로그아웃하세요.")
                    .setPositiveButton("확인", null)
                    .show();
            return;
        }
        UserSessionManager.clearLocalUserData(requireContext());
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private String firstNotEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    @Override
    public void onResume() {
        super.onResume();
        View appBar = requireActivity().findViewById(R.id.app_bar_layout);
        if (appBar != null) {
            appBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        if (profileRecordsCall != null) {
            profileRecordsCall.cancel();
            profileRecordsCall = null;
        }
        if (profileInfoCall != null) {
            profileInfoCall.cancel();
            profileInfoCall = null;
        }
        super.onDestroyView();
    }
}
