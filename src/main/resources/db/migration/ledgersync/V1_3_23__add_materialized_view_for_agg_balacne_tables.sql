-- index on existing tables
CREATE INDEX IF NOT EXISTS address_balance_address_idx ON address_balance (address);
CREATE INDEX IF NOT EXISTS address_balance_unit_idx ON address_balance (unit);
CREATE INDEX IF NOT EXISTS address_balance_quantity_idx ON address_balance (quantity);
CREATE INDEX IF NOT EXISTS address_balance_address_unit_idx ON address_balance (address, unit);
CREATE INDEX IF NOT EXISTS address_balance_address_slot_idx ON address_balance (address, slot);

CREATE INDEX IF NOT EXISTS address_tx_amount_unit_idx ON address_tx_amount (unit);
CREATE INDEX IF NOT EXISTS address_tx_amount_quantity_idx ON address_tx_amount (quantity);
CREATE INDEX IF NOT EXISTS address_tx_amount_address_idx ON address_tx_amount (address);
CREATE INDEX IF NOT EXISTS address_tx_amount_tx_hash_idx ON address_tx_amount (tx_hash);
CREATE INDEX IF NOT EXISTS address_tx_amount_stake_address_idx ON address_tx_amount (stake_address);
CREATE INDEX IF NOT EXISTS address_tx_amount_unit_quantity_idx ON address_tx_amount (unit, quantity);
CREATE INDEX IF NOT EXISTS address_tx_amount_epoch_idx ON address_tx_amount (epoch);

CREATE INDEX IF NOT EXISTS multi_asset_unit_idx ON multi_asset (unit);

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


--- latest token balance
CREATE MATERIALIZED VIEW IF NOT EXISTS latest_token_balance AS
SELECT ab.address AS address, ab.slot as slot, ab.unit AS unit, ab.quantity as quantity
from address_balance ab
         JOIN (SELECT ab2.address AS address, ab2.unit AS unit, max(ab2.slot) AS slot
               FROM address_balance ab2
               where ab2.unit != 'lovelace'
               GROUP BY ab2.address, ab2.unit) as tmp
              ON tmp.address = ab.address AND tmp.slot = ab.slot AND tmp.unit = ab.unit
WHERE ab.quantity > 0
  and ab.unit != 'lovelace';

CREATE INDEX IF NOT EXISTS latest_token_balance_address_idx ON latest_token_balance (address);
CREATE INDEX IF NOT EXISTS latest_token_balance_unit_idx ON latest_token_balance (unit);
CREATE INDEX IF NOT EXISTS latest_token_balance_slot_idx ON latest_token_balance (slot);
CREATE INDEX IF NOT EXISTS latest_token_balance_quantity_idx ON latest_token_balance (quantity);