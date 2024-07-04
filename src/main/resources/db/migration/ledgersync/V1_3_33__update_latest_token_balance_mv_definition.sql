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
