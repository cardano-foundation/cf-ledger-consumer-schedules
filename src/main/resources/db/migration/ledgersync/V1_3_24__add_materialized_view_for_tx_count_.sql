DROP MATERIALIZED VIEW IF EXISTS tx_count;

CREATE MATERIALIZED VIEW tx_count AS
SELECT add.address                        AS address,
       count(1)                        AS count,
       timezone('utc', now())  AS lastUpdated
FROM address add
         left join address_tx_amount ata on ata.address = add.address
GROUP BY add.address;