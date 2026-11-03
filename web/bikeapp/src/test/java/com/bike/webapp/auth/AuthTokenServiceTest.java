package com.bike.webapp.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class AuthTokenServiceTest {

	/** A clock the test can move. */
	private static class MovableClock extends Clock {
		Instant now = Instant.parse("2026-11-01T00:00:00Z");

		@Override
		public java.time.ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}

	private final FakeAuthStore store = new FakeAuthStore();
	private final MovableClock clock = new MovableClock();
	private final AuthTokenService service = new AuthTokenService(store, clock);

	@Test
	void anIssuedAccessTokenIdentifiesTheRider() {
		AuthTokenService.Session session = service.issue("kakao_1");
		assertEquals(Optional.of("kakao_1"), service.resolveAccess(session.accessToken));
		assertEquals(clock.now.plus(AuthTokenService.ACCESS_TTL).toEpochMilli(), session.accessTokenExpiresAtMillis);
	}

	@Test
	void tokensAreStoredOnlyAsHashes() {
		AuthTokenService.Session session = service.issue("kakao_1");
		assertFalse(store.tokens.containsKey(session.accessToken));
		assertFalse(store.tokens.containsKey(session.refreshToken));
		assertTrue(store.tokens.containsKey(AuthTokenService.sha256(session.accessToken)));
		assertNotEquals(session.accessToken, session.refreshToken);
	}

	@Test
	void unknownEmptyAndRefreshTokensAreNotAccessTokens() {
		AuthTokenService.Session session = service.issue("kakao_1");
		assertFalse(service.resolveAccess("nonsense").isPresent());
		assertFalse(service.resolveAccess("").isPresent());
		assertFalse(service.resolveAccess(null).isPresent());
		assertFalse(service.resolveAccess(session.refreshToken).isPresent());
	}

	@Test
	void anAccessTokenExpires() {
		AuthTokenService.Session session = service.issue("kakao_1");
		clock.now = clock.now.plus(AuthTokenService.ACCESS_TTL).plusSeconds(1);
		assertFalse(service.resolveAccess(session.accessToken).isPresent());
	}

	@Test
	void refreshGivesANewPairAndKeepsTheSameRider() {
		AuthTokenService.Session first = service.issue("kakao_1");
		clock.now = clock.now.plus(AuthTokenService.ACCESS_TTL).plusSeconds(5);
		AuthTokenService.Session second = service.refresh(first.refreshToken).get();
		assertEquals("kakao_1", second.riderId);
		assertNotEquals(first.accessToken, second.accessToken);
		assertEquals(Optional.of("kakao_1"), service.resolveAccess(second.accessToken));
	}

	@Test
	void aRefreshTokenWorksOnlyOnce_andReuseEndsTheWholeSignIn() {
		AuthTokenService.Session first = service.issue("kakao_1");
		AuthTokenService.Session second = service.refresh(first.refreshToken).get();

		assertFalse(service.refresh(first.refreshToken).isPresent()); // replayed copy
		// the family is revoked: even the newest tokens stop working
		assertFalse(service.resolveAccess(second.accessToken).isPresent());
		assertFalse(service.refresh(second.refreshToken).isPresent());
	}

	@Test
	void anExpiredRefreshTokenIsRefused() {
		AuthTokenService.Session session = service.issue("kakao_1");
		clock.now = clock.now.plus(AuthTokenService.REFRESH_TTL).plusSeconds(1);
		assertFalse(service.refresh(session.refreshToken).isPresent());
	}

	@Test
	void anAccessTokenCannotBeUsedToRefresh() {
		AuthTokenService.Session session = service.issue("kakao_1");
		assertFalse(service.refresh(session.accessToken).isPresent());
		assertFalse(service.refresh("").isPresent());
		assertFalse(service.refresh(null).isPresent());
	}

	@Test
	void revokeEndsTheSignInForBothTokens() {
		AuthTokenService.Session session = service.issue("kakao_1");
		service.revoke(session.accessToken);
		assertFalse(service.resolveAccess(session.accessToken).isPresent());
		assertFalse(service.refresh(session.refreshToken).isPresent());
	}

	@Test
	void anotherSignInOfTheSameRiderIsNotAffectedByRevoke() {
		AuthTokenService.Session phone = service.issue("kakao_1");
		AuthTokenService.Session tablet = service.issue("kakao_1");
		service.revoke(phone.refreshToken);
		assertFalse(service.resolveAccess(phone.accessToken).isPresent());
		assertTrue(service.resolveAccess(tablet.accessToken).isPresent());
	}
}
