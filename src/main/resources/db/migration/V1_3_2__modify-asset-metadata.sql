ALTER TABLE asset_metadata ALTER COLUMN description TYPE varchar(65535) USING description::varchar;
ALTER TABLE asset_metadata ALTER COLUMN "name" TYPE varchar(500) USING "name"::varchar;
ALTER TABLE asset_metadata ALTER COLUMN "policy" TYPE varchar(500) USING "policy"::varchar;
ALTER TABLE asset_metadata ALTER COLUMN subject TYPE varchar(500) USING subject::varchar;
ALTER TABLE asset_metadata ALTER COLUMN url TYPE varchar(2048) USING url::varchar;
ALTER TABLE asset_metadata ALTER COLUMN ident DROP NOT NULL;
ALTER TABLE asset_metadata ALTER COLUMN decimals DROP NOT NULL;
ALTER TABLE asset_metadata ALTER COLUMN logo DROP NOT NULL;
ALTER TABLE asset_metadata ALTER COLUMN "policy" TYPE varchar(500) USING "policy"::varchar;
ALTER TABLE asset_metadata ALTER COLUMN "policy" DROP NOT NULL;
ALTER TABLE asset_metadata ALTER COLUMN ticker DROP NOT NULL;
ALTER TABLE asset_metadata ALTER COLUMN url DROP NOT NULL;