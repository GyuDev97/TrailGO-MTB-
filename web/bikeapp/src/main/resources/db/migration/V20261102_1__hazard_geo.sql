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
