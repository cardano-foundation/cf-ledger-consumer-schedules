DROP MATERIALIZED VIEW IF EXISTS token_tx_count;
DROP TABLE IF EXISTS token_tx_count;
CREATE TABLE IF NOT EXISTS token_tx_count
(
    unit     varchar(255) PRIMARY KEY,
    tx_count bigint NOT NULL
);

CREATE INDEX IF NOT EXISTS token_tx_count_unit_tx_count_idx ON token_tx_count (unit, tx_count);