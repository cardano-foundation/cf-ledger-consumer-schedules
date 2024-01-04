CREATE TABLE IF NOT EXISTS native_script_info
(
    id                      bigserial             NOT NULL,
    script_hash             character varying(64) NOT NULL,
    type                    character varying(20) NOT NULL,
    number_of_tokens        int8 default 0,
    number_of_asset_holders int8 default 0,
    before_slot             int8,
    after_slot              int8,
    number_sig              int8 default 0,
    CONSTRAINT unique_native_script_info UNIQUE (script_hash)
);

CREATE SEQUENCE IF NOT EXISTS native_script_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE native_script_info_id_seq OWNED BY native_script_info.id;

CREATE INDEX IF NOT EXISTS native_script_info_number_of_tokens_idx ON native_script_info (number_of_tokens);
CREATE INDEX IF NOT EXISTS native_script_info_number_of_asset_holders_idx ON native_script_info (number_of_asset_holders);
CREATE INDEX IF NOT EXISTS native_script_info_before_slot_idx ON native_script_info (before_slot);
CREATE INDEX IF NOT EXISTS native_script_info_after_slot_idx ON native_script_info (after_slot);
CREATE INDEX IF NOT EXISTS native_script_info_number_sig_idx ON native_script_info (number_sig);