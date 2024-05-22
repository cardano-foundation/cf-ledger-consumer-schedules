DROP INDEX IF EXISTS address_tx_amount_unit_idx;
DROP INDEX IF EXISTS address_tx_amount_quantity_idx;
DROP INDEX IF EXISTS address_tx_amount_address_idx;
DROP INDEX IF EXISTS address_tx_amount_tx_hash_idx;
DROP INDEX IF EXISTS address_tx_amount_unit_quantity_idx;
DROP INDEX IF EXISTS address_tx_amount_block_time_idx;
DROP INDEX IF EXISTS address_tx_amount_unit_tx_hash_idx;
DROP INDEX IF EXISTS address_tx_amount_unit_block_time_idx;
DROP INDEX IF EXISTS address_tx_amount_address_tx_hash_idx;
DROP INDEX IF EXISTS address_tx_amount_stake_address_tx_hash_idx;

CREATE INDEX IF NOT EXISTS address_tx_amount_stake_address_unit_block_time_idx
    ON address_tx_amount (stake_address, unit, block_time);

CREATE INDEX IF NOT EXISTS address_tx_amount_slot_idx
    ON address_tx_amount (slot);