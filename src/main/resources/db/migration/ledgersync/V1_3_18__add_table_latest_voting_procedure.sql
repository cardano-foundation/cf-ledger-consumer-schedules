create table if not exists latest_voting_procedure
(
    id                 uuid        not null,
    tx_hash            varchar(64) not null,
    idx                int         not null,
    voter_type         varchar(50),
    voter_hash         varchar(56),
    gov_action_tx_hash varchar(64),
    gov_action_index   int,
    vote               varchar(10),
    anchor_url         varchar,
    anchor_hash        varchar(64),
    epoch              int,
    slot               bigint,
    block              bigint,
    block_time         bigint,
    update_datetime    timestamp,
    repeat_vote        boolean,
    primary key (tx_hash, voter_hash, gov_action_tx_hash, gov_action_index)
);

CREATE INDEX IF NOT EXISTS idx_latest_voting_procedure_slot
    ON latest_voting_procedure (slot);

CREATE INDEX IF NOT EXISTS idx_latest_voting_procedure_txhash
    ON latest_voting_procedure (tx_hash);

CREATE INDEX IF NOT EXISTS idx_latest_voting_procedure_gov_action_tx_hash
    ON latest_voting_procedure (gov_action_tx_hash);

CREATE INDEX IF NOT EXISTS idx_latest_voting_procedure_gov_action_tx_hash_gov_action_index
    ON latest_voting_procedure (gov_action_tx_hash, gov_action_index);
