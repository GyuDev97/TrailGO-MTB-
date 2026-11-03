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
