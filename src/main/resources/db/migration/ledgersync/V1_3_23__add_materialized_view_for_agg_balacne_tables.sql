-- aggregate address token

CREATE MATERIALIZED VIEW IF NOT EXISTS agg_address_token AS
SELECT ma.id                                           AS ident,
       sum(atm.quantity)                               AS balance,
       date_trunc('day', to_timestamp(atm.block_time)) AS day
FROM address_tx_amount atm
         INNER JOIN multi_asset ma on atm.unit = ma.unit
WHERE to_timestamp(atm.block_time) < date_trunc('day', now())
  AND atm.quantity > 0
  AND to_timestamp(atm.block_time) > now() - INTERVAL '3' MONTH - INTERVAL '1' DAY
GROUP BY ma.id, day
ORDER BY day;

CREATE INDEX IF NOT EXISTS agg_address_token_day_idx
    ON agg_address_token (day);

CREATE INDEX IF NOT EXISTS agg_address_token_ident_day_balance_idx
    ON agg_address_token (ident, day, balance);

-- aggregate address tx balance
CREATE MATERIALIZED VIEW IF NOT EXISTS agg_address_tx_balance AS
SELECT sa.id                                           AS stake_address_id,
       addr.id                                         AS address_id,
       SUM(atm.quantity)                               AS balance,
       date_trunc('day', to_timestamp(atm.block_time)) AS day
FROM address_tx_amount atm
         INNER JOIN address addr ON atm.address = addr.address
         LEFT JOIN stake_address sa ON sa.view = atm.stake_address
WHERE to_timestamp(atm.block_time) > now() - INTERVAL '3' MONTH - INTERVAL '1' DAY
  AND atm.unit = 'lovelace'
GROUP BY addr.id, sa.id, day
ORDER BY day;

CREATE INDEX IF NOT EXISTS agg_address_tx_balance_day_idx
    ON agg_address_tx_balance (day);

CREATE INDEX IF NOT EXISTS agg_address_tx_balance_stake_address_id_day_balance_idx
    ON agg_address_tx_balance (stake_address_id, day, balance);

CREATE INDEX IF NOT EXISTS agg_address_tx_balance_address_id_day_balance_idx
    ON agg_address_tx_balance (address_id, day, balance);


-- aggregate stake address tx balance
DROP MATERIALIZED VIEW IF EXISTS stake_tx_balance;
DROP TABLE IF EXISTS stake_tx_balance;

CREATE MATERIALIZED VIEW IF NOT EXISTS stake_tx_balance AS
SELECT sa.id                        AS stake_address_id,
       tx.id                        AS tx_id,
       SUM(atm.quantity)            AS balance_change,
       to_timestamp(atm.block_time) AS time
FROM address_tx_amount atm
         INNER JOIN tx on atm.tx_hash = tx.hash
         INNER JOIN stake_address sa ON sa.view = atm.stake_address
WHERE to_timestamp(atm.block_time) > now() - INTERVAL '3' MONTH - INTERVAL '1' DAY
  AND atm.unit = 'lovelace'
GROUP BY tx.id, atm.block_time, sa.id;

CREATE INDEX IF NOT EXISTS stake_tx_balance_tx_id_idx ON stake_tx_balance (tx_id);
CREATE INDEX IF NOT EXISTS stake_tx_balance_time_idx ON stake_tx_balance (time);
CREATE INDEX IF NOT EXISTS stake_tx_balance_stake_address_id_idx ON stake_tx_balance (stake_address_id);