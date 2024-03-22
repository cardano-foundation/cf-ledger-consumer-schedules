ALTER TABLE agg_pool_info
    ADD voting_power bigint,
    ADD governance_participation_rate bigint;

ALTER TABLE drep_info
    ADD type varchar(20);

CREATE INDEX IF NOT EXISTS drep_info_type_idx ON drep_info (type);
