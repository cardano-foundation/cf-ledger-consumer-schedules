CREATE INDEX IF NOT EXISTS idx_gov_action_proposal_composite ON gov_action_proposal (tx_hash,idx,"type",slot);
CREATE INDEX IF NOT EXISTS idx_gov_action_proposal_epoch ON gov_action_proposal (epoch);
CREATE INDEX IF NOT EXISTS idx_gov_action_proposal_anchor_text ON gov_action_proposal (anchor_url, anchor_hash);
CREATE INDEX IF NOT EXISTS idx_gov_action_proposal_block_time ON gov_action_proposal (block_time);
CREATE INDEX IF NOT EXISTS hash_idx_gov_action_proposal_tx_hash on gov_action_proposal using hash(tx_hash);
CREATE INDEX IF NOT EXISTS hash_idx_gov_action_proposal_tx_idx on gov_action_proposal using hash(idx);

CREATE INDEX IF NOT EXISTS idx_committee_registration_epoch ON committee_registration (epoch);
CREATE INDEX IF NOT EXISTS idx_committee_registration_block_time ON committee_registration (block_time);
CREATE INDEX IF NOT EXISTS idx_committee_registration_tx_hash_cert_index ON committee_registration (tx_hash, cert_index);
CREATE INDEX IF NOT EXISTS idx_committee_registration_cold_key_hot_key ON committee_registration (cold_key, hot_key);
CREATE INDEX IF NOT EXISTS idx_committee_registration_cred_type ON committee_registration (cred_type);

CREATE INDEX IF NOT EXISTS hash_idx_committee_deregistration_tx_hash ON committee_deregistration using hash (tx_hash);
CREATE INDEX IF NOT EXISTS hash_idx_committee_deregistration_tx_cert_index ON committee_deregistration using hash (cert_index);
CREATE INDEX IF NOT EXISTS idx_committee_deregistration_anchor_text ON committee_deregistration (anchor_url, anchor_hash);
CREATE INDEX IF NOT EXISTS idx_committee_deregistration_cold_key ON committee_deregistration using hash (cold_key);
CREATE INDEX IF NOT EXISTS idx_committee_deregistration_cred_type ON committee_deregistration using hash (cred_type);
CREATE INDEX IF NOT EXISTS idx_committee_deregistration_slot ON committee_deregistration (slot);
CREATE INDEX IF NOT EXISTS idx_committee_deregistration_block_time ON committee_deregistration (block_time);

CREATE INDEX IF NOT EXISTS idx_voting_procedure_voter_type ON voting_procedure (voter_type);
CREATE INDEX IF NOT EXISTS idx_voting_procedure_voter_type_voter_hash ON voting_procedure (voter_type,voter_hash);
CREATE INDEX IF NOT EXISTS idx_voting_procedure_vote ON voting_procedure (vote);
CREATE INDEX IF NOT EXISTS idx_voting_procedure_block_time ON voting_procedure (block_time);

CREATE INDEX IF NOT EXISTS idx_latest_voting_procedure_voter_type ON latest_voting_procedure (voter_type);
CREATE INDEX IF NOT EXISTS idx_latest_voting_procedure_voter_type_voter_hash ON latest_voting_procedure (voter_type,voter_hash);
CREATE INDEX IF NOT EXISTS idx_latest_voting_procedure_vote ON latest_voting_procedure (vote);
CREATE INDEX IF NOT EXISTS idx_latest_voting_procedure_voter_hash_block_time ON latest_voting_procedure (voter_hash,block_time);
CREATE INDEX IF NOT EXISTS idx_latest_voting_procedure_voter_hash_gov_action_tx_hash_index ON latest_voting_procedure (gov_action_tx_hash,gov_action_index);