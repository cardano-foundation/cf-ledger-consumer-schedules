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
    updated_at bigint NULL,
    voting_power float NULL,
    gov_participation_rate float NULL ,
    type varchar(30),
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

CREATE INDEX IF NOT EXISTS drep_info_anchor_url_idx ON drep_info (anchor_url);
CREATE INDEX IF NOT EXISTS drep_info_anchor_hash_idx ON drep_info (anchor_hash);
CREATE INDEX IF NOT EXISTS drep_info_delegators_idx ON drep_info (delegators);
CREATE INDEX IF NOT EXISTS drep_info_active_vote_stake_idx ON drep_info (active_vote_stake);
CREATE INDEX IF NOT EXISTS drep_info_live_stake_idx ON drep_info (live_stake);
CREATE INDEX IF NOT EXISTS drep_info_created_at_idx ON drep_info (created_at);
CREATE INDEX IF NOT EXISTS drep_info_status_idx ON drep_info (status);
CREATE INDEX IF NOT EXISTS drep_info_voting_power_idx ON drep_info (voting_power);
CREATE INDEX IF NOT EXISTS drep_info_gov_participation_rate_idx ON drep_info (gov_participation_rate);
CREATE INDEX IF NOT EXISTS drep_info_type_idx ON drep_info (type);