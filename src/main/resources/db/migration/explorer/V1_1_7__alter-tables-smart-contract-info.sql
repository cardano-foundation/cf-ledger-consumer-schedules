ALTER TABLE smart_contract_info
    ADD is_script_vote  boolean DEFAULT FALSE,
    ADD is_script_propose   boolean DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS smart_contract_info_is_script_vote_idx ON smart_contract_info (is_script_vote);
CREATE INDEX IF NOT EXISTS smart_contract_info_is_script_propose_idx ON smart_contract_info (is_script_propose);