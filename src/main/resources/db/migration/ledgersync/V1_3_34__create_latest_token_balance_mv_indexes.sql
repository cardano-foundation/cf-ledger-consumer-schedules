CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_token_balance_idx ON latest_token_balance (address,unit);
CREATE INDEX IF NOT EXISTS latest_token_balance_unit_idx ON latest_token_balance (unit);
CREATE INDEX IF NOT EXISTS latest_token_balance_policy_idx ON latest_token_balance (policy);
CREATE INDEX IF NOT EXISTS latest_token_balance_slot_idx ON latest_token_balance (slot);
CREATE INDEX IF NOT EXISTS latest_token_balance_quantity_idx ON latest_token_balance (quantity);
CREATE INDEX IF NOT EXISTS latest_token_balance_block_time_idx ON latest_token_balance (block_time);
CREATE INDEX IF NOT EXISTS latest_token_balance_unit_quantity_idx ON latest_token_balance (unit, quantity);
CREATE INDEX IF NOT EXISTS latest_token_balance_policy_quantity_idx ON latest_token_balance (policy, quantity);
