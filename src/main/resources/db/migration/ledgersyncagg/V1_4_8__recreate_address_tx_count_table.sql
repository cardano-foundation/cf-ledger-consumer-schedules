DROP TABLE IF EXISTS address_tx_count CASCADE;


CREATE TABLE IF NOT EXISTS address_tx_count
(
    address                                        varchar(256) NOT NULL,
    tx_count                                    bigint NULL,
    updated_slot                                bigint NOT NULL,
    previous_tx_count                           bigint NULL,
    previous_slot                               bigint NULL,
    is_calculated_in_incremental_mode           boolean NULL,
    CONSTRAINT address_tx_count_pkey PRIMARY KEY (address)
);

