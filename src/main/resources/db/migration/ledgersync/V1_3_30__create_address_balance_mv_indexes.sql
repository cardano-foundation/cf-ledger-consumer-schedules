CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_address_balance_idx ON latest_address_balance (address);
CREATE INDEX IF NOT EXISTS latest_address_balance_unit_idx ON latest_address_balance (unit);
CREATE INDEX IF NOT EXISTS latest_address_balance_slot_idx ON latest_address_balance (slot);
CREATE INDEX IF NOT EXISTS latest_address_balance_quantity_idx ON latest_address_balance (quantity);

