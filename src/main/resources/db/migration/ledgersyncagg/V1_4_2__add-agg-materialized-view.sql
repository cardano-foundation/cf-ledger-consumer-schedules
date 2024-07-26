-- aggregate address token
DROP MATERIALIZED VIEW IF EXISTS agg_address_token;
CREATE MATERIALIZED VIEW IF NOT EXISTS agg_address_token AS
SELECT ata.unit                                        AS unit,
       sum(ata.quantity)                               AS balance,
       date_trunc('day', to_timestamp(ata.block_time)) AS day
FROM address_tx_amount ata
WHERE to_timestamp(ata.block_time) < date_trunc('day', now())
  AND to_timestamp(ata.block_time) > now() - INTERVAL '3' MONTH - INTERVAL '1' DAY
  AND unit != 'lovelace'
  AND ata.quantity > 0
GROUP BY ata.unit, day;

------------------------------------------------------------------------------------------------------------------------
-- aggregate address tx balance
DROP MATERIALIZED VIEW IF EXISTS agg_address_tx_balance;
CREATE MATERIALIZED VIEW IF NOT EXISTS agg_address_tx_balance AS
SELECT ata.address                                     AS address,
       ata.stake_address                               AS stake_address,
       SUM(ata.quantity)                               AS balance,
       date_trunc('day', to_timestamp(ata.block_time)) AS day
FROM address_tx_amount ata
WHERE to_timestamp(ata.block_time) > now() - INTERVAL '3' MONTH - INTERVAL '1' DAY
  AND ata.unit = 'lovelace'
GROUP BY ata.address, ata.stake_address, day
ORDER BY day;
------------------------------------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS stake_tx_balance;
CREATE MATERIALIZED VIEW stake_tx_balance AS
SELECT ata.tx_hash       AS tx_hash,
       ata.stake_address AS stake_address,
       SUM(ata.quantity) AS balance_change,
       ata.slot          AS slot
FROM address_tx_amount ata
WHERE ata.slot > (SELECT ata.slot
                  FROM address_tx_amount ata
                  WHERE ata.block_time >
                        CAST(EXTRACT(epoch from (now() - INTERVAL '3' MONTH - INTERVAL '1' DAY)) AS BIGINT)
                  ORDER BY ata.block_time
                  LIMIT 1)
  AND ata.unit = 'lovelace'
  AND ata.stake_address IS NOT NULL
GROUP BY ata.tx_hash, ata.slot, ata.stake_address;

------------------------------------------------------------------------------------------------------------------------
DROP MATERIALIZED VIEW IF EXISTS latest_address_balance;
CREATE MATERIALIZED VIEW IF NOT EXISTS top_address_balance AS
WITH full_balances AS
         (
             SELECT DISTINCT ON (address) address, slot, quantity
             FROM address_balance
             WHERE unit = 'lovelace'
             ORDER BY address, slot DESC
         )
SELECT * FROM full_balances WHERE quantity > 0 order by quantity desc limit 1000;

------------------------------------------------------------------------------------------------------------------------
DROP MATERIALIZED VIEW IF EXISTS latest_stake_address_balance;
CREATE MATERIALIZED VIEW IF NOT EXISTS top_stake_address_balance AS
WITH full_balances AS
         (
             SELECT DISTINCT ON (address) address, slot, quantity
             FROM stake_address_balance
             ORDER BY address, slot DESC
         )
SELECT * FROM full_balances WHERE quantity > 0 order by quantity desc limit 1000;

------------------------------------------------------------------------------------------------------------------------
CREATE MATERIALIZED VIEW IF NOT EXISTS stake_address_view AS
SELECT DISTINCT (addr.stake_address) as stake_address FROM address addr
WHERE addr.stake_address IS NOT NULL;