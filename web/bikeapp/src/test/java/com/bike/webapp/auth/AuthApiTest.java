package com.bike.webapp.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.bike.webapp.controller.restful.AndroidController;
import com.bike.webapp.dto.App_RidingRecord;
import com.bike.webapp.service.AndroidService;
import com.bike.webapp.weather.WeatherCaptureService;

/** Sign-in, refresh, logout, and what the bearer token does to the app's own-data endpoints. */
class AuthApiTest {

	private FakeAuthStore store;
	private AuthTokenService tokens;
	private SocialVerifier kakao;
	private AndroidService androidService;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		store = new FakeAuthStore();
		tokens = new AuthTokenService(store);
		kakao = mock(SocialVerifier.class);
		when(kakao.provider()).thenReturn("kakao");
		SocialVerifier naver = mock(SocialVerifier.class);
		when(naver.provider()).thenReturn("naver");

		AuthController auth = new AuthController(Arrays.asList(kakao, naver), new MemberAccounts(store), tokens);

		androidService = mock(AndroidService.class);
		AndroidController app = new AndroidController();
		ReflectionTestUtils.setField(app, "androidService", androidService);
		ReflectionTestUtils.setField(app, "weatherCaptureService", mock(WeatherCaptureService.class));
		ReflectionTestUtils.setField(app, "recordPhotoService", mock(com.bike.webapp.service.RecordPhotoService.class));
		ReflectionTestUtils.setField(app, "storageRoot", "build/test-storage");

