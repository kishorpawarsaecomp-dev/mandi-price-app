create table mandi_price_history (
    id           bigserial primary key,
    state        varchar(100) not null,
    district     varchar(150) not null,
    market       varchar(200) not null,
    commodity    varchar(200) not null,
    variety      varchar(200) not null default '',
    grade        varchar(100) not null default '',
    arrival_date date         not null,
    min_price    numeric(12,2),
    max_price    numeric(12,2),
    modal_price  numeric(12,2) not null,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now(),
    -- One row per market/commodity/variety/grade per reported day. A re-reported row UPDATES this one.
    constraint uq_mandi_price_natural_key
        unique (state, district, market, commodity, variety, grade, arrival_date)
);
-- commodity + district + date range (history / coverage / market counts)
create index idx_mph_lookup on mandi_price_history (state, district, commodity, arrival_date);
-- single-market series
create index idx_mph_series on mandi_price_history (state, district, commodity, market, arrival_date);
create index idx_mph_date   on mandi_price_history (arrival_date);

create table price_alert (
    id                   bigserial primary key,
    state                varchar(100) not null,
    district             varchar(150) not null,
    commodity            varchar(200) not null,
    market               varchar(200),
    direction            varchar(10)  not null,
    threshold            numeric(12,2) not null,
    active               boolean      not null default true,
    triggered            boolean      not null default false,
    last_observed_price  numeric(12,2),
    last_observed_market varchar(200),
    last_observed_date   date,
    last_checked_at      timestamptz,
    created_at           timestamptz  not null default now()
);
