package com.bike.trailgo.net;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bike.trailgo.dto.AuthSession;
import com.bike.trailgo.setting.AuthTokens;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Puts the session token on requests to the TrailGo server, and only to it: images and anything
 * else loaded from other hosts (Picasso shares this client) never see the token. When the server
 * answers 401 the token is renewed once with the refresh token and the request is repeated.
 */
final class SessionAuth implements Interceptor, Authenticator {
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    /** Asks the server for a new token pair. Returns null when it could not be asked at all. */
    interface Refresher {
        @Nullable
        retrofit2.Response<AuthSession> refresh(String refreshToken) throws IOException;
    }

    private final HttpUrl serverUrl;
    private final Refresher refresher;

    SessionAuth(String baseUrl, Refresher refresher) {
        this.serverUrl = HttpUrl.parse(baseUrl);
        this.refresher = refresher;
    }

    private boolean isServer(HttpUrl url) {
        return serverUrl != null && url.host().equals(serverUrl.host()) && url.port() == serverUrl.port();
    }

    private static boolean isAuthEndpoint(HttpUrl url) {
        return url.encodedPath().contains("/app/auth/");
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        if (!isServer(request.url()) || isAuthEndpoint(request.url())) {
            return chain.proceed(request);
        }
        String token = AuthTokens.accessToken();
        if (token == null) {
            return chain.proceed(request);
        }
        return chain.proceed(request.newBuilder().header(AUTHORIZATION, BEARER + token).build());
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        Request failed = response.request();
        String sent = failed.header(AUTHORIZATION);
        if (sent == null || !isServer(failed.url()) || isAuthEndpoint(failed.url()) || attempts(response) >= 2) {
            return null;
        }
        String sentToken = sent.startsWith(BEARER) ? sent.substring(BEARER.length()) : sent;

        synchronized (AuthTokens.class) {
            String current = AuthTokens.accessToken();
            if (current != null && !current.equals(sentToken)) {
                // another request already renewed the session meanwhile
                return failed.newBuilder().header(AUTHORIZATION, BEARER + current).build();
            }
            String refreshToken = AuthTokens.refreshToken();
            if (refreshToken == null) {
                return null;
            }
            retrofit2.Response<AuthSession> answer = refresher.refresh(refreshToken);
            if (answer == null) {
                return null;
            }
            AuthSession session = answer.body();
            if (answer.isSuccessful() && session != null && session.isComplete()) {
                AuthTokens.save(session);
                return failed.newBuilder().header(AUTHORIZATION, BEARER + session.getAccessToken()).build();
            }
            if (answer.code() == 401) {
                // the refresh token is no longer valid: this sign-in is over
                AuthTokens.clear();
            }
            return null;
        }
    }

    private static int attempts(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}
