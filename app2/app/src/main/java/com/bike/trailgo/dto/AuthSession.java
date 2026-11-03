package com.bike.trailgo.dto;

import androidx.annotation.Nullable;

/** The server's answer to a social sign-in or a token refresh. */
public class AuthSession {
    private String accessToken;
    private long accessTokenExpiresAt;
    private String refreshToken;
    private String riderId;
    private boolean newMember;
    private String nickname;
    private String imageUrl;

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    public long getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    @Nullable
    public String getRefreshToken() {
        return refreshToken;
    }

    @Nullable
    public String getRiderId() {
        return riderId;
    }

    public boolean isNewMember() {
        return newMember;
    }

    @Nullable
    public String getNickname() {
        return nickname;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isComplete() {
        return accessToken != null && !accessToken.isEmpty()
                && refreshToken != null && !refreshToken.isEmpty()
                && riderId != null && !riderId.isEmpty();
    }
}
