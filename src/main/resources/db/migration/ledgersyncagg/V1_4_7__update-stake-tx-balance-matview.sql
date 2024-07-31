DROP MATERIALIZED VIEW IF EXISTS stake_tx_balance;
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

CREATE UNIQUE INDEX IF NOT EXISTS unique_stake_tx_balance_idx ON stake_tx_balance (stake_address, tx_hash, slot);
CREATE INDEX IF NOT EXISTS stake_tx_balance_tx_id_idx ON stake_tx_balance (tx_hash);
CREATE INDEX IF NOT EXISTS stake_tx_balance_time_idx ON stake_tx_balance (slot);
CREATE INDEX IF NOT EXISTS stake_tx_balance_stake_address_id_idx ON stake_tx_balance (stake_address);