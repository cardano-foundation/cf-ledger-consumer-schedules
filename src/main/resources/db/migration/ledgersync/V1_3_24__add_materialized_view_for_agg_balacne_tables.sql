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
CREATE INDEX IF NOT EXISTS address_tx_amount_block_time_idx ON address_tx_amount (block_time);
CREATE INDEX IF NOT EXISTS address_tx_amount_unit_quantity_block_time_idx ON address_tx_amount (unit, quantity, block_time);
CREATE INDEX IF NOT EXISTS address_tx_amount_unit_tx_hash_idx ON address_tx_amount (unit, tx_hash);
CREATE INDEX IF NOT EXISTS address_tx_amount_unit_block_time_idx ON address_tx_amount (unit, block_time);
CREATE INDEX IF NOT EXISTS address_tx_amount_unit_block_time2_idx ON address_tx_amount (unit, to_timestamp(block_time));
CREATE INDEX IF NOT EXISTS address_tx_amount_unit_tx_hash_block_time_idx ON address_tx_amount (unit, tx_hash, block_time);
CREATE INDEX IF NOT EXISTS address_tx_amount_address_tx_hash_idx ON address_tx_amount (address, tx_hash);
CREATE INDEX IF NOT EXISTS address_tx_amount_stake_address_tx_hash_idx ON address_tx_amount (stake_address, tx_hash);
CREATE INDEX IF NOT EXISTS address_tx_amount_address_tx_hash_block_time_idx ON address_tx_amount (address, tx_hash, block_time);
CREATE INDEX IF NOT EXISTS address_tx_amount_stake_address_tx_hash_block_time_idx ON address_tx_amount (stake_address, tx_hash, block_time);

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

CREATE UNIQUE INDEX IF NOT EXISTS unique_agg_address_token_idx ON agg_address_token (ident, day);

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

CREATE UNIQUE INDEX IF NOT EXISTS unique_agg_address_tx_balance_idx ON agg_address_tx_balance (address_id, stake_address_id, day);

CREATE INDEX IF NOT EXISTS agg_address_tx_balance_day_idx
    ON agg_address_tx_balance (day);

CREATE INDEX IF NOT EXISTS agg_address_tx_balance_stake_address_id_day_balance_idx
    ON agg_address_tx_balance (stake_address_id, day, balance);

CREATE INDEX IF NOT EXISTS agg_address_tx_balance_address_id_day_balance_idx
    ON agg_address_tx_balance (address_id, day, balance);


-- aggregate stake address tx balance
DROP TABLE IF EXISTS stake_tx_balance;

CREATE MATERIALIZED VIEW IF NOT EXISTS stake_tx_balance AS
SELECT sa.id             AS stake_address_id,
       tx.id             AS tx_id,
       SUM(atm.quantity) AS balance_change,
       atm.block_time    AS time
FROM address_tx_amount atm
         INNER JOIN tx on atm.tx_hash = tx.hash
         INNER JOIN stake_address sa ON sa.view = atm.stake_address
WHERE to_timestamp(atm.block_time) > now() - INTERVAL '3' MONTH - INTERVAL '1' DAY
  AND atm.unit = 'lovelace'
GROUP BY tx.id, atm.block_time, sa.id;

CREATE UNIQUE INDEX IF NOT EXISTS unique_stake_tx_balance_idx ON stake_tx_balance (stake_address_id, tx_id, time);
CREATE INDEX IF NOT EXISTS stake_tx_balance_tx_id_idx ON stake_tx_balance (tx_id);
CREATE INDEX IF NOT EXISTS stake_tx_balance_time_idx ON stake_tx_balance (time);
CREATE INDEX IF NOT EXISTS stake_tx_balance_stake_address_id_idx ON stake_tx_balance (stake_address_id);


--- latest token balance
CREATE MATERIALIZED VIEW IF NOT EXISTS latest_token_balance AS
SELECT ab.address         AS address,
       addr.stake_address as stake_address,
       ab.slot            as slot,
       ab.unit            AS unit,
       ab.quantity        AS quantity,
       ab.block_time      AS block_time
FROM address_balance ab
         JOIN address addr on ab.address = addr.address
WHERE ab.quantity >= 0
  AND ab.unit != 'lovelace'
  AND NOT exists(SELECT 1
                 FROM address_balance ab2
                 WHERE ab2.address = ab.address
                   AND ab2.slot > ab.slot
                   AND ab2.quantity >= 0
                   AND ab2.unit = ab.unit);

CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_token_balance_idx ON latest_token_balance (address,unit);
CREATE INDEX IF NOT EXISTS latest_token_balance_unit_idx ON latest_token_balance (unit);
CREATE INDEX IF NOT EXISTS latest_token_balance_slot_idx ON latest_token_balance (slot);
CREATE INDEX IF NOT EXISTS latest_token_balance_quantity_idx ON latest_token_balance (quantity);
CREATE INDEX IF NOT EXISTS latest_token_balance_block_time_idx ON latest_token_balance (block_time);
CREATE INDEX IF NOT EXISTS latest_token_balance_unit_quantity_idx ON latest_token_balance (unit, quantity);

-- latest address balance
CREATE MATERIALIZED VIEW IF NOT EXISTS latest_address_balance AS
SELECT ab.address AS address, ab.slot as slot, ab.unit AS unit, ab.quantity as quantity
from address_balance ab
where ab.unit = 'lovelace'
  and not exists(select 1 from address_balance ab2 where ab2.address = ab.address and ab2.slot > ab.slot and ab2.unit = 'lovelace');

CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_address_balance_idx ON latest_address_balance (address);
CREATE INDEX IF NOT EXISTS latest_address_balance_unit_idx ON latest_address_balance (unit);
CREATE INDEX IF NOT EXISTS latest_address_balance_slot_idx ON latest_address_balance (slot);
CREATE INDEX IF NOT EXISTS latest_address_balance_quantity_idx ON latest_address_balance (quantity);

-- latest stake address balance
CREATE MATERIALIZED VIEW IF NOT EXISTS latest_stake_address_balance AS
SELECT sab.address AS address, sab.slot as slot, sab.quantity as quantity
from stake_address_balance sab
where not exists(select 1 from stake_address_balance sab2 where sab2.address = sab.address and sab2.slot > sab.slot);

CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_stake_address_balance_idx ON latest_stake_address_balance (address);
CREATE INDEX IF NOT EXISTS latest_stake_address_balance_slot_idx ON latest_stake_address_balance (slot);
CREATE INDEX IF NOT EXISTS latest_stake_address_balance_quantity_idx ON latest_stake_address_balance (quantity);