CREATE MATERIALIZED VIEW IF NOT EXISTS committee_info as
select cr.hot_key as hot_key, min(cr.block_time) as created_at from committee_registration cr
where not exists (select 1 from committee_deregistration cd where cd.cold_key = cr.cold_key and cd.block_time >
(select max(cr2.block_time) from committee_registration cr2 where cr2.hot_key = cr.hot_key))
group by cr.hot_key;

CREATE INDEX IF NOT EXISTS idx_committee_info_hot_key on committee_info USING hash (hot_key);
CREATE INDEX IF NOT EXISTS idx_committee_info_created_at on committee_info (created_at);