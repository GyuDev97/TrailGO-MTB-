package com.bike.webapp.auth;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Naver: the profile call is the verification; its {@code response.id} is the stable user id. */
@Component
public class NaverVerifier implements SocialVerifier {

	static final String ME_URL = "https://openapi.naver.com/v1/nid/me";

	private final SocialApiClient api;
	private final ObjectMapper mapper = new ObjectMapper();

	@Autowired
	public NaverVerifier(SocialApiClient api) {
		this.api = api;
	}

	@Override
	public String provider() {
		return "naver";
	}

	@Override
	public Optional<SocialProfile> verify(String accessToken) {
		try {
			SocialApiClient.Response me = api.get(ME_URL, accessToken);
			if (me.status == 401 || me.status == 403) {
				return Optional.empty();
			}
			if (me.status != 200) {
				throw new SocialUnavailableException("naver me answered " + me.status);
			}
			JsonNode json = mapper.readTree(me.body);
			if (!"00".equals(json.path("resultcode").asText())) {
				return Optional.empty();
			}
			JsonNode profile = json.path("response");
			String id = profile.path("id").asText("");
			if (id.isEmpty() || !id.matches("[A-Za-z0-9_-]{1,100}")) {
				return Optional.empty();
			}
			return Optional.of(new SocialProfile("naver", id, text(profile, "nickname"), text(profile, "profile_image")));
		} catch (IOException e) {
			throw new SocialUnavailableException("naver answer unreadable");
		}
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.path(field);
		return value.isTextual() && !value.asText().trim().isEmpty() ? value.asText().trim() : null;
	}
}
