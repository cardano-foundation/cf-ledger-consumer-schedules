-- Leverage PSQL distinct on https://www.geekytidbits.com/postgres-distinct-on/
DROP MATERIALIZED VIEW IF EXISTS latest_address_balance;
CREATE MATERIALIZED VIEW IF NOT EXISTS latest_address_balance AS
WITH full_balances AS
    (
    SELECT DISTINCT ON (address, unit) address, unit, slot, quantity
    FROM address_balance
    WHERE unit = 'lovelace'
    ORDER BY address, unit, slot DESC
    )
SELECT * FROM full_balances WHERE quantity > 0;

CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_address_balance_idx ON latest_address_balance (address);
CREATE INDEX IF NOT EXISTS latest_address_balance_unit_idx ON latest_address_balance (unit);
CREATE INDEX IF NOT EXISTS latest_address_balance_slot_idx ON latest_address_balance (slot);
CREATE INDEX IF NOT EXISTS latest_address_balance_quantity_idx ON latest_address_balance (quantity);
