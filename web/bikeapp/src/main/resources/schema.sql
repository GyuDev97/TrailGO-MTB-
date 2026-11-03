CREATE TABLE IF NOT EXISTS trailgo_hazard_review (
    hazard_id INT NOT NULL,
    title VARCHAR(120) NULL,
    hazard_type VARCHAR(32) NOT NULL DEFAULT 'OTHER',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    approved_at DATETIME NULL,
    approved_by VARCHAR(100) NULL,
    rejection_reason VARCHAR(500) NULL,
    confirm_count INT NOT NULL DEFAULT 0,
    report_count INT NOT NULL DEFAULT 0,
    expires_at DATETIME NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    merged_into INT NULL,
    PRIMARY KEY (hazard_id),
    INDEX idx_trailgo_hazard_status (status),
    INDEX idx_trailgo_hazard_type (hazard_type),
    INDEX idx_trailgo_hazard_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trailgo_hazard_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hazard_id INT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_trailgo_hazard_image (hazard_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trailgo_hazard_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hazard_id INT NOT NULL,
    action VARCHAR(32) NOT NULL,
    previous_status VARCHAR(20) NULL,
    next_status VARCHAR(20) NULL,
    admin_id VARCHAR(100) NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_trailgo_hazard_history (hazard_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO trailgo_hazard_review
    (hazard_id, title, hazard_type, status, created_at, updated_at, confirm_count, report_count, is_active)
SELECT idx,
       LEFT(COALESCE(da_content, '위험지역 제보'), 120),
       'OTHER',
       CASE da_status
           WHEN '1' THEN 'APPROVED'
           WHEN '2' THEN 'REJECTED'
           WHEN '3' THEN 'REVIEWING'
           WHEN '4' THEN 'INACTIVE'
           ELSE 'PENDING'
       END,
       COALESCE(create_date, CURRENT_TIMESTAMP),
       CURRENT_TIMESTAMP,
       0,
       0,
       CASE WHEN da_status = '4' THEN 0 ELSE 1 END
FROM dangerous_area;

-- V20261013_1: riding_record weather snapshot (same as db/migration/V20261013_1__riding_record_weather_snapshot.sql)
-- Weather snapshot of a ride, captured once when the record is saved (never refreshed on read).
-- All columns are nullable: existing rides, rides without coordinates, a missing KMA key and
-- external API failures simply leave them NULL.
-- sky/precipitation hold the normalised app values (CLEAR, PARTLY_CLOUDY, CLOUDY, UNKNOWN /
-- NONE, RAIN, RAIN_SNOW, SNOW, SHOWER), never raw KMA codes. rr_weather_observed_at is Asia/Seoul
-- wall-clock time. Do not put weather into rr_high (accumulated climb) or rr_area.
-- IF NOT EXISTS (MariaDB) keeps the statement safe to run on every start-up via schema.sql.
ALTER TABLE riding_record
    ADD COLUMN IF NOT EXISTS rr_weather_observed_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS rr_weather_nx INT NULL,
    ADD COLUMN IF NOT EXISTS rr_weather_ny INT NULL,
    ADD COLUMN IF NOT EXISTS rr_weather_temperature_c DECIMAL(4,1) NULL,
    ADD COLUMN IF NOT EXISTS rr_weather_sky_code VARCHAR(16) NULL,
    ADD COLUMN IF NOT EXISTS rr_weather_precipitation_code VARCHAR(16) NULL,
    ADD COLUMN IF NOT EXISTS rr_weather_label VARCHAR(40) NULL;

-- V20261014_1: riding_record memo + photos (same as db/migration/V20261014_1__riding_record_memo_photos.sql)
-- Rider memo and photos for a riding record. Purely additive: existing rides keep working with
-- NULL memo and no photos. Photo bytes live on disk under <storage-root>/photos, the table only
-- holds an unguessable token and the file name.
ALTER TABLE riding_record
    ADD COLUMN IF NOT EXISTS rr_memo VARCHAR(500) NULL;

CREATE TABLE IF NOT EXISTS riding_record_photo (
    photo_id VARCHAR(32) NOT NULL,
    rr_idx VARCHAR(32) NOT NULL,
    file_name VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (photo_id),
    INDEX idx_riding_record_photo_record (rr_idx, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V20261015_1: weather precipitation amount (same as db/migration/V20261015_1__riding_record_weather_precipitation_mm.sql)
-- Observed precipitation amount (RN1, mm in the last hour) of the weather snapshot. The sky
-- condition column (rr_weather_sky_code) stays NULL for new rows: the 초단기실황 observation has none.
ALTER TABLE riding_record
    ADD COLUMN IF NOT EXISTS rr_weather_precipitation_mm DECIMAL(5,1) NULL;

-- V20261101_1: social login (same as db/migration/V20261101_1__social_login.sql)
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

-- V20261102_1: hazard zones (same as db/migration/V20261102_1__hazard_geo.sql)
-- Hazard zones (user reports with a radius). Purely additive: existing dangerous_area rows keep working.
-- da_radius_m: radius of the circular hazard zone in metres; legacy rows get the 30 m default.
-- da_lat / da_lon: numeric copies of the legacy VARCHAR da_latitude / da_longitude, computed by the
-- database (NULL when the text is not a plain decimal), so legacy inserts need no change and the
-- bounding-box search of GET /app/hazards/nearby can use an index. Type, title and moderation status
-- live in trailgo_hazard_review (hazard_id = dangerous_area.idx), which the admin screens already use.
-- IF NOT EXISTS (MariaDB) keeps the statements safe to run on every start-up via schema.sql.
ALTER TABLE dangerous_area
    ADD COLUMN IF NOT EXISTS da_radius_m INT NOT NULL DEFAULT 30,
    ADD COLUMN IF NOT EXISTS da_lat DECIMAL(10,7) AS (CASE WHEN da_latitude REGEXP '^-?[0-9]{1,3}([.][0-9]{1,12})?$' THEN CAST(da_latitude AS DECIMAL(10,7)) END) PERSISTENT,
    ADD COLUMN IF NOT EXISTS da_lon DECIMAL(10,7) AS (CASE WHEN da_longitude REGEXP '^-?[0-9]{1,3}([.][0-9]{1,12})?$' THEN CAST(da_longitude AS DECIMAL(10,7)) END) PERSISTENT;

CREATE INDEX IF NOT EXISTS idx_dangerous_area_geo ON dangerous_area (da_lat, da_lon);

-- V20261102_2: admin hazard management (same as db/migration/V20261102_2__hazard_admin.sql)
-- Admin hazard management: who created a zone, and the merge of duplicate zones. Purely additive.
-- source: USER = reported by a rider (default, also for every existing row), ADMIN = registered by an operator.
-- created_by: the operator for source ADMIN.
-- trailgo_hazard_review.merged_into / status MERGED already say "this zone was folded into that one";
-- trailgo_hazard_merge keeps the audit trail of each merge. Nothing is ever deleted: the merged zone's
-- dangerous_area row, reporter and history stay, and rider apps simply no longer receive it.
-- IF NOT EXISTS (MariaDB) keeps the statements safe to run on every start-up via schema.sql.
ALTER TABLE trailgo_hazard_review
    ADD COLUMN IF NOT EXISTS source VARCHAR(10) NOT NULL DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(100) NULL;

CREATE INDEX IF NOT EXISTS idx_trailgo_hazard_merged_into ON trailgo_hazard_review (merged_into);

-- One row per merged (source) zone. merge_group ties together the rows of one merge operation.
-- uk_hazard_merge_merged: a zone can be merged only once, even if two operators click at the same time.
CREATE TABLE IF NOT EXISTS trailgo_hazard_merge (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merge_group CHAR(32) NOT NULL,
    canonical_id INT NOT NULL,
    merged_id INT NOT NULL,
    admin_id VARCHAR(100) NOT NULL,
    merged_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    merged_before TEXT NULL,
    canonical_before TEXT NULL,
    canonical_after TEXT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_hazard_merge_merged (merged_id),
    INDEX idx_hazard_merge_canonical (canonical_id),
    INDEX idx_hazard_merge_group (merge_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
