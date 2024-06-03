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


DROP MATERIALIZED VIEW IF EXISTS latest_stake_address_balance;
CREATE MATERIALIZED VIEW IF NOT EXISTS latest_stake_address_balance AS
WITH full_balances AS
    (
    SELECT DISTINCT ON (address) address, slot, quantity
    FROM stake_address_balance
    ORDER BY address, slot DESC
    )
SELECT * FROM full_balances WHERE quantity > 0;

CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_stake_address_balance_idx ON latest_stake_address_balance (address);
CREATE INDEX IF NOT EXISTS latest_stake_address_balance_slot_idx ON latest_stake_address_balance (slot);
CREATE INDEX IF NOT EXISTS latest_stake_address_balance_quantity_idx ON latest_stake_address_balance (quantity);


DROP MATERIALIZED VIEW IF EXISTS latest_token_balance;
CREATE MATERIALIZED VIEW IF NOT EXISTS latest_token_balance AS
WITH full_balances AS
    (
    SELECT DISTINCT ON (address, unit) address, unit, slot, quantity, block_time
    FROM address_balance
    WHERE unit <> 'lovelace'
    ORDER BY address, unit, slot DESC
    )
SELECT ab.address         AS address,
       addr.stake_address as stake_address,
       ma.policy          AS policy,
       ab.slot            as slot,
       ab.unit            AS unit,
       ab.quantity        AS quantity,
       ab.block_time      AS block_time
FROM full_balances ab
 JOIN address addr ON ab.address = addr.address
 JOIN multi_asset ma ON ab.unit = ma.unit
WHERE ab.quantity > 0;

CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_token_balance_idx ON latest_token_balance (address,unit);
CREATE INDEX IF NOT EXISTS latest_token_balance_unit_idx ON latest_token_balance (unit);
CREATE INDEX IF NOT EXISTS latest_token_balance_policy_idx ON latest_token_balance (policy);
CREATE INDEX IF NOT EXISTS latest_token_balance_slot_idx ON latest_token_balance (slot);
CREATE INDEX IF NOT EXISTS latest_token_balance_quantity_idx ON latest_token_balance (quantity);
CREATE INDEX IF NOT EXISTS latest_token_balance_block_time_idx ON latest_token_balance (block_time);
CREATE INDEX IF NOT EXISTS latest_token_balance_unit_quantity_idx ON latest_token_balance (unit, quantity);
CREATE INDEX IF NOT EXISTS latest_token_balance_policy_quantity_idx ON latest_token_balance (policy, quantity);
