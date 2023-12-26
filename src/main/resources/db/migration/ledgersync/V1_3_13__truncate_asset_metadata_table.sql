TRUNCATE TABLE asset_metadata RESTART IDENTITY RESTRICT;
ALTER TABLE asset_metadata ADD COLUMN IF NOT EXISTS fingerprint varchar(255) NULL;
CREATE INDEX IF NOT EXISTS asset_metadata_fingerprint_idx ON asset_metadata (fingerprint);
