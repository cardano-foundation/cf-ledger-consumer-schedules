CREATE INDEX IF NOT EXISTS address_balance_unit_idx ON address_balance (unit);

DROP MATERIALIZED VIEW IF EXISTS token_tx_count;
CREATE TABLE IF NOT EXISTS token_tx_count
(
    unit     varchar(255) PRIMARY KEY,
    tx_count bigint NOT NULL
);

CREATE INDEX IF NOT EXISTS token_tx_count_unit_tx_count_idx ON token_tx_count (unit, tx_count);

----------------------------------------------------------------------------------------------------
DROP MATERIALIZED VIEW IF EXISTS address_tx_count;

CREATE TABLE IF NOT EXISTS address_tx_count
(
    address  varchar(500) PRIMARY KEY,
    tx_count numeric NOT NULL
);

CREATE INDEX IF NOT EXISTS address_tx_count_tx_count_idx ON address_tx_count (tx_count);

----------------------------------------------------------------------------------------------------
DROP MATERIALIZED VIEW IF EXISTS stake_address_tx_count;

CREATE TABLE IF NOT EXISTS stake_address_tx_count
(
    stake_address varchar(500) PRIMARY KEY,
    tx_count      numeric NOT NULL
);

CREATE INDEX IF NOT EXISTS stake_address_tx_count_tx_count_idx ON stake_address_tx_count (tx_count);

----------------------------------------------------------------------------------------------------
DROP MATERIALIZED VIEW IF EXISTS latest_token_balance;
CREATE TABLE IF NOT EXISTS latest_token_balance
(
    address       varchar(500) not null,
    stake_address varchar(500),
    policy        varchar(56),
    slot          numeric      not null,
    unit          varchar(255) not null,
    quantity      numeric(38),
    block_time    numeric,
    primary key (address, unit)
);

CREATE INDEX IF NOT EXISTS latest_token_balance_unit_idx
    on latest_token_balance (unit);

CREATE INDEX IF NOT EXISTS latest_token_balance_policy_idx
    on latest_token_balance (policy);

CREATE INDEX IF NOT EXISTS latest_token_balance_slot_idx
    on latest_token_balance (slot);

CREATE INDEX IF NOT EXISTS latest_token_balance_quantity_idx
    on latest_token_balance (quantity);

CREATE INDEX IF NOT EXISTS latest_token_balance_block_time_idx
    on latest_token_balance (block_time);

CREATE INDEX IF NOT EXISTS latest_token_balance_unit_quantity_idx
    on latest_token_balance (unit, quantity);

CREATE INDEX IF NOT EXISTS latest_token_balance_policy_quantity_idx
    on latest_token_balance (policy, quantity);


