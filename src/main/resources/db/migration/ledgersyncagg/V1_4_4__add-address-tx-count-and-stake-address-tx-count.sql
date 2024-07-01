DROP MATERIALIZED VIEW IF EXISTS address_tx_count;

CREATE TABLE IF NOT EXISTS address_tx_count
(
    address     varchar(500) PRIMARY KEY,
    tx_count numeric NOT NULL
);

CREATE INDEX IF NOT EXISTS address_tx_count_tx_count_idx ON address_tx_count (tx_count);

DROP MATERIALIZED VIEW IF EXISTS stake_address_tx_count;

CREATE TABLE IF NOT EXISTS stake_address_tx_count
(
    stake_address     varchar(500) PRIMARY KEY,
    tx_count numeric NOT NULL
);

CREATE INDEX IF NOT EXISTS stake_address_tx_count_tx_count_idx ON stake_address_tx_count (tx_count);