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
