DROP INDEX IF EXISTS address_tx_amount_unit_block_time2_idx;
DROP INDEX IF EXISTS address_tx_amount_unit_quantity_block_time_idx;
DROP INDEX IF EXISTS address_tx_amount_unit_tx_hash_block_time_idx;
DROP INDEX IF EXISTS address_tx_amount_address_tx_hash_block_time_idx;
DROP INDEX IF EXISTS address_tx_amount_stake_address_tx_hash_block_time_idx;
DROP INDEX IF EXISTS address_tx_amount_stake_address_unit_block_time_idx;
DROP INDEX IF EXISTS address_tx_amount_slot_idx;

CREATE INDEX IF NOT EXISTS address_tx_amount_block_time_idx
    ON address_tx_amount (block_time);

CREATE INDEX IF NOT EXISTS address_tx_amount_address_tx_hash_slot_index
    ON address_tx_amount (address, tx_hash, slot);

CREATE INDEX IF NOT EXISTS address_tx_amount_stake_address_tx_hash_slot_index
    ON address_tx_amount (stake_address, tx_hash, slot);

CREATE INDEX IF NOT EXISTS address_tx_amount_unit_tx_hash_slot_index
    ON address_tx_amount (unit, tx_hash, slot);
