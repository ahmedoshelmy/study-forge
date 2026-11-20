-- FSRS keeps a memory model per card. 0 means "never scheduled by FSRS" (SM-2 cards, new cards).
alter table card
  add column stability  double precision not null default 0 check (stability >= 0),
  add column difficulty double precision not null default 0 check (difficulty between 0 and 10);
