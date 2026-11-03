package com.bike.trailgo.setting;

import android.content.Context;

import com.bike.trailgo.ui.riding.RideLocalData;
import com.kakao.sdk.user.UserApiClient;

/** Keeps local logout cleanup consistent across the two settings entry points. */
public final class UserSessionManager {
    private UserSessionManager() {
    }

    public static boolean hasUnfinishedRide(Context context) {
        return RideLocalData.hasUnfinishedRide(context);
    }

    public static void clearLocalUserData(Context context) {
        Context appContext = context.getApplicationContext();
        // Kakao SDK keeps its token separately from SharedPreferences. Clear it on app logout too.
        UserApiClient.getInstance().logout(error -> kotlin.Unit.INSTANCE);
        // Ends the server session (best effort), forgets the tokens and clears "auto": the remembered
        // account/profile fields and SOS contact.
        AccountSession.signOut(appContext);
        // These profile customizations are associated with the signed-in rider, not the device.
        appContext.getSharedPreferences("profile_features", Context.MODE_PRIVATE).edit().clear().apply();
        RideLocalData.clear(appContext);
    }
}
