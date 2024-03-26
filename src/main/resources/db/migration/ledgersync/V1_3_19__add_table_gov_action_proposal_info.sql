create table if not exists gov_action_proposal_info
(
    tx_hash       varchar(64) not null,
    idx           int         not null,
    expired_epoch int,
    status        varchar(30),
    voting_power  bigint,
    primary key (tx_hash, idx)
);

CREATE INDEX IF NOT EXISTS idx_gov_action_proposal_info_tx_hash_idx
    ON gov_action_proposal_info (tx_hash, idx);

CREATE INDEX IF NOT EXISTS idx_gov_action_proposal_info_expired_epoch
    ON gov_action_proposal_info (expired_epoch);

CREATE INDEX IF NOT EXISTS idx_gov_action_proposal_info_status
    ON gov_action_proposal_info (status);