DROP MATERIALIZED VIEW IF EXISTS address_tx_count;

CREATE MATERIALIZED VIEW address_tx_count AS
SELECT add.address                        AS address,
       count(distinct ata.tx_hash)                        AS tx_count
FROM address add
         left join address_tx_amount ata on ata.address = add.address
GROUP BY add.address;

CREATE INDEX IF NOT EXISTS address_tx_count_address_idx ON address_tx_count (address);
CREATE INDEX IF NOT EXISTS address_tx_count_tx_count_idx ON address_tx_count (tx_count);