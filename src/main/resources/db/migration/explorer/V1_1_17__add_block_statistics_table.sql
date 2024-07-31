CREATE TABLE IF NOT EXISTS block_statistics_daily
(
    id bigserial PRIMARY KEY,
    time DATE NOT NULL,
    epoch_no int NOT NULL,
    no_reporting_nodes int NOT NULL,
    no_unique_peers int NOT NULL,
    no_countries int NOT NULL,
    no_continents int NOT NULL,
    block_prop_mean int NOT NULL,
    block_prop_median int NOT NULL,
    block_prop_p90 int NOT NULL,
    block_prop_p95 int NOT NULL,
    block_adopt_mean int NOT NULL,
    block_adopt_median int NOT NULL,
    block_adopt_p90 int NOT NULL,
    block_adopt_p95 int NOT NULL,
    txs int NOT NULL,
    mean_size_load numeric(4,2) NOT NULL,
    mean_steps_load numeric(4,2) NOT NULL,
    mean_mem_load numeric(4,2) NOT NULL,
    slot_battles int NOT NULL,
    height_battles int NOT NULL,
    cdf3 numeric(5,2) NOT NULL,
    cdf5 numeric(5,2) NOT NULL,
    CONSTRAINT unique_block_statistics_daily UNIQUE (time)
);

CREATE SEQUENCE IF NOT EXISTS block_statistics_daily_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE block_statistics_daily_id_seq OWNED BY block_statistics_daily.id;

-- block_statistics_per_epoch
CREATE TABLE IF NOT EXISTS block_statistics_per_epoch
(
    id bigserial PRIMARY KEY,
    time DATE NOT NULL,
    epoch_no int NOT NULL,
    no_reporting_nodes int NOT NULL,
    no_countries int NOT NULL,
    no_continents int NOT NULL,
    block_prop_mean int NOT NULL,
    block_prop_median int NOT NULL,
    block_prop_p90 int NOT NULL,
    block_prop_p95 int NOT NULL,
    block_adopt_mean int NOT NULL,
    block_adopt_median int NOT NULL,
    block_adopt_p90 int NOT NULL,
    block_adopt_p95 int NOT NULL,
    txs int NOT NULL,
    mean_size_load numeric(4,2) NOT NULL,
    mean_steps_load numeric(4,2) NOT NULL,
    mean_mem_load numeric(4,2) NOT NULL,
    slot_battles int NOT NULL,
    height_battles int NOT NULL,
    cdf3 numeric(5,2) NOT NULL,
    cdf5 numeric(5,2) NOT NULL,
    CONSTRAINT unique_block_statistics_per_epoch UNIQUE (epoch_no)
    );

CREATE SEQUENCE IF NOT EXISTS block_statistics_per_epoch_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE block_statistics_per_epoch_id_seq OWNED BY block_statistics_per_epoch.id;

