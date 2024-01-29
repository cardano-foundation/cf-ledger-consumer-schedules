--
-- Name: asset_metadata; Type: TABLE;
--

CREATE TABLE IF NOT EXISTS asset_metadata
(
    id          bigint          NOT NULL,
    decimals    integer         NOT NULL,
    description varchar(255)    NOT NULL,
    logo        varchar(100000) NOT NULL,
    name        varchar(255)    NOT NULL,
    policy      varchar(255)    NOT NULL,
    subject     varchar(255)    NOT NULL,
    ticker      varchar(255)    NOT NULL,
    url         varchar(255)    NOT NULL,
    ident       bigint          NOT NULL
);

--
-- Name: asset_metadata_id_seq; Type: SEQUENCE;
--

CREATE SEQUENCE IF NOT EXISTS asset_metadata_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE CACHE 1;

ALTER SEQUENCE asset_metadata_id_seq OWNED BY asset_metadata.id;

--
-- Name: asset_metadata id; Type: DEFAULT;
--

ALTER TABLE ONLY asset_metadata
    ALTER COLUMN id SET DEFAULT nextval('asset_metadata_id_seq'::regclass);

--
-- Name: asset_metadata asset_metadata_pkey; Type: CONSTRAINT;
--

ALTER TABLE ONLY asset_metadata
    DROP CONSTRAINT IF EXISTS asset_metadata_pkey;

ALTER TABLE ONLY asset_metadata
    ADD CONSTRAINT asset_metadata_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX IF NOT EXISTS asset_metadata_ux ON asset_metadata (ident);

ALTER TABLE asset_metadata ALTER COLUMN description TYPE varchar(65535) USING description::varchar;
ALTER TABLE asset_metadata ALTER COLUMN "name" TYPE varchar(500) USING "name"::varchar;
ALTER TABLE asset_metadata ALTER COLUMN "policy" TYPE varchar(500) USING "policy"::varchar;
ALTER TABLE asset_metadata ALTER COLUMN subject TYPE varchar(500) USING subject::varchar;
ALTER TABLE asset_metadata ALTER COLUMN url TYPE varchar(2048) USING url::varchar;
ALTER TABLE asset_metadata ALTER COLUMN ident DROP NOT NULL;
ALTER TABLE asset_metadata ALTER COLUMN decimals DROP NOT NULL;
ALTER TABLE asset_metadata ALTER COLUMN logo DROP NOT NULL;
ALTER TABLE asset_metadata ALTER COLUMN "policy" TYPE varchar(500) USING "policy"::varchar;
ALTER TABLE asset_metadata ALTER COLUMN "policy" DROP NOT NULL;
ALTER TABLE asset_metadata ALTER COLUMN ticker DROP NOT NULL;
ALTER TABLE asset_metadata ALTER COLUMN url DROP NOT NULL;