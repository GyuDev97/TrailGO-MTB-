package com.bike.webapp.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Issues and checks the app's own session tokens. Tokens are random and opaque; only their SHA-256
 * is stored. The access token is short-lived. The refresh token is single-use: presenting one a
 * second time (a copy was stolen, or replayed) revokes every token of that sign-in.
 */
@Service
public class AuthTokenService {

	static final Duration ACCESS_TTL = Duration.ofHours(1);
	static final Duration REFRESH_TTL = Duration.ofDays(90);
	private static final String ACCESS = "ACCESS";
	private static final String REFRESH = "REFRESH";

	private final AuthStore store;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	@Autowired
	public AuthTokenService(AuthStore store) {
		this(store, Clock.systemUTC());
	}

	AuthTokenService(AuthStore store, Clock clock) {
		this.store = store;
		this.clock = clock;
	}

	public static class Session {
		public final String riderId;
		public final String accessToken;
		public final long accessTokenExpiresAtMillis;
		public final String refreshToken;

		Session(String riderId, String accessToken, long accessTokenExpiresAtMillis, String refreshToken) {
			this.riderId = riderId;
			this.accessToken = accessToken;
			this.accessTokenExpiresAtMillis = accessTokenExpiresAtMillis;
			this.refreshToken = refreshToken;
		}
	}

	/** A new sign-in: starts a new token family. */
	public Session issue(String riderId) {
		store.deleteExpiredTokens(clock.instant());
		return issueInFamily(riderId, UUID.randomUUID().toString().replace("-", ""));
	}

	/** Exchanges a refresh token for a new pair; empty when it is unknown, expired, revoked or reused. */
	public Optional<Session> refresh(String refreshToken) {
		if (refreshToken == null || refreshToken.isEmpty()) {
			return Optional.empty();
		}
		String hash = sha256(refreshToken);
		AuthStore.TokenRow row = store.findToken(hash);
		if (row == null || !REFRESH.equals(row.kind)) {
			return Optional.empty();
		}
		if (row.revoked) {
			store.revokeFamily(row.familyId); // reuse of a spent refresh token
			return Optional.empty();
		}
		if (!row.expiresAt.isAfter(clock.instant())) {
			return Optional.empty();
		}
		if (!store.revokeToken(hash)) {
			store.revokeFamily(row.familyId); // lost the race to another use of the same token
			return Optional.empty();
		}
		return Optional.of(issueInFamily(row.riderId, row.familyId));
	}

	/** The rider an access token belongs to, or empty when it is unknown, expired or revoked. */
	public Optional<String> resolveAccess(String accessToken) {
		if (accessToken == null || accessToken.isEmpty() || accessToken.length() > 200) {
			return Optional.empty();
		}
		AuthStore.TokenRow row = store.findToken(sha256(accessToken));
		if (row == null || !ACCESS.equals(row.kind) || row.revoked || !row.expiresAt.isAfter(clock.instant())) {
			return Optional.empty();
		}
		return Optional.of(row.riderId);
	}

	/** Ends the sign-in the given access or refresh token belongs to. */
	public void revoke(String token) {
		if (token == null || token.isEmpty()) {
			return;
		}
		AuthStore.TokenRow row = store.findToken(sha256(token));
		if (row != null) {
			store.revokeFamily(row.familyId);
		}
	}

	private Session issueInFamily(String riderId, String familyId) {
		Instant now = clock.instant();
		String access = newToken();
		String refresh = newToken();
		Instant accessExpires = now.plus(ACCESS_TTL);
		store.saveToken(sha256(access), ACCESS, riderId, familyId, accessExpires);
		store.saveToken(sha256(refresh), REFRESH, riderId, familyId, now.plus(REFRESH_TTL));
		return new Session(riderId, access, accessExpires.toEpochMilli(), refresh);
	}

	private String newToken() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String sha256(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(64);
			for (byte b : digest) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
