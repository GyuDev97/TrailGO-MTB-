package com.bike.trailgo.ui.riding;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

/** Safe, user-confirmed emergency contact actions. No direct calls or SMS are sent. */
final class SosHelper {
    private static final String PREFS_AUTO = "auto";
    private static final String CONTACT_KEY = "sosphone";
    private static final String EMERGENCY_SERVICES_NUMBER = "119";
    private static final long ACTION_COOLDOWN_MILLIS = 3_000L;

    private SosHelper() {
    }

    static void confirmEmergencyServiceCall(Activity activity) {
        confirmDial(activity, "119 긴급 전화", EMERGENCY_SERVICES_NUMBER);
    }

    static void confirmEmergencyServiceMessage(Activity activity, String message) {
        confirmSms(activity, "119 긴급 문자", EMERGENCY_SERVICES_NUMBER, message);
    }

    static void confirmContactCall(Activity activity) {
        String contact = getConfiguredContact(activity);
        if (contact == null) {
            promptToConfigureContact(activity);
            return;
        }
        confirmDial(activity, "긴급 연락처 전화", contact);
    }

    static void confirmContactMessage(Activity activity, String message) {
        String contact = getConfiguredContact(activity);
        if (contact == null) {
            promptToConfigureContact(activity);
            return;
        }
        confirmSms(activity, "긴급 연락처 문자", contact, message);
    }

    static String buildEmergencyMessage(String address, double latitude, double longitude) {
        StringBuilder message = new StringBuilder("긴급 상황입니다. 도움이 필요합니다.");
        if (address != null && !address.trim().isEmpty()) {
            message.append("\n현재 위치: ").append(address.trim());
        }
        if (isValidLocation(latitude, longitude)) {
            message.append("\n좌표: ").append(latitude).append(", ").append(longitude);
            message.append("\n지도: https://maps.google.com/?q=")
                    .append(latitude).append(",").append(longitude);
        } else {
            message.append("\n현재 GPS 위치를 확인하지 못했습니다.");
        }
        return message.toString();
    }

    private static void confirmDial(Activity activity, String title, String phoneNumber) {
        if (!isUsable(activity)) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(phoneNumber + " 번호의 전화 앱을 엽니다. 통화는 다음 화면에서 직접 눌러 주세요.")
                .setNegativeButton("취소", null)
                .setPositiveButton("전화 앱 열기", (dialog, which) -> {
                    if (!isUsable(activity) || !beginAction(activity)) {
                        return;
                    }
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber)));
                    } catch (ActivityNotFoundException exception) {
                        Toast.makeText(activity, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private static void confirmSms(Activity activity, String title, String phoneNumber, String message) {
        if (!isUsable(activity)) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(phoneNumber + " 번호로 보낼 긴급 문자를 기본 문자 앱에서 확인합니다. 전송은 다음 화면에서 직접 눌러 주세요.")
                .setNegativeButton("취소", null)
                .setPositiveButton("문자 앱 열기", (dialog, which) -> {
                    if (!isUsable(activity) || !beginAction(activity)) {
                        return;
                    }
                    try {
                        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phoneNumber));
                        intent.putExtra("sms_body", message);
                        activity.startActivity(intent);
                    } catch (ActivityNotFoundException exception) {
                        Toast.makeText(activity, "문자 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private static String getConfiguredContact(Context context) {
        String value = context.getSharedPreferences(PREFS_AUTO, Context.MODE_PRIVATE)
                .getString(CONTACT_KEY, "");
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9+*#]", "");
        return normalized.length() >= 3 ? normalized : null;
    }

    private static void promptToConfigureContact(Activity activity) {
        if (!isUsable(activity)) {
            return;
        }
        EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setSingleLine(true);
        input.setText(activity.getSharedPreferences(PREFS_AUTO, Context.MODE_PRIVATE)
                .getString(CONTACT_KEY, ""));
        input.setSelection(input.length());

        new AlertDialog.Builder(activity)
                .setTitle("긴급 연락처 설정")
                .setMessage("지정 연락처 기능을 사용하려면 전화번호를 등록해 주세요.")
                .setView(input)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", (dialog, which) -> {
                    String phone = input.getText().toString().trim();
                    activity.getSharedPreferences(PREFS_AUTO, Context.MODE_PRIVATE)
                            .edit()
                            .putString(CONTACT_KEY, phone)
                            .apply();
                    Toast.makeText(activity, "긴급 연락처를 저장했습니다. 다시 요청해 주세요.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private static boolean beginAction(Activity activity) {
        SharedPreferences preferences = activity.getSharedPreferences("sos_action", Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long lastActionAt = preferences.getLong("last_action_at", 0L);
        if (now - lastActionAt < ACTION_COOLDOWN_MILLIS) {
            Toast.makeText(activity, "긴급 요청을 준비 중입니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }
        preferences.edit().putLong("last_action_at", now).apply();
        return true;
    }

    private static boolean isValidLocation(double latitude, double longitude) {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude)
                && latitude >= -90d && latitude <= 90d
                && longitude >= -180d && longitude <= 180d
                && !(latitude == 0d && longitude == 0d);
    }

    private static boolean isUsable(Activity activity) {
        return !activity.isFinishing() && !activity.isDestroyed();
    }
}
