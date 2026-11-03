package com.bike.webapp.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class SocialVerifierTest {

	/** Answers per URL; remembers which tokens were sent. */
	private static class FakeApi implements SocialApiClient {
		final Map<String, Response> answers = new HashMap<String, Response>();
		final List<String> calls = new ArrayList<String>();
		boolean down;

		@Override
		public Response get(String url, String bearerToken) {
			calls.add(url + " <- " + bearerToken);
			if (down) {
				throw new SocialUnavailableException("down");
			}
			Response answer = answers.get(url);
			return answer == null ? new Response(404, "") : answer;
		}
	}

	private final FakeApi api = new FakeApi();

	private void kakaoAnswers(int infoStatus, String infoBody, int meStatus, String meBody) {
		api.answers.put(KakaoVerifier.TOKEN_INFO_URL, new SocialApiClient.Response(infoStatus, infoBody));
		api.answers.put(KakaoVerifier.ME_URL, new SocialApiClient.Response(meStatus, meBody));
	}

	private static final String KAKAO_INFO = "{\"id\":123456789,\"expires_in\":7000,\"app_id\":1234567}";
	private static final String KAKAO_ME = "{\"id\":123456789,\"kakao_account\":{\"profile\":"
			+ "{\"nickname\":\"자전거\",\"thumbnail_image_url\":\"http://k.kakaocdn.net/t.jpg\"}}}";

	@Test
	void kakaoTokenOfThisAppGivesTheProfile() {
		kakaoAnswers(200, KAKAO_INFO, 200, KAKAO_ME);
		SocialProfile profile = new KakaoVerifier(api, "1234567").verify("tok").get();
		assertEquals("kakao", profile.getProvider());
		assertEquals("123456789", profile.getUserId());
		assertEquals("자전거", profile.getNickname());
		assertEquals("http://k.kakaocdn.net/t.jpg", profile.getImageUrl());
	}

	@Test
	void kakaoTokenOfAnotherAppIsRefusedBeforeAnythingIsRead() {
		kakaoAnswers(200, "{\"id\":1,\"app_id\":999}", 200, KAKAO_ME);
		assertFalse(new KakaoVerifier(api, "1234567").verify("tok").isPresent());
		assertEquals(1, api.calls.size()); // the profile was never requested
	}

	@Test
	void kakaoRefusesEverythingWithoutAConfiguredAppId() {
		kakaoAnswers(200, KAKAO_INFO, 200, KAKAO_ME);
		assertThrows(SocialUnavailableException.class, () -> new KakaoVerifier(api, "").verify("tok"));
		assertThrows(SocialUnavailableException.class, () -> new KakaoVerifier(api, null).verify("tok"));
		assertTrue(api.calls.isEmpty());
	}

	@Test
	void kakaoInvalidTokenIsEmptyButAnOutageIsNot() {
		kakaoAnswers(401, "{\"code\":-401}", 401, "");
		assertFalse(new KakaoVerifier(api, "1234567").verify("bad").isPresent());

		kakaoAnswers(500, "", 500, "");
		assertThrows(SocialUnavailableException.class, () -> new KakaoVerifier(api, "1234567").verify("tok"));
		api.down = true;
		assertThrows(SocialUnavailableException.class, () -> new KakaoVerifier(api, "1234567").verify("tok"));
	}

	@Test
	void kakaoProfileWithoutNicknameStillSignsIn() {
		kakaoAnswers(200, KAKAO_INFO, 200, "{\"id\":42}");
		SocialProfile profile = new KakaoVerifier(api, "1234567").verify("tok").get();
		assertEquals("42", profile.getUserId());
		assertNull(profile.getNickname());
		assertNull(profile.getImageUrl());
	}

	@Test
	void kakaoIdMustBeANumber() {
		kakaoAnswers(200, KAKAO_INFO, 200, "{\"id\":\"abc; drop\"}");
		assertFalse(new KakaoVerifier(api, "1234567").verify("tok").isPresent());
	}

	@Test
	void naverProfileGivesTheStableId() {
		api.answers.put(NaverVerifier.ME_URL, new SocialApiClient.Response(200,
				"{\"resultcode\":\"00\",\"message\":\"success\",\"response\":{\"id\":\"AbC-dEf_123\","
						+ "\"nickname\":\"산악\",\"profile_image\":\"https://p.pstatic.net/a.png\"}}"));
		Optional<SocialProfile> profile = new NaverVerifier(api).verify("tok");
		assertEquals("AbC-dEf_123", profile.get().getUserId());
		assertEquals("naver", profile.get().getProvider());
		assertEquals("산악", profile.get().getNickname());
	}

	@Test
	void naverInvalidTokenIsEmptyAndOutageIsReported() {
		api.answers.put(NaverVerifier.ME_URL, new SocialApiClient.Response(401, "{\"resultcode\":\"024\"}"));
		assertFalse(new NaverVerifier(api).verify("bad").isPresent());

		api.answers.put(NaverVerifier.ME_URL, new SocialApiClient.Response(200, "{\"resultcode\":\"024\"}"));
		assertFalse(new NaverVerifier(api).verify("bad").isPresent());

		api.answers.put(NaverVerifier.ME_URL, new SocialApiClient.Response(503, ""));
		assertThrows(SocialUnavailableException.class, () -> new NaverVerifier(api).verify("tok"));

		api.answers.put(NaverVerifier.ME_URL, new SocialApiClient.Response(200, "not json"));
		assertThrows(SocialUnavailableException.class, () -> new NaverVerifier(api).verify("tok"));
	}
}
