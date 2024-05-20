DROP MATERIALIZED VIEW IF EXISTS latest_token_balance;
CREATE MATERIALIZED VIEW IF NOT EXISTS latest_token_balance AS
SELECT ab.address         AS address,
       addr.stake_address as stake_address,
       ma.policy          AS policy,
       ab.slot            as slot,
       ab.unit            AS unit,
       ab.quantity        AS quantity,
       ab.block_time      AS block_time
FROM address_balance ab
         JOIN address addr on ab.address = addr.address
         JOIN multi_asset ma on ab.unit = ma.unit
WHERE ab.quantity >= 0
  AND NOT exists(SELECT 1
                 FROM address_balance ab2
                 WHERE ab2.address = ab.address
                   AND ab2.slot > ab.slot
                   AND ab2.quantity >= 0
                   AND ab2.unit = ab.unit);

CREATE UNIQUE INDEX IF NOT EXISTS unique_latest_token_balance_idx ON latest_token_balance (address,unit);
CREATE INDEX IF NOT EXISTS latest_token_balance_unit_idx ON latest_token_balance (unit);
CREATE INDEX IF NOT EXISTS latest_token_balance_policy_idx ON latest_token_balance (policy);
CREATE INDEX IF NOT EXISTS latest_token_balance_slot_idx ON latest_token_balance (slot);
CREATE INDEX IF NOT EXISTS latest_token_balance_quantity_idx ON latest_token_balance (quantity);
CREATE INDEX IF NOT EXISTS latest_token_balance_block_time_idx ON latest_token_balance (block_time);
CREATE INDEX IF NOT EXISTS latest_token_balance_unit_quantity_idx ON latest_token_balance (unit, quantity);
CREATE INDEX IF NOT EXISTS latest_token_balance_policy_quantity_idx ON latest_token_balance (policy, quantity);