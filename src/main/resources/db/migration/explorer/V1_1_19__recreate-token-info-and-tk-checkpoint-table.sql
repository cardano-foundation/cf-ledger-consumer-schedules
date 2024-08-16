DROP TABLE IF EXISTS token_info, token_info_checkpoint CASCADE;

CREATE TABLE IF NOT EXISTS token_info
(
    unit                        varchar(256) NOT NULL,
    number_of_holders           int8 NULL,
    volume_24h                  numeric(40) NULL,
    total_volume                numeric(40) NULL,
    tx_count int8               NULL,
    updated_slot bigint         NOT NULL,
    previous_number_of_holders  int8 NULL,
    previous_volume_24h         numeric(40) NULL,
    previous_total_volume       numeric(40) NULL,
    previous_tx_count           int8 NULL,
    previous_slot               bigint NULL,
    CONSTRAINT token_info_pkey PRIMARY KEY (unit)
    );


CREATE TABLE IF NOT EXISTS token_info_checkpoint
(
    id              bigserial NOT NULL,
    slot            bigint NULL,
    update_time     timestamp NULL,
    CONSTRAINT block_token_info_checkpoint_pkey PRIMARY KEY (id)
    );

CREATE SEQUENCE IF NOT EXISTS token_info_checkpoint_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE token_info_checkpoint_id_seq OWNED BY token_info_checkpoint.id;