DROP MATERIALIZED VIEW IF EXISTS latest_stake_address_balance;
CREATE MATERIALIZED VIEW IF NOT EXISTS latest_stake_address_balance AS
WITH full_balances AS
    (
    SELECT DISTINCT ON (address) address, slot, quantity
    FROM stake_address_balance
    ORDER BY address, slot DESC
    )
SELECT * FROM full_balances WHERE quantity > 0;
