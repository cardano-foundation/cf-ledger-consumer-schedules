-- address_tx_count
DROP MATERIALIZED VIEW IF EXISTS address_tx_count;

CREATE MATERIALIZED VIEW address_tx_count AS
SELECT add.address                        AS address,
       count(distinct ata.tx_hash)                        AS tx_count
FROM address add
         left join address_tx_amount ata on ata.address = add.address
GROUP BY add.address;

CREATE UNIQUE INDEX IF NOT EXISTS unique_address_tx_count_idx ON address_tx_count (address);
CREATE INDEX IF NOT EXISTS address_tx_count_tx_count_idx ON address_tx_count (tx_count);

-- token_tx_count
DROP MATERIALIZED VIEW IF EXISTS token_tx_count;

CREATE MATERIALIZED VIEW token_tx_count as
SELECT ma.id as ident, count(distinct (ata.tx_hash)) as tx_count
FROM address_tx_amount ata
         JOIN multi_asset ma ON ata.unit = ma.unit
GROUP BY ma.id;

CREATE UNIQUE INDEX IF NOT EXISTS unique_token_tx_count__idx ON token_tx_count(ident);
CREATE INDEX IF NOT EXISTS token_tx_count_tx_count_idx ON token_tx_count(tx_count);

-- add index for address entitys

CREATE INDEX IF NOT EXISTS address_address_hash_idx ON address using hash (address);
CREATE INDEX IF NOT EXISTS address_payment_credential_hash_idx ON address using hash (payment_credential);
CREATE INDEX IF NOT EXISTS address_stake_address_hash_idx ON address using hash (stake_address);
CREATE INDEX IF NOT EXISTS address_stake_credential_hash_idx ON address using hash (stake_credential);


-- materialized view for stake_address_tx_count
DROP MATERIALIZED VIEW IF EXISTS stake_address_tx_count;

CREATE MATERIALIZED VIEW stake_address_tx_count AS
SELECT ata.stake_address           AS stake_address,
       count(distinct ata.tx_hash) AS tx_count
FROM address_tx_amount ata
GROUP BY ata.stake_address;

CREATE UNIQUE INDEX IF NOT EXISTS unique_stake_address_tx_count_idx ON stake_address_tx_count (stake_address);
CREATE INDEX IF NOT EXISTS stake_address_tx_count_tx_count_idx ON stake_address_tx_count (tx_count);