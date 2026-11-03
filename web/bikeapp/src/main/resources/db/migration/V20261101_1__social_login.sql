-- Social login (Kakao / Naver). A rider is identified by the provider's user id, never by anything
-- the app claims. rider_id is the existing rider.r_id the rest of the app already uses.
CREATE TABLE IF NOT EXISTS trailgo_member_social (
    provider VARCHAR(16) NOT NULL,
    provider_user_id VARCHAR(128) NOT NULL,
    rider_id VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME NULL,
    PRIMARY KEY (provider, provider_user_id),
    INDEX idx_member_social_rider (rider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Opaque session tokens. Only the SHA-256 of a token is stored, so a database leak does not leak
-- usable sessions. A refresh token is single-use: using it revokes the whole family.
CREATE TABLE IF NOT EXISTS trailgo_auth_token (
    token_hash CHAR(64) NOT NULL,
    kind VARCHAR(8) NOT NULL,
    rider_id VARCHAR(64) NOT NULL,
    family_id CHAR(32) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (token_hash),
    INDEX idx_auth_token_family (family_id),
    INDEX idx_auth_token_rider (rider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
