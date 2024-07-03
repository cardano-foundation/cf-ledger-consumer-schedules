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
