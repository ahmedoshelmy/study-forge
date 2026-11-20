-- insights-service owns this database. Every table is a read model rebuilt from Kafka (replay from
-- offset 0 after truncating them all) — nothing here is a source of truth.
create table processed_event (
  event_id     uuid primary key,
  event_type   text        not null,
  processed_at timestamptz not null default now()
);

create table learner (
  learner_id text primary key,
  zone_id    text not null
);

create table daily_activity (
  learner_id      text    not null,
  day             date    not null,
  reviews         integer not null default 0,
  recalled        integer not null default 0,
  seconds         integer not null default 0,
  items_completed integer not null default 0,
  minutes         integer not null default 0,
  primary key (learner_id, day)
);

create table retention (
  learner_id text    not null,
  bucket     text    not null,
  reviews    integer not null default 0,
  recalled   integer not null default 0,
  primary key (learner_id, bucket)
);

create table card_due (
  learner_id text not null,
  card_id    uuid not null,
  due_on     date not null,
  primary key (learner_id, card_id)
);
create index card_due_day_idx on card_due (learner_id, due_on);

create table dead_letter (
  id          bigserial primary key,
  topic       text        not null,
  record_key  text,
  payload     text,
  error       text,
  received_at timestamptz not null default now()
);
