ALTER TABLE report_history
    ADD COLUMN zone_offset integer DEFAULT NULL;

ALTER TABLE report_history
    ADD COLUMN time_pattern varchar(255) DEFAULT NULL;

ALTER TABLE report_history
    ADD COLUMN date_format varchar(255) DEFAULT NULL;