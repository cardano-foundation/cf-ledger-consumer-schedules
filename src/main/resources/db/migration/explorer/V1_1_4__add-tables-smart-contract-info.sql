CREATE TABLE IF NOT EXISTS smart_contract_info
(
    id               bigserial             NOT NULL,
    script_hash      character varying(64) NOT NULL,
    type             character varying(20) NOT NULL,
    tx_count         int8,
    is_script_reward boolean DEFAULT FALSE,
    is_script_cert   boolean DEFAULT FALSE,
    is_script_spend  boolean DEFAULT FALSE,
    is_script_mint   boolean DEFAULT FALSE,
    CONSTRAINT unique_smart_contract_info UNIQUE (script_hash)
);

CREATE SEQUENCE IF NOT EXISTS smart_contract_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE smart_contract_info_id_seq OWNED BY smart_contract_info.id;

CREATE INDEX IF NOT EXISTS smart_contract_info_tx_count_idx ON smart_contract_info (tx_count);
CREATE INDEX IF NOT EXISTS smart_contract_info_type_idx ON smart_contract_info ("type");
CREATE INDEX IF NOT EXISTS smart_contract_info_is_script_reward_idx ON smart_contract_info (is_script_reward);
CREATE INDEX IF NOT EXISTS smart_contract_info_is_script_cert_idx ON smart_contract_info (is_script_cert);
CREATE INDEX IF NOT EXISTS smart_contract_info_is_script_spend_idx ON smart_contract_info (is_script_spend);
CREATE INDEX IF NOT EXISTS smart_contract_info_is_script_mint_idx ON smart_contract_info (is_script_mint);