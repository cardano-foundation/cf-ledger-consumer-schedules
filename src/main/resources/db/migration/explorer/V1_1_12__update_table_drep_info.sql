ALTER TABLE drep_info ADD COLUMN IF NOT EXISTS voting_power bigint NULL;
ALTER TABLE drep_info ADD COLUMN IF NOT EXISTS updated_at bigint NULL;

CREATE INDEX IF NOT EXISTS drep_info_anchor_url_idx ON drep_info (anchor_url);
CREATE INDEX IF NOT EXISTS drep_info_anchor_hash_idx ON drep_info (anchor_hash);
CREATE INDEX IF NOT EXISTS drep_info_delegators_idx ON drep_info (delegators);
CREATE INDEX IF NOT EXISTS drep_info_active_vote_stake_idx ON drep_info (active_vote_stake);
CREATE INDEX IF NOT EXISTS drep_info_live_stake_idx ON drep_info (live_stake);
CREATE INDEX IF NOT EXISTS drep_info_created_at_idx ON drep_info (created_at);
CREATE INDEX IF NOT EXISTS drep_info_status_idx ON drep_info (status);
CREATE INDEX IF NOT EXISTS drep_info_voting_power_idx ON drep_info (voting_power);
