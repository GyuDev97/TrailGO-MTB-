package com.bike.trailgo;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.bike.trailgo.net.Server;
import com.bike.trailgo.setting.AuthTokens;
import com.kakao.sdk.common.KakaoSdk;
import com.kakao.vectormap.KakaoMapSdk;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.File;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class MtbCraftApplication extends Application {
    private static final String TAG = "MtbCraftApplication";
    private static final String KAKAO_APP_KEY_META_DATA = "com.kakao.sdk.AppKey";

    @Override
    public void onCreate() {
        super.onCreate();
        AuthTokens.init(this);
        installImageLoader();
        String appKey = getKakaoNativeAppKey();
        if (appKey == null || appKey.trim().isEmpty()) {
            Log.e(TAG, "Kakao native app key is missing.");
            return;
        }
        Log.d(TAG, "Using Kakao native app key " + maskAppKey(appKey));
        // The Map and Login SDKs use the same Kakao native app key.
        KakaoMapSdk.init(this, appKey);
        KakaoSdk.init(this, appKey);
    }

    /** Images from the TrailGo server (private ride photos) need the member's session like any other call. */
    private void installImageLoader() {
        OkHttpClient client = Server.getInstance().getClient().newBuilder()
                .cache(new Cache(new File(getCacheDir(), "picasso-cache"), 50L * 1024 * 1024))
                .build();
        Picasso.setSingletonInstance(new Picasso.Builder(this).downloader(new OkHttp3Downloader(client)).build());
    }

    private String maskAppKey(String appKey) {
        if (appKey.length() <= 8) {
            return "****";
        }
        return appKey.substring(0, 4) + "..." + appKey.substring(appKey.length() - 4);
    }

    private String getKakaoNativeAppKey() {
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(
                    getPackageName(),
                    PackageManager.GET_META_DATA
            );
            Bundle metaData = appInfo.metaData;
            if (metaData == null) {
                return null;
            }
            return metaData.getString(KAKAO_APP_KEY_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to read Kakao native app key.", e);
            return null;
        }
    }
}
