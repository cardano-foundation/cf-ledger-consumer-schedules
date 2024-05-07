DROP MATERIALIZED VIEW IF EXISTS token_tx_count;

CREATE MATERIALIZED VIEW token_tx_count as
SELECT ma.id as ident, count(distinct (ata.tx_hash)) as tx_count
FROM address_tx_amount ata
         JOIN multi_asset ma ON ata.unit = ma.unit
GROUP BY ma.id;

CREATE INDEX token_tx_count_ident_idx ON token_tx_count(ident);
CREATE INDEX token_tx_count_tx_count_idx ON token_tx_count(tx_count);