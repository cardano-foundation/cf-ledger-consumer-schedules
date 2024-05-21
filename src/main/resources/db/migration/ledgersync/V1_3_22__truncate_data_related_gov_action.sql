TRUNCATE TABLE gov_action_proposal_info RESTART IDENTITY RESTRICT;
TRUNCATE TABLE latest_voting_procedure RESTART IDENTITY RESTRICT;
TRUNCATE TABLE agg_pool_info RESTART IDENTITY RESTRICT;

DROP INDEX IF EXISTS agg_pool_info_governance_participation_rate_idx;
CREATE INDEX IF NOT EXISTS agg_pool_info_governance_participation_rate_index ON agg_pool_info (governance_participation_rate);
