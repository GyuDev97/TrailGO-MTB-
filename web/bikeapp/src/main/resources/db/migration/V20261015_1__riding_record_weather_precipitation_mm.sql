-- Observed precipitation amount (RN1, mm in the last hour) of the weather snapshot. The sky
-- condition column (rr_weather_sky_code) stays NULL for new rows: the 초단기실황 observation has none.
ALTER TABLE riding_record
    ADD COLUMN IF NOT EXISTS rr_weather_precipitation_mm DECIMAL(5,1) NULL;
