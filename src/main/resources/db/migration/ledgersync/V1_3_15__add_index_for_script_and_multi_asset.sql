CREATE INDEX IF NOT EXISTS multi_asset_policy_id_idx ON multi_asset USING btree (policy, id);
CREATE INDEX IF NOT EXISTS multi_asset_policy_idx ON multi_asset USING btree (policy);
CREATE INDEX IF NOT EXISTS script_type_idx ON script USING btree (type);