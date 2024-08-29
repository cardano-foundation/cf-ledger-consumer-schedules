DROP TABLE IF EXISTS token_tx_count CASCADE;


CREATE TABLE IF NOT EXISTS token_tx_count
(
    unit                                        varchar(256) NOT NULL,
    tx_count                                    bigint NULL,
    updated_slot                                bigint NOT NULL,
    previous_tx_count                           bigint NULL,
    previous_slot                               bigint NULL,
    is_calculated_in_incremental_mode           boolean NULL,
    CONSTRAINT token_tx_count_pkey PRIMARY KEY (unit)
    );