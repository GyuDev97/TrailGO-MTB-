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
