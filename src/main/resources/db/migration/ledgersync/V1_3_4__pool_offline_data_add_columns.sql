ALTER TABLE pool_offline_data
ADD COLUMN IF NOT EXISTS logo_url varchar(2000) NULL;

ALTER TABLE pool_offline_data
ADD COLUMN IF NOT EXISTS icon_url varchar(2000) NULL;