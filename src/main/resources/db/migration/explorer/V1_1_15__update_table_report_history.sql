ALTER TABLE report_history
    ADD COLUMN IF NOT EXISTS zone_offset integer DEFAULT NULL;

ALTER TABLE report_history
    ADD COLUMN IF NOT EXISTS time_pattern varchar(255) DEFAULT NULL;

ALTER TABLE report_history
    ADD COLUMN IF NOT EXISTS date_format varchar(255) DEFAULT NULL;