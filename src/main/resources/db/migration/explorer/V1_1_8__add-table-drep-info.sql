CREATE TABLE IF NOT EXISTS data_checkpoint
(
    id          bigserial NOT NULL,
    block_no    int8      NULL,
    slot_no     int8      NULL,
    update_time timestamp NULL,
    type        varchar(20),
    CONSTRAINT data_checkpoint_pkey PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS data_checkpoint_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE data_checkpoint_id_seq OWNED BY data_checkpoint.id;

CREATE TABLE IF NOT EXISTS drep_info
(
    id                bigserial NOT NULL,
    drep_hash         varchar(56),
    drep_id           varchar(255),
    anchor_url        varchar,
    anchor_hash       varchar(64),
    delegators        integer,
    active_vote_stake bigint,
    live_stake        bigint,
    created_at        bigint,
    status            varchar(10),
    CONSTRAINT drep_info_pkey PRIMARY KEY (id),
    CONSTRAINT unique_drep_info UNIQUE (drep_hash),
    CONSTRAINT unique_drep_id UNIQUE (drep_id)
);

CREATE SEQUENCE IF NOT EXISTS drep_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE drep_info_id_seq OWNED BY drep_info.id;
