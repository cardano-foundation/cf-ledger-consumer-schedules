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
CREATE UNIQUE INDEX IF NOT EXISTS unique_stake_tx_balance_idx ON stake_tx_balance (stake_address, tx_hash, slot);
CREATE INDEX IF NOT EXISTS stake_tx_balance_tx_id_idx ON stake_tx_balance (tx_hash);
CREATE INDEX IF NOT EXISTS stake_tx_balance_time_idx ON stake_tx_balance (slot);
CREATE INDEX IF NOT EXISTS stake_tx_balance_stake_address_id_idx ON stake_tx_balance (stake_address);

------------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS unique_stake_address_idx ON stake_address_view (stake_address);
------------------------------------------------------------------------------------------------------------------------