
DROP TABLE IF EXISTS stake_address_tx_count CASCADE;


CREATE TABLE IF NOT EXISTS stake_address_tx_count
(
    stake_address                               varchar(256) NOT NULL,
    tx_count                                    bigint NULL,
    updated_slot                                bigint NOT NULL,
    previous_tx_count                           bigint NULL,
    previous_slot                               bigint NULL,
    is_calculated_in_incremental_mode           boolean NULL,
    CONSTRAINT stake_address_tx_count_pkey PRIMARY KEY (stake_address)
    );


