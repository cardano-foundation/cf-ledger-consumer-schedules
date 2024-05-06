DROP MATERIALIZED VIEW IF EXISTS address_tx_count;

CREATE MATERIALIZED VIEW address_tx_count AS
SELECT ata.address                        AS address,
       count(distinct ata.tx_hash)        AS tx_count
FROM address_tx_amount ata
GROUP BY ata.address;