ALTER TABLE gov_action_proposal_info
    ADD index_type BIGINT, ADD type varchar(50), ADD block_time bigint, ADD spoAllowedVote boolean;


CREATE INDEX IF NOT EXISTS idx_gov_action_proposal_info_type ON gov_action_proposal_info using hash (type);
CREATE INDEX IF NOT EXISTS idx_gov_action_proposal_info_spo_allowed_vote ON gov_action_proposal_info (spo_allowed_vote);
CREATE INDEX IF NOT EXISTS idx_gov_action_proposal_info_block_time ON gov_action_proposal_info (block_time);