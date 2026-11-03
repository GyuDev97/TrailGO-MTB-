package com.bike.trailgo.ui.riding;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bike.trailgo.R;
import com.bike.trailgo.rideassistant.RideAlertKind;
import com.bike.trailgo.setting.RideSettings;

/**
 * What the rider feels and sees of an alert besides the voice: a vibration (when switched on) and a
 * heads-up notification, so a warning is not lost with the screen off or the sound muted.
 */
final class RideAlertFeedback {
    private static final String CHANNEL_ID = "ride_alerts";
    private static final long[] PATTERN = {0, 300, 150, 300};
    private static final long NOTIFICATION_TIMEOUT_MS = 60_000L;

    private RideAlertFeedback() {
    }

    static void alert(Context context, RideAlertKind kind, String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        if (RideSettings.isVibrationEnabled(context)) {
            vibrate(context);
        }
        notifyRider(context, kind, message);
    }

    private static void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(PATTERN, -1));
            } else {
                vibrator.vibrate(PATTERN, -1);
            }
        } catch (RuntimeException ignored) {
            // a vibration that cannot be played must never break the ride service
        }
    }

    private static void notifyRider(Context context, RideAlertKind kind, String message) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            return;
        }
        ensureChannel(context);
        try {
            manager.notify(kind.notificationId, new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.riding_ic_pin)
                    .setContentTitle(kind.title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setTimeoutAfter(NOTIFICATION_TIMEOUT_MS)
                    .build());
        } catch (SecurityException ignored) {
            // notification permission revoked between the check and the call
        }
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "주행 중 알림",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("위험지역 접근, 코스 이탈, 배터리 부족 알림");
        // the vibration is the app's own (and can be switched off in the app), not the channel's
        channel.enableVibration(false);
        manager.createNotificationChannel(channel);
    }
}
