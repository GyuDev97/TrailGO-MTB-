package com.bike.trailgo.setting;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.bike.trailgo.dto.AuthSession;

/**
 * The server session of a signed-in member. The tokens live in encrypted preferences (Android
 * Keystore); if that is not available on a device they are simply not stored and the member signs
 * in again, they are never written in plain text.
 */
public final class AuthTokens {
    private static final String FILE = "trailgo_session";
    private static final String KEY_ACCESS = "access";
    private static final String KEY_ACCESS_EXPIRES = "access_expires";
    private static final String KEY_REFRESH = "refresh";

    private static Context appContext;
    private static SharedPreferences prefs;

    private AuthTokens() {
    }

    public static synchronized void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static synchronized void save(AuthSession session) {
        SharedPreferences store = store();
        if (store == null || !session.isComplete()) {
            return;
        }
        store.edit()
                .putString(KEY_ACCESS, session.getAccessToken())
                .putLong(KEY_ACCESS_EXPIRES, session.getAccessTokenExpiresAt())
                .putString(KEY_REFRESH, session.getRefreshToken())
                .apply();
    }

    @Nullable
    public static synchronized String accessToken() {
        SharedPreferences store = store();
        return store == null ? null : store.getString(KEY_ACCESS, null);
    }

    @Nullable
    public static synchronized String refreshToken() {
        SharedPreferences store = store();
        return store == null ? null : store.getString(KEY_REFRESH, null);
    }

    public static synchronized boolean hasSession() {
        String refresh = refreshToken();
        return refresh != null && !refresh.isEmpty();
    }

    public static synchronized void clear() {
        SharedPreferences store = store();
        if (store != null) {
            store.edit().clear().apply();
        }
    }

    @Nullable
    private static SharedPreferences store() {
        if (prefs != null) {
            return prefs;
        }
        if (appContext == null) {
            return null;
        }
        try {
            String key = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(FILE, key, appContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            // Keystore unavailable or its data unreadable (e.g. restored backup): no session.
            appContext.deleteSharedPreferences(FILE);
            prefs = null;
        }
        return prefs;
    }
}
