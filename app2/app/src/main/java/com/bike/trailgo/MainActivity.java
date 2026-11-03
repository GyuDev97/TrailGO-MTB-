package com.bike.trailgo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bike.trailgo.dto.LoginInfo;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.setting.AccountSession;
import com.bike.trailgo.setting.RideSettings;
import com.kakao.sdk.user.UserApiClient;
import com.bike.trailgo.setting.UserSessionManager;
import com.bike.trailgo.ui.member.LoginActivity;
import com.bike.trailgo.ui.riding.StartActivity;
import com.squareup.picasso.Picasso;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private TextView lay;
    private TextView userMeta;
    private ImageView userImage;
    private SharedPreferences auto;
    private View mainContent;
    private CoordinatorLayout.Behavior<?> appBarScrollingBehavior;
    private Call<LoginInfo> profileCall;
    private DrawerLayout drawer;
    private NavController navController;
    private OnBackInvokedCallback systemBackCallback;
    private int[] drawerItemIds = {
            R.id.nav_home,
            R.id.nav_records,
            R.id.nav_ride,
            R.id.nav_course_search,
            R.id.nav_course_scrap,
            R.id.nav_badge,
            R.id.nav_sett,
            R.id.nav_help
    };

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        mainContent = findViewById(R.id.main_content);
        CoordinatorLayout.LayoutParams contentParams =
                (CoordinatorLayout.LayoutParams) mainContent.getLayoutParams();
        appBarScrollingBehavior = contentParams.getBehavior();
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_records, R.id.nav_course_view, R.id.nav_course_search, R.id.nav_course_scrap,
                R.id.nav_competition_view, R.id.nav_mission_view, R.id.nav_danger_view,
                R.id.setting, R.id.app_settings)
                .setDrawerLayout(drawer)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        toolbar.setTitle("TrailGO");
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            toolbar.setTitle("TrailGO");
            updateDrawerSelection(destination.getId());
            int destinationId = destination.getId();
            boolean useCustomHeader = destinationId == R.id.nav_home
                    || destinationId == R.id.nav_records
                    || destinationId == R.id.nav_course_view
                    || destinationId == R.id.setting
                    || destinationId == R.id.app_settings
                    || destinationId == R.id.profile_stats
                    || destinationId == R.id.profile_courses
                    || destinationId == R.id.profile_favorites
                    || destinationId == R.id.profile_badges
                    || destinationId == R.id.profile_gear
                    || destinationId == R.id.myDetailFragment;
            View appBar = findViewById(R.id.app_bar_layout);
            if (appBar != null) {
                appBar.setVisibility(useCustomHeader ? View.GONE : View.VISIBLE);
            }
            updateContentAppBarBehavior(useCustomHeader);
        });

        MainViewModel model = new ViewModelProvider(this).get(MainViewModel.class);
        auto = getSharedPreferences("auto", Activity.MODE_PRIVATE);
        model.r_Id.setValue(auto.getString("LoginId", ""));
        model.r_Image.setValue(auto.getString("r_image", ""));
        model.r_Nickname.setValue(auto.getString("r_nickname", ""));
        model.r_ClubName.setValue(auto.getString("r_clubname", ""));

        lay = findViewById(R.id.infouserid);
        userMeta = findViewById(R.id.infousermeta);
        userImage = findViewById(R.id.user_image);
        bindDrawerHeader(model);
        refreshProfile(model);
        setupDrawerClicks(drawer, navController);
        configureBackNavigation();
        openChosenStartScreen(savedInstanceState);

        logKeyHash();
    }

    /** Settings > General > start screen: applied once per start, never after a rotation or restore. */
    private void openChosenStartScreen(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            return;
        }
        String chosen = RideSettings.startScreen(this);
        if (RideSettings.START_RECORDS.equals(chosen)) {
            navController.navigate(R.id.nav_records);
        } else if (RideSettings.START_COURSES.equals(chosen)) {
            navController.navigate(R.id.nav_course_view);
        }
    }

    private void bindDrawerHeader(MainViewModel model) {
        boolean guest = AccountSession.isGuest(this);
        String riderId = valueOrDefault(model.r_Id.getValue(), guest ? "게스트" : "회원");
        String nickname = valueOrDefault(model.r_Nickname.getValue(), riderId);
        lay.setText(nickname);
        userMeta.setText(guest ? "로그인하면 기록을 동기화할 수 있어요  ›" : "내 정보 보기  ›");

        String imageName = model.r_Image.getValue();
        if (imageName != null && imageName.length() > 0) {
            Picasso.get()
                    .load(Server.getRiderImageUrl(imageName))
                    .placeholder(R.drawable.m_logo)
                    .error(R.drawable.m_logo)
                    .into(userImage);
        } else {
            userImage.setImageResource(R.drawable.m_logo);
        }
    }

    private void setupDrawerClicks(DrawerLayout drawer, NavController navController) {
        setDrawerClick(R.id.drawer_profile_header, drawer, () -> {
            if (AccountSession.isGuest(this)) {
                // Guest MVP intentionally has no account/server-sync entry point.
                // Keep the drawer header useful by opening the local app settings instead.
                navigateTo(navController, R.id.app_settings);
            } else {
                navigateTo(navController, R.id.setting);
            }
        });
        setDrawerClick(R.id.nav_home, drawer, () -> navigateTo(navController, R.id.nav_home));
        setDrawerClick(R.id.nav_records, drawer, () -> navigateTo(navController, R.id.nav_records));
        setDrawerClick(R.id.nav_ride, drawer, () -> startActivity(new Intent(this, StartActivity.class)));
        setDrawerClick(R.id.nav_course_search, drawer, () -> navigateTo(navController, R.id.nav_course_view));
        setDrawerClick(R.id.nav_course_scrap, drawer, () -> {
            if (AccountSession.requireMember(this, "스크랩 보관함")) {
                navigateTo(navController, R.id.nav_course_scrap);
            }
        });
        setDrawerClick(R.id.nav_badge, drawer, () -> {
            if (AccountSession.requireMember(this, "미션과 랭킹")) {
                navigateTo(navController, R.id.nav_mission_view);
            }
        });
        setDrawerClick(R.id.nav_sett, drawer, () -> navigateTo(navController, R.id.app_settings));
        setDrawerClick(R.id.nav_help, drawer, () -> Toast.makeText(this, "고객센터는 준비 중입니다.", Toast.LENGTH_SHORT).show());
    }

    private void setDrawerClick(int viewId, DrawerLayout drawer, Runnable action) {
        View item = findViewById(viewId);
        if (item == null) {
            return;
        }
        item.setOnClickListener(v -> {
            action.run();
            drawer.closeDrawers();
        });
    }

    private void updateDrawerSelection(int destinationId) {
        for (int itemId : drawerItemIds) {
            View item = findViewById(itemId);
            if (item != null) {
                boolean selected = itemId == destinationId
                        || (itemId == R.id.nav_sett && destinationId == R.id.app_settings)
                        || (itemId == R.id.nav_course_search && destinationId == R.id.nav_course_view)
                        || (itemId == R.id.nav_badge && destinationId == R.id.nav_mission_view);
                item.setSelected(selected);
            }
        }
    }

    private void refreshProfile(MainViewModel model) {
        if (AccountSession.isGuest(this)) {
            return;
        }
        String loginId = auto.getString("LoginId", "");
        if (loginId.isEmpty()) {
            return;
        }
        profileCall = Server.getInstance().getApi().getLoginInfo(loginId);
        profileCall.enqueue(new Callback<LoginInfo>() {
            @Override
            public void onResponse(Call<LoginInfo> call, Response<LoginInfo> response) {
                if (!response.isSuccessful() || response.body() == null || isFinishing()) {
                    return;
                }
                LoginInfo info = response.body();
                SharedPreferences.Editor editor = auto.edit();
                if (info.getR_nickname() != null && !info.getR_nickname().trim().isEmpty()) {
                    model.r_Nickname.setValue(info.getR_nickname());
                    editor.putString("r_nickname", info.getR_nickname());
                }
                if (info.getR_image() != null && !info.getR_image().trim().isEmpty()) {
                    model.r_Image.setValue(info.getR_image());
                    editor.putString("r_image", info.getR_image());
                }
                String clubName = info.getCb_name() == null ? "" : info.getCb_name();
                model.r_ClubName.setValue(clubName);
                editor.putString("r_clubname", clubName).apply();
                bindDrawerHeader(model);
            }

            @Override
            public void onFailure(Call<LoginInfo> call, Throwable error) {
                Log.d("ProfileApi", "Unable to refresh drawer profile", error);
            }
        });
    }

    private void updateContentAppBarBehavior(boolean useCustomHeader) {
        if (mainContent == null) {
            return;
        }
        CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) mainContent.getLayoutParams();
        params.setBehavior(useCustomHeader ? null : appBarScrollingBehavior);
        mainContent.setLayoutParams(params);
        mainContent.setTranslationY(0f);
        mainContent.requestLayout();
    }

    private String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    private void navigateTo(NavController navController, int destinationId) {
        if (navController.getCurrentDestination() == null || navController.getCurrentDestination().getId() != destinationId) {
            navController.navigate(destinationId);
        }
    }

    private void configureBackNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            systemBackCallback = this::handleBackNavigation;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    systemBackCallback
            );
        }
    }

    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }

    /** A screen with inner pages (settings) takes Back one page at a time before the app leaves it. */
    private boolean currentScreenHandlesBack() {
        androidx.fragment.app.Fragment host = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (host == null) return false;
        androidx.fragment.app.Fragment current = host.getChildFragmentManager().getPrimaryNavigationFragment();
        return current instanceof com.bike.trailgo.ui.BackHandler
                && ((com.bike.trailgo.ui.BackHandler) current).onBackPressed();
    }

    private void handleBackNavigation() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        }
        if (currentScreenHandlesBack()) {
            return;
        }
        if (navController != null && navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() != R.id.nav_home) {
            int destinationId = navController.getCurrentDestination().getId();
            if (isMainMenuDestination(destinationId)) {
                // Drawer destinations are app sections, not a history chain: Back always returns Home.
                if (!navController.popBackStack(R.id.nav_home, false)) {
                    navController.navigate(R.id.nav_home);
                }
            } else if (!navController.popBackStack()) {
                navController.navigate(R.id.nav_home);
            }
            return;
        }
        // Android's normal behavior is preserved only when the user is already Home.
        moveTaskToBack(true);
    }

    private boolean isMainMenuDestination(int destinationId) {
        return destinationId == R.id.nav_records
                || destinationId == R.id.nav_course_view
                || destinationId == R.id.nav_course_search
                || destinationId == R.id.nav_course_scrap
                || destinationId == R.id.nav_competition_view
                || destinationId == R.id.nav_mission_view
                || destinationId == R.id.nav_danger_view
                || destinationId == R.id.setting
                || destinationId == R.id.app_settings;
    }

    private void shareInvite(MainViewModel model) {
        String nickname = valueOrDefault(model.r_Nickname.getValue(), "TrailGO");
        Intent friend = new Intent();
        friend.setAction(Intent.ACTION_SEND);
        friend.setType("text/plain");
        friend.putExtra(Intent.EXTRA_SUBJECT, nickname + "\uB2D8\uC774 TrailGO\uC5D0 \uCD08\uB300\uD569\uB2C8\uB2E4");
        friend.putExtra(Intent.EXTRA_TEXT, "TrailGO\uC5D0\uC11C \uD568\uAED8 \uB77C\uC774\uB529\uD574\uC694.");
        Intent chooser = Intent.createChooser(friend, "\uCE5C\uAD6C\uCD08\uB300\uD558\uAE30");
        startActivity(chooser);
    }

    private void logout() {
        UserSessionManager.clearLocalUserData(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void logKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("키 해시는 ", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && systemBackCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(systemBackCallback);
        }
        if (profileCall != null) {
            profileCall.cancel();
        }
        super.onDestroy();
    }
}
