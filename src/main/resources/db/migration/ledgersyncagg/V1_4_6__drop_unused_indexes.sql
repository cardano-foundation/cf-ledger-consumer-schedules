DROP INDEX IF EXISTS address_tx_amount_unit_block_time2_idx;
DROP INDEX IF EXISTS address_tx_amount_unit_quantity_block_time_idx;
DROP INDEX IF EXISTS address_tx_amount_unit_tx_hash_block_time_idx;
DROP INDEX IF EXISTS address_tx_amount_address_tx_hash_block_time_idx;
DROP INDEX IF EXISTS address_tx_amount_stake_address_tx_hash_block_time_idx;
DROP INDEX IF EXISTS address_tx_amount_stake_address_unit_block_time_idx;
DROP INDEX IF EXISTS address_tx_amount_slot_idx;

CREATE INDEX IF NOT EXISTS address_tx_amount_block_time_idx
    ON address_tx_amount (block_time);
