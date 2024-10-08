CREATE INDEX IF NOT EXISTS address_tx_amount_stake_address_idx
    ON address_tx_amount (stake_address);
CREATE INDEX IF NOT EXISTS address_tx_amount_epoch_idx
    ON address_tx_amount (epoch);
CREATE INDEX IF NOT EXISTS address_tx_amount_block_time_idx
    ON address_tx_amount (block_time);

CREATE INDEX IF NOT EXISTS address_balance_slot_quantity_unit_idx ON address_balance (slot, quantity, unit);
CREATE INDEX IF NOT EXISTS address_balance_unit_idx ON address_balance (unit);
CREATE INDEX IF NOT EXISTS address_balance_address_slot_lovelace_idx ON address_balance (address, slot) WHERE unit = 'lovelace';

CREATE INDEX IF NOT EXISTS address_address_hash_idx ON address using hash (address);
CREATE INDEX IF NOT EXISTS address_payment_credential_hash_idx ON address using hash (payment_credential);
CREATE INDEX IF NOT EXISTS address_stake_address_hash_idx ON address using hash (stake_address);
CREATE INDEX IF NOT EXISTS address_stake_credential_hash_idx ON address using hash (stake_credential);