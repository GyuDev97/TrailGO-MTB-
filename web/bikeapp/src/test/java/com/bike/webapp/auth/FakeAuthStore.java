package com.bike.webapp.auth;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** In-memory {@link AuthStore} for the tests. */
class FakeAuthStore implements AuthStore {

	final Map<String, String> links = new HashMap<String, String>();
	final Set<String> riders = new HashSet<String>();
	final Map<String, TokenRow> tokens = new HashMap<String, TokenRow>();

	private static String key(String provider, String id) {
		return provider + ":" + id;
	}

	@Override
	public String findLinkedRider(String provider, String providerUserId) {
		return links.get(key(provider, providerUserId));
	}

	@Override
	public boolean link(String provider, String providerUserId, String riderId) {
		return links.putIfAbsent(key(provider, providerUserId), riderId) == null;
	}

	@Override
	public void touchLogin(String provider, String providerUserId) {
	}

	@Override
	public boolean riderExists(String riderId) {
		return riders.contains(riderId);
	}

	@Override
	public void createRider(String riderId) {
		riders.add(riderId);
	}

	@Override
	public void saveToken(String tokenHash, String kind, String riderId, String familyId, Instant expiresAt) {
		tokens.put(tokenHash, new TokenRow(kind, riderId, familyId, expiresAt, false));
	}

	@Override
	public TokenRow findToken(String tokenHash) {
		return tokens.get(tokenHash);
	}

	@Override
	public boolean revokeToken(String tokenHash) {
		TokenRow row = tokens.get(tokenHash);
		if (row == null || row.revoked) {
			return false;
		}
		tokens.put(tokenHash, new TokenRow(row.kind, row.riderId, row.familyId, row.expiresAt, true));
		return true;
	}

	@Override
	public void revokeFamily(String familyId) {
		for (Map.Entry<String, TokenRow> entry : new HashMap<String, TokenRow>(tokens).entrySet()) {
			if (entry.getValue().familyId.equals(familyId)) {
				revokeToken(entry.getKey());
			}
		}
	}

	@Override
	public void deleteExpiredTokens(Instant now) {
	}
}
