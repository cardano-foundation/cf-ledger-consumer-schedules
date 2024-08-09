CREATE TABLE IF NOT EXISTS off_chain_vote_gov_action_data (
    anchor_url      varchar(255) NOT NULL,
    anchor_hash     text  NOT NULL,
    raw_data        text,
    title           text,
    abstract       text,
    motivation     text,
    rationale      text,
    primary key (anchor_hash, anchor_url)
    );

CREATE TABLE IF NOT EXISTS off_chain_vote_fetch_error (
    anchor_url      varchar(255) NOT NULL,
    anchor_hash     varchar(64)  NOT NULL,
    fetch_error     varchar(255),
    fetch_time timestamp,
    retry_count int8,
    primary key (anchor_hash, anchor_url)
    );