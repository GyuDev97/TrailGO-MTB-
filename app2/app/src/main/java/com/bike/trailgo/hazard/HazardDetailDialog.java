package com.bike.trailgo.hazard;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.bike.trailgo.net.Server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** The info card for a tapped hazard zone. Unverified reports are plainly labelled as such. */
public final class HazardDetailDialog {
    private HazardDetailDialog() {
    }

    public static String describe(Hazard hazard) {
        HazardType type = hazard.hazardType();
        StringBuilder text = new StringBuilder();
        text.append(type.glyph).append(' ').append(type.label);
        text.append("\n반경 ").append(hazard.effectiveRadiusM()).append("m");
        if (hazard.description != null && !hazard.description.trim().isEmpty()) {
            text.append("\n\n").append(hazard.description.trim());
        }
        if (hazard.createdAtMs > 0) {
            text.append("\n\n제보일 ").append(new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
                    .format(new Date(hazard.createdAtMs)));
        }
        text.append('\n').append(hazard.verified
                ? "운영자가 확인한 위험지역입니다."
                : "라이더가 제보한 내용으로, 아직 운영자가 확인하지 않았습니다.");
        return text.toString();
    }

    /** @param onWithdrawn called after the rider's own report was withdrawn on the server */
    public static void show(final Context context, final Hazard hazard, final Runnable onWithdrawn) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(hazard.title == null || hazard.title.isEmpty() ? hazard.hazardType().label : hazard.title)
                .setMessage(describe(hazard))
                .setPositiveButton("닫기", null);
        if (hazard.mine) {
            builder.setNeutralButton("제보 철회", (dialog, which) -> withdraw(context, hazard, onWithdrawn));
        }
        builder.show();
    }

    private static void withdraw(final Context context, final Hazard hazard, final Runnable onWithdrawn) {
        try {
            Server.getInstance().getApi().deleteHazard(hazard.id).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() || response.code() == 404) {
                        HazardRepository.get(context).onWithdrawn(hazard.id);
                        Toast.makeText(context, "제보를 철회했습니다.", Toast.LENGTH_SHORT).show();
                        if (onWithdrawn != null) onWithdrawn.run();
                    } else {
                        Toast.makeText(context, "철회하지 못했습니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Toast.makeText(context, "네트워크에 연결할 수 없어 철회하지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (RuntimeException e) {
            Toast.makeText(context, "철회하지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
