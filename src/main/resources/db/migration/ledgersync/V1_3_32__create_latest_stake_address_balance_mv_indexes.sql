CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_stake_address_balance_idx ON latest_stake_address_balance (address);
CREATE INDEX IF NOT EXISTS latest_stake_address_balance_slot_idx ON latest_stake_address_balance (slot);
CREATE INDEX IF NOT EXISTS latest_stake_address_balance_quantity_idx ON latest_stake_address_balance (quantity);
