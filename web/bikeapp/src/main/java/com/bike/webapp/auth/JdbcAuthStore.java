package com.bike.webapp.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuthStore implements AuthStore {

	private final JdbcTemplate jdbc;

	@Autowired
	public JdbcAuthStore(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public String findLinkedRider(String provider, String providerUserId) {
		List<String> rows = jdbc.queryForList(
				"SELECT rider_id FROM trailgo_member_social WHERE provider = ? AND provider_user_id = ?",
				String.class, provider, providerUserId);
		return rows.isEmpty() ? null : rows.get(0);
	}

	@Override
	public boolean link(String provider, String providerUserId, String riderId) {
		try {
			jdbc.update("INSERT INTO trailgo_member_social (provider, provider_user_id, rider_id, last_login_at) "
					+ "VALUES (?, ?, ?, CURRENT_TIMESTAMP)", provider, providerUserId, riderId);
			return true;
		} catch (DuplicateKeyException e) {
			return false;
		}
	}

	@Override
	public void touchLogin(String provider, String providerUserId) {
		jdbc.update("UPDATE trailgo_member_social SET last_login_at = CURRENT_TIMESTAMP "
				+ "WHERE provider = ? AND provider_user_id = ?", provider, providerUserId);
	}

	@Override
	public boolean riderExists(String riderId) {
		Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM rider WHERE r_id = ?", Integer.class, riderId);
		return count != null && count > 0;
	}

	@Override
	public void createRider(String riderId) {
		jdbc.update("INSERT INTO rider (r_id) VALUES (?)", riderId);
	}

	@Override
	public void saveToken(String tokenHash, String kind, String riderId, String familyId, Instant expiresAt) {
		jdbc.update("INSERT INTO trailgo_auth_token (token_hash, kind, rider_id, family_id, expires_at) "
				+ "VALUES (?, ?, ?, ?, ?)", tokenHash, kind, riderId, familyId, Timestamp.from(expiresAt));
	}

	@Override
	public TokenRow findToken(String tokenHash) {
		List<TokenRow> rows = jdbc.query(
				"SELECT kind, rider_id, family_id, expires_at, revoked FROM trailgo_auth_token WHERE token_hash = ?",
				(rs, i) -> new TokenRow(rs.getString("kind"), rs.getString("rider_id"), rs.getString("family_id"),
						rs.getTimestamp("expires_at").toInstant(), rs.getBoolean("revoked")),
				tokenHash);
		return rows.isEmpty() ? null : rows.get(0);
	}

	@Override
	public boolean revokeToken(String tokenHash) {
		return jdbc.update("UPDATE trailgo_auth_token SET revoked = 1 WHERE token_hash = ? AND revoked = 0", tokenHash) > 0;
	}

	@Override
	public void revokeFamily(String familyId) {
		jdbc.update("UPDATE trailgo_auth_token SET revoked = 1 WHERE family_id = ?", familyId);
	}

	@Override
	public void deleteExpiredTokens(Instant now) {
		jdbc.update("DELETE FROM trailgo_auth_token WHERE expires_at < ?", Timestamp.from(now.minusSeconds(86400)));
	}
}
