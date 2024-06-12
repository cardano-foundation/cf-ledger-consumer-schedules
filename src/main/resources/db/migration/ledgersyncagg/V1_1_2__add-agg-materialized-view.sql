-- aggregate address token
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
CREATE MATERIALIZED VIEW IF NOT EXISTS stake_tx_balance AS
SELECT ata.tx_hash       AS tx_hash,
       ata.stake_address AS stake_address,
       SUM(ata.quantity) AS balance_change,
       ata.block_time    AS time
FROM address_tx_amount ata
WHERE to_timestamp(ata.block_time) > now() - INTERVAL '3' MONTH - INTERVAL '1' DAY
  AND ata.unit = 'lovelace'
GROUP BY ata.tx_hash, ata.block_time, ata.stake_address;
------------------------------------------------------------------------------------------------------------------------
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
CREATE MATERIALIZED VIEW IF NOT EXISTS top_stake_address_balance AS
WITH full_balances AS
         (
             SELECT DISTINCT ON (address) address, slot, quantity
             FROM stake_address_balance
             ORDER BY address, slot DESC
         )
SELECT * FROM full_balances WHERE quantity > 0 order by quantity desc limit 1000;

------------------------------------------------------------------------------------------------------------------------
--- latest token balance
CREATE MATERIALIZED VIEW IF NOT EXISTS latest_token_balance AS
WITH full_balances AS
         (SELECT DISTINCT ON (address, unit) address, unit, slot, quantity, block_time
          FROM address_balance
          WHERE unit <> 'lovelace'
          ORDER BY address, unit, slot DESC)
SELECT ab.address             AS address,
       addr.stake_address     as stake_address,
       substr(ab.unit, 1, 56) AS policy,
       ab.slot                as slot,
       ab.unit                AS unit,
       ab.quantity            AS quantity,
       ab.block_time          AS block_time
FROM full_balances ab
         JOIN address addr ON ab.address = addr.address
WHERE ab.quantity > 0;

------------------------------------------------------------------------------------------------------------------------
CREATE MATERIALIZED VIEW IF NOT EXISTS address_tx_count AS
SELECT ata.address                 AS address,
       count(distinct ata.tx_hash) AS tx_count
FROM address_tx_amount ata
GROUP BY ata.address;

------------------------------------------------------------------------------------------------------------------------
-- materialized view for stake_address_tx_count
CREATE MATERIALIZED VIEW stake_address_tx_count AS
SELECT ata.stake_address           AS stake_address,
       count(distinct ata.tx_hash) AS tx_count
FROM address_tx_amount ata
WHERE ata.stake_address IS NOT NULL
GROUP BY ata.stake_address;
------------------------------------------------------------------------------------------------------------------------
CREATE MATERIALIZED VIEW IF NOT EXISTS stake_address_view AS
SELECT DISTINCT (addr.stake_address) as stake_address FROM address addr
WHERE addr.stake_address IS NOT NULL;