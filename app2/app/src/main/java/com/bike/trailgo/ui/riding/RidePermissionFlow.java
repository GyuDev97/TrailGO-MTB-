package com.bike.trailgo.ui.riding;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/** Feature-scoped runtime permission requests for the riding experience. */
final class RidePermissionFlow {
    static final int REQUEST_RIDE_LOCATION = 7101;
    static final int REQUEST_BACKGROUND_LOCATION = 7102;
    static final int REQUEST_MICROPHONE = 7103;

    private static final String PREFS = "ride_permission_flow";

    private RidePermissionFlow() {
    }

    static boolean hasPreciseLocation(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    static boolean hasBackgroundLocation(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    static void requestRideLocation(Activity activity) {
        requestWithExplanation(
                activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_RIDE_LOCATION,
                "정확한 위치 권한이 필요합니다",
                "주행 거리와 GPX 경로를 기록하기 위해 정확한 위치를 사용합니다."
        );
    }

    /**
     * Foreground tracking works with the persistent foreground notification.  Background
     * location is only requested for the extra recovery case where Android must recreate a
     * long-running tracking service while no activity is visible.
     */
    static void requestBackgroundLocationForLongRide(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || !hasPreciseLocation(activity)
                || hasBackgroundLocation(activity)) {
            return;
        }

        SharedPreferences preferences = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (preferences.getBoolean("background_location_explained", false)) {
            return;
        }
        preferences.edit().putBoolean("background_location_explained", true).apply();

        new AlertDialog.Builder(activity)
                .setTitle("화면이 꺼진 장시간 라이딩 기록")
                .setMessage("주행 중 시스템이 위치 서비스를 다시 시작해야 하는 경우에도 기록을 이어가려면 위치를 '항상 허용'으로 설정할 수 있습니다. 허용하지 않아도 현재 화면에서 시작한 주행은 계속 기록됩니다.")
                .setNegativeButton("나중에", null)
                .setPositiveButton("계속", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                        ActivityCompat.requestPermissions(
                                activity,
                                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                REQUEST_BACKGROUND_LOCATION
                        );
                    } else {
                        openAppPermissionSettings(activity);
                    }
                })
                .show();
    }

    static void requestMicrophone(Activity activity) {
        requestWithExplanation(
                activity,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_MICROPHONE,
                "음성 명령을 사용하시겠습니까?",
                "주행 중 음성 명령을 인식할 때만 마이크를 사용합니다."
        );
    }

    static void showDenied(Activity activity, String feature) {
        new AlertDialog.Builder(activity)
                .setTitle(feature + " 권한이 필요합니다")
                .setMessage("권한을 허용하지 않아 이 기능은 실행하지 않았습니다. 설정에서 권한을 허용한 뒤 다시 시도할 수 있습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("설정", (dialog, which) -> openAppPermissionSettings(activity))
                .show();
    }

    private static void requestWithExplanation(
            Activity activity,
            String[] permissions,
            int requestCode,
            String title,
            String message
    ) {
        boolean granted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }
        if (granted) {
            return;
        }

        boolean permanentlyDenied = wasRequested(activity, permissions[0])
                && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[0]);
        if (permanentlyDenied) {
            showDenied(activity, title);
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("나중에", null)
                .setPositiveButton("허용", (dialog, which) -> {
                    markRequested(activity, permissions[0]);
                    ActivityCompat.requestPermissions(activity, permissions, requestCode);
                })
                .show();
    }

    private static boolean wasRequested(Context context, String permission) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("asked_" + permission, false);
    }

    private static void markRequested(Context context, String permission) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("asked_" + permission, true)
                .apply();
    }

    private static void openAppPermissionSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(intent);
    }
}