		mvc = MockMvcBuilders.standaloneSetup(auth, app).addFilters(new AppAuthFilter(tokens)).build();
	}

	private String signInAs(String kakaoId) throws Exception {
		when(kakao.verify("kakao-token")).thenReturn(
				Optional.of(new SocialProfile("kakao", kakaoId, "자전거", "http://img")));
		return mvc.perform(post("/app/auth/kakao").param("access_token", "kakao-token"))
				.andReturn().getResponse().getContentAsString();
	}

	private static String field(String json, String name) throws Exception {
		return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get(name).asText();
	}

	// ---- sign-in --------------------------------------------------------------------------------

	@Test
	void aVerifiedKakaoTokenGivesASessionAndTheRider() throws Exception {
		when(kakao.verify("kakao-token")).thenReturn(Optional.of(new SocialProfile("kakao", "123", "자전거", "http://img")));
		mvc.perform(post("/app/auth/kakao").param("access_token", "kakao-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.riderId").value("kakao_123"))
				.andExpect(jsonPath("$.newMember").value(true))
				.andExpect(jsonPath("$.nickname").value("자전거"))
				.andExpect(jsonPath("$.imageUrl").value("http://img"))
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty())
				.andExpect(jsonPath("$.accessTokenExpiresAt").isNumber());
	}

	@Test
	void aTokenTheProviderDoesNotAcceptIsUnauthorizedAndCreatesNothing() throws Exception {
		when(kakao.verify("bad")).thenReturn(Optional.empty());
		mvc.perform(post("/app/auth/kakao").param("access_token", "bad")).andExpect(status().isUnauthorized());
		assertTrue(store.riders.isEmpty());
		assertTrue(store.tokens.isEmpty());
	}

	@Test
	void aProviderOutageIsServiceUnavailable() throws Exception {
		when(kakao.verify(any())).thenThrow(new SocialUnavailableException("down"));
		mvc.perform(post("/app/auth/kakao").param("access_token", "x")).andExpect(status().isServiceUnavailable());
	}

	@Test
	void unknownProvidersAndEmptyTokensAreRejected() throws Exception {
		mvc.perform(post("/app/auth/google").param("access_token", "x")).andExpect(status().isNotFound());
		mvc.perform(post("/app/auth/kakao").param("access_token", "  ")).andExpect(status().isBadRequest());
		mvc.perform(post("/app/auth/kakao")).andExpect(status().isBadRequest());
		verify(kakao, never()).verify(any());
	}

	@Test
	void signInIsRateLimited() throws Exception {
		when(kakao.verify(any())).thenReturn(Optional.empty());
		int tooMany = 0;
		for (int i = 0; i < 40; i++) {
			if (mvc.perform(post("/app/auth/kakao").param("access_token", "x")).andReturn().getResponse()
					.getStatus() == 429) {
				tooMany++;
			}
		}
		assertTrue(tooMany > 0);
	}

	// ---- refresh / logout -------------------------------------------------------------------------

	@Test
	void refreshExchangesTheTokenOnce() throws Exception {
		String signIn = signInAs("123");
		String refresh = field(signIn, "refreshToken");

		String renewed = mvc.perform(post("/app/auth/refresh").param("refresh_token", refresh))
				.andExpect(status().isOk()).andExpect(jsonPath("$.riderId").value("kakao_123"))
				.andReturn().getResponse().getContentAsString();
		assertFalse(field(renewed, "accessToken").equals(field(signIn, "accessToken")));

		mvc.perform(post("/app/auth/refresh").param("refresh_token", refresh)).andExpect(status().isUnauthorized());
		mvc.perform(post("/app/auth/refresh").param("refresh_token", "nonsense")).andExpect(status().isUnauthorized());
	}

	@Test
	void logoutEndsTheSessionOnTheServer() throws Exception {
		String signIn = signInAs("123");
		String access = field(signIn, "accessToken");
		String refresh = field(signIn, "refreshToken");

		mvc.perform(post("/app/auth/logout").param("refresh_token", refresh)
				.header("Authorization", "Bearer " + access)).andExpect(status().isNoContent());
		mvc.perform(get("/app/riding/records/kakao_123").header("Authorization", "Bearer " + access))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/app/auth/refresh").param("refresh_token", refresh)).andExpect(status().isUnauthorized());
		mvc.perform(post("/app/auth/logout")).andExpect(status().isNoContent()); // always succeeds
	}

	// ---- the token decides who a call is about ------------------------------------------------------

	private App_RidingRecord record() {
		App_RidingRecord record = new App_RidingRecord();
		record.setRr_num(1);
		return record;
	}

	@Test
	void ownRecordsAreServedToTheSignedInRider() throws Exception {
		String access = field(signInAs("123"), "accessToken");
		when(androidService.readRecord("kakao_123")).thenReturn(Collections.singletonList(record()));
		mvc.perform(get("/app/riding/records/kakao_123").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].rr_num").value(1));
		mvc.perform(get("/app/get/kakao_123").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
	}

	@Test
	void anotherRidersDataIsForbiddenEvenWithAValidSession() throws Exception {
		String access = field(signInAs("123"), "accessToken");
		mvc.perform(get("/app/riding/records/kakao_999").header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden());
		mvc.perform(get("/app/riding/records/kakao_999/1").header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden());
		mvc.perform(post("/app/riding/records/kakao_999/1/memo").param("memo", "x")
				.header("Authorization", "Bearer " + access)).andExpect(status().isForbidden());
		mvc.perform(post("/app/riding/upload").param("rr_rider", "kakao_999")
				.header("Authorization", "Bearer " + access)).andExpect(status().isForbidden());
		verify(androidService, never()).readRecord("kakao_999");
		verify(androidService, never()).updateMemo(any(), org.mockito.ArgumentMatchers.anyInt(), any());
	}

	@Test
	void withoutASessionOwnDataIsUnauthorized() throws Exception {
		mvc.perform(get("/app/riding/records/kakao_123")).andExpect(status().isUnauthorized());
		mvc.perform(get("/app/get/kakao_123")).andExpect(status().isUnauthorized());
		mvc.perform(post("/app/riding/upload").param("rr_rider", "kakao_123")).andExpect(status().isUnauthorized());
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5").param("riderId", "kakao_123"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void aBadOrExpiredTokenIsUnauthorizedSoTheAppKnowsToRefresh() throws Exception {
		mvc.perform(get("/app/riding/records/kakao_123").header("Authorization", "Bearer forged"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.containsString("invalid_token")));
		// even a call that would be public is not silently downgraded to a guest
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5")
				.header("Authorization", "Bearer forged")).andExpect(status().isUnauthorized());
	}

	@Test
	void aRefreshTokenIsNotAcceptedAsAnAccessToken() throws Exception {
		String refresh = field(signInAs("123"), "refreshToken");
		mvc.perform(get("/app/riding/records/kakao_123").header("Authorization", "Bearer " + refresh))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void guestsStillReachThePublicEndpointsWithoutAToken() throws Exception {
		// no riderId, no Authorization header: the weather lookup is available to guests
		mvc.perform(get("/app/weather").param("lat", "35.8").param("lon", "128.5"))
				.andExpect(result -> assertTrue(result.getResponse().getStatus() != 401
						&& result.getResponse().getStatus() != 403));
	}
}
