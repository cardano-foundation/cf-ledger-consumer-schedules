CREATE INDEX multi_asset_policy_id_idx ON multi_asset USING btree (policy, id);
CREATE INDEX multi_asset_tx_count_idx ON multi_asset USING btree (tx_count);
CREATE INDEX multi_asset_policy_tx_count_idx ON multi_asset USING btree (policy, tx_count);
CREATE INDEX multi_asset_policy_idx ON multi_asset USING btree (policy);
CREATE INDEX address_token_balance_ident_balance_idx ON address_token_balance USING btree (ident, balance);
CREATE INDEX script_type_idx ON script USING btree (type);