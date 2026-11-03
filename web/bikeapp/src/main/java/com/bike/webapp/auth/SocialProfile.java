package com.bike.webapp.auth;

/** Who a social provider says the person is. Only what the provider returned for a verified token. */
public class SocialProfile {

	private final String provider;
	private final String userId;
	private final String nickname;
	private final String imageUrl;

	public SocialProfile(String provider, String userId, String nickname, String imageUrl) {
		this.provider = provider;
		this.userId = userId;
		this.nickname = nickname;
		this.imageUrl = imageUrl;
	}

	public String getProvider() {
		return provider;
	}

	public String getUserId() {
		return userId;
	}

	public String getNickname() {
		return nickname;
	}

	public String getImageUrl() {
		return imageUrl;
	}
}
