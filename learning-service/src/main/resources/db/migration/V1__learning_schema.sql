-- learning-service owns this database (database per service); insights-service never reads it.
create table roadmap (
  id          uuid primary key,
  version     bigint      not null,
  learner_id  text        not null,
  zone_id     text        not null,
  title       text        not null,
  created_at  timestamptz not null
);
create index roadmap_learner_idx on roadmap (learner_id, created_at);

create table stage (
  roadmap_id uuid    not null references roadmap (id) on delete cascade,
  position   integer not null,
  stage_id   uuid    not null,
  title      text    not null,
  primary key (roadmap_id, position)
);

create table roadmap_item (
  roadmap_id        uuid    not null references roadmap (id) on delete cascade,
  position          integer not null,
  item_id           uuid    not null unique,
  stage_id          uuid    not null,
  title             text    not null,
  estimated_minutes integer not null check (estimated_minutes >= 0),
  completed_at      timestamptz,
  primary key (roadmap_id, position)
);

create table card (
  id            uuid primary key,
  version       bigint           not null,
  roadmap_id    uuid             not null references roadmap (id) on delete cascade,
  item_id       uuid,
  learner_id    text             not null,
  zone_id       text             not null,
  front         text             not null,
  back          text             not null,
  repetitions   integer          not null check (repetitions >= 0),
  ease_factor   double precision not null check (ease_factor >= 1.3),
  interval_days integer          not null check (interval_days >= 0),
  due_on        date             not null
);
create index card_due_idx on card (learner_id, due_on);

create table review_log (
  id               uuid primary key,
  version          bigint           not null,
  card_id          uuid             not null references card (id) on delete cascade,
  grade            integer          not null check (grade between 0 and 5),
  duration_seconds integer          not null,
  interval_before  integer          not null,
  interval_after   integer          not null,
  ease_after       double precision not null,
  strategy         text             not null,
  reviewed_at      timestamptz      not null
);
create index review_log_card_idx on review_log (card_id, reviewed_at);
