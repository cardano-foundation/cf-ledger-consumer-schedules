CREATE TABLE IF NOT EXISTS block_statistics_daily
(
    id bigserial PRIMARY KEY,
    time DATE NOT NULL,
    epoch_no int NOT NULL,
    no_reporting_nodes int,
    no_unique_peers int,
    no_countries int,
    no_continents int,
    block_prop_mean int,
    block_prop_median int,
    block_prop_p90 int,
    block_prop_p95 int,
    block_adopt_mean int,
    block_adopt_median int,
    block_adopt_p90 int,
    block_adopt_p95 int,
    txs int,
    mean_size_load numeric(4,2),
    mean_steps_load numeric(4,2),
    mean_mem_load numeric(4,2),
    slot_battles int,
    height_battles int,
    cdf3 numeric(5,2),
    cdf5 numeric(5,2),
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
    no_reporting_nodes int,
    no_countries int,
    no_continents int,
    block_prop_mean int,
    block_prop_median int,
    block_prop_p90 int,
    block_prop_p95 int,
    block_adopt_mean int,
    block_adopt_median int,
    block_adopt_p90 int,
    block_adopt_p95 int,
    txs int,
    mean_size_load numeric(4,2),
    mean_steps_load numeric(4,2),
    mean_mem_load numeric(4,2),
    slot_battles int,
    height_battles int,
    cdf3 numeric(5,2),
    cdf5 numeric(5,2),
    CONSTRAINT unique_block_statistics_per_epoch UNIQUE (epoch_no)
    );

CREATE SEQUENCE IF NOT EXISTS block_statistics_per_epoch_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE block_statistics_per_epoch_id_seq OWNED BY block_statistics_per_epoch.id;

