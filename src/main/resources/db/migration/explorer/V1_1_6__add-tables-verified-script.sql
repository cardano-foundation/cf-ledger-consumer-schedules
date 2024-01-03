CREATE TABLE IF NOT EXISTS verified_script
(
    id     bigserial   NOT NULL,
    hash   varchar(64) NOT NULL,
    "json" text        NULL,
    CONSTRAINT verified_script_pkey PRIMARY KEY (id),
    CONSTRAINT unique_verified_script UNIQUE (hash)
);

CREATE SEQUENCE IF NOT EXISTS verified_script_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE verified_script_id_seq OWNED BY verified_script.id;

CREATE UNIQUE INDEX verified_script_hash_uindex ON verified_script USING btree (hash);