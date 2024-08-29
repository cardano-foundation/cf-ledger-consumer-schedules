TRUNCATE TABLE data_checkpoint CASCADE;

ALTER TABLE data_checkpoint
    ADD CONSTRAINT unique_data_checkpoint_type UNIQUE (type);