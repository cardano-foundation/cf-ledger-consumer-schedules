ALTER TABLE token_info ADD COLUMN IF NOT EXISTS tx_count int8 NULL;
ALTER TABLE token_info ADD COLUMN IF NOT EXISTS total_volume numeric(40) NULL;

CREATE INDEX IF NOT EXISTS token_info_tx_count_idx ON token_info(tx_count);
CREATE INDEX IF NOT EXISTS token_info_total_volume_idx ON token_info(total_volume);