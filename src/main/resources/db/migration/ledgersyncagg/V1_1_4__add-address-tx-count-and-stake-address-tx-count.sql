-- DROP MATERIALIZED VIEW IF EXISTS address_tx_count;
-- DROP MATERIALIZED VIEW IF EXISTS stake_address_tx_count;
--
-- DROP INDEX IF EXISTS unique_address_tx_count_idx;
-- DROP INDEX IF EXISTS address_tx_count_tx_count_idx;
-- DROP INDEX IF EXISTS unique_stake_address_tx_count_idx;
-- DROP INDEX IF EXISTS stake_address_tx_count_tx_count_idx;

CREATE TABLE IF NOT EXISTS address_tx_count
(
    address     varchar(500) PRIMARY KEY,
    tx_count bigint NOT NULL
);