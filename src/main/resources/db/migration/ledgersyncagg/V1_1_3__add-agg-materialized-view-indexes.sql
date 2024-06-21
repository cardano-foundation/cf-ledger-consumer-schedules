CREATE UNIQUE INDEX IF NOT EXISTS unique_agg_address_token_idx ON agg_address_token (unit, day);
CREATE INDEX IF NOT EXISTS agg_address_token_day_idx
    ON agg_address_token (day);
CREATE INDEX IF NOT EXISTS agg_address_token_ident_day_balance_idx
    ON agg_address_token (unit, day, balance);

------------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS unique_agg_address_tx_balance_idx ON agg_address_tx_balance (address, stake_address, day);
CREATE INDEX IF NOT EXISTS agg_address_tx_balance_day_idx
    ON agg_address_tx_balance (day);
CREATE INDEX IF NOT EXISTS agg_address_tx_balance_stake_address_id_day_balance_idx
    ON agg_address_tx_balance (stake_address, day, balance);
CREATE INDEX IF NOT EXISTS agg_address_tx_balance_address_id_day_balance_idx
    ON agg_address_tx_balance (address, day, balance);

------------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS unique_stake_tx_balance_idx ON stake_tx_balance (stake_address, tx_hash, time);
CREATE INDEX IF NOT EXISTS stake_tx_balance_tx_id_idx ON stake_tx_balance (tx_hash);
CREATE INDEX IF NOT EXISTS stake_tx_balance_time_idx ON stake_tx_balance (time);
CREATE INDEX IF NOT EXISTS stake_tx_balance_stake_address_id_idx ON stake_tx_balance (stake_address);

------------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_token_balance_idx ON latest_token_balance (address, unit);
CREATE INDEX IF NOT EXISTS latest_token_balance_unit_idx ON latest_token_balance (unit);
CREATE INDEX IF NOT EXISTS latest_token_balance_policy_idx ON latest_token_balance (policy);
CREATE INDEX IF NOT EXISTS latest_token_balance_slot_idx ON latest_token_balance (slot);
CREATE INDEX IF NOT EXISTS latest_token_balance_quantity_idx ON latest_token_balance (quantity);
CREATE INDEX IF NOT EXISTS latest_token_balance_block_time_idx ON latest_token_balance (block_time);
CREATE INDEX IF NOT EXISTS latest_token_balance_unit_quantity_idx ON latest_token_balance (unit, quantity);
CREATE INDEX IF NOT EXISTS latest_token_balance_policy_quantity_idx ON latest_token_balance (policy, quantity);

------------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS unique_stake_address_idx ON stake_address_view (stake_address);
------------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_address_balance_idx ON top_address_balance (address);
CREATE INDEX IF NOT EXISTS latest_address_balance_quantity_idx ON top_address_balance (quantity);
------------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_stake_address_balance_idx ON top_stake_address_balance (address);
CREATE INDEX IF NOT EXISTS latest_stake_address_balance_quantity_idx ON top_stake_address_balance (quantity);