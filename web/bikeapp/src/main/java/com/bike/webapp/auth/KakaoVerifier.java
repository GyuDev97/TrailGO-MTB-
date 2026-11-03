package com.bike.webapp.auth;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Kakao: the token must have been issued for <em>this</em> Kakao app ({@code app_id} of
 * access_token_info), otherwise a token that another service obtained from the same person could be
 * replayed here. Without a configured app id this server refuses Kakao sign-in altogether.
 */
@Component
public class KakaoVerifier implements SocialVerifier {

	static final String TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info";
	static final String ME_URL = "https://kapi.kakao.com/v2/user/me";

	private final SocialApiClient api;
	private final String appId;
	private final ObjectMapper mapper = new ObjectMapper();

	@Autowired
	public KakaoVerifier(SocialApiClient api, @Value("${trailgo.auth.kakao-app-id:}") String appId) {
		this.api = api;
		this.appId = appId == null ? "" : appId.trim();
	}

	@Override
	public String provider() {
		return "kakao";
	}

	@Override
	public Optional<SocialProfile> verify(String accessToken) {
		if (appId.isEmpty()) {
			throw new SocialUnavailableException("kakao app id is not configured");
		}
		try {
			SocialApiClient.Response info = api.get(TOKEN_INFO_URL, accessToken);
			if (info.status == 401 || info.status == 400) {
				return Optional.empty();
			}
			if (info.status != 200) {
				throw new SocialUnavailableException("kakao token info answered " + info.status);
			}
			JsonNode infoJson = mapper.readTree(info.body);
			if (!appId.equals(infoJson.path("app_id").asText())) {
				return Optional.empty();
			}

			SocialApiClient.Response me = api.get(ME_URL, accessToken);
			if (me.status == 401 || me.status == 400) {
				return Optional.empty();
			}
			if (me.status != 200) {
				throw new SocialUnavailableException("kakao me answered " + me.status);
			}
			JsonNode meJson = mapper.readTree(me.body);
			String id = meJson.path("id").asText("");
			if (id.isEmpty() || !id.matches("[0-9]{1,20}")) {
				return Optional.empty();
			}
			JsonNode profile = meJson.path("kakao_account").path("profile");
			return Optional.of(new SocialProfile("kakao", id,
					text(profile, "nickname"), text(profile, "thumbnail_image_url")));
		} catch (IOException e) {
			throw new SocialUnavailableException("kakao answer unreadable");
		}
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.path(field);
		return value.isTextual() && !value.asText().trim().isEmpty() ? value.asText().trim() : null;
	}
}
