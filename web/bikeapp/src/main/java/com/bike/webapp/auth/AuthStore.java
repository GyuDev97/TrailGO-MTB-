package com.bike.webapp.auth;

import java.time.Instant;

/** Persistence behind social sign-in; a seam so the logic can be tested without a database. */
public interface AuthStore {

	/** The rider linked to this provider account, or null. */
	String findLinkedRider(String provider, String providerUserId);

	/** @return false when the provider account is already linked (lost a race) */
	boolean link(String provider, String providerUserId, String riderId);

	void touchLogin(String provider, String providerUserId);

	boolean riderExists(String riderId);

	/** Adds the rider row. The rider table only has r_id, r_pw and r_image; social riders have no password. */
	void createRider(String riderId);

	void saveToken(String tokenHash, String kind, String riderId, String familyId, Instant expiresAt);

	/** Null when unknown. */
	TokenRow findToken(String tokenHash);

	/** Marks one token revoked; true only for the call that actually changed it (single-use refresh). */
	boolean revokeToken(String tokenHash);

	void revokeFamily(String familyId);

	void deleteExpiredTokens(Instant now);

	class TokenRow {
		public final String kind;
		public final String riderId;
		public final String familyId;
		public final Instant expiresAt;
		public final boolean revoked;

		public TokenRow(String kind, String riderId, String familyId, Instant expiresAt, boolean revoked) {
			this.kind = kind;
			this.riderId = riderId;
			this.familyId = familyId;
			this.expiresAt = expiresAt;
			this.revoked = revoked;
		}
	}
}
