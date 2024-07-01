drop materialized view if exists latest_token_balance;
create table if not exists latest_token_balance
(
    address       varchar(500) not null,
    stake_address varchar(500),
    policy        varchar(56),
    slot          numeric      not null,
    unit          varchar(255) not null,
    quantity      numeric(38),
    block_time    numeric,
    primary key (address, unit)
);

create index if not exists latest_token_balance_unit_idx
    on latest_token_balance (unit);

create index if not exists latest_token_balance_policy_idx
    on latest_token_balance (policy);

create index if not exists latest_token_balance_slot_idx
    on latest_token_balance (slot);

create index if not exists latest_token_balance_quantity_idx
    on latest_token_balance (quantity);

create index if not exists latest_token_balance_block_time_idx
    on latest_token_balance (block_time);

create index if not exists latest_token_balance_unit_quantity_idx
    on latest_token_balance (unit, quantity);

create index if not exists latest_token_balance_policy_quantity_idx
    on latest_token_balance (policy, quantity);

