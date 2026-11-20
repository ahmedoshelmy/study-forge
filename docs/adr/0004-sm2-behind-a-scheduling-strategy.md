# 4. SM-2 behind a scheduling strategy (FSRS-ready)

Date: 2026-11-21 · Status: accepted

## Context

Spaced repetition needs a rule that turns "how well did I recall this card" into "when do I see it next".
SM-2 (SuperMemo 2, 1987) is simple, well known and easy to test. FSRS is more accurate but needs fitted
parameters and more state per card.

## Decision

- The domain defines a `SchedulingStrategy` interface: given a card's current schedule, a grade (0–5) and the
  learner-local review date, return the next schedule (interval, ease, repetitions, due time).
- `Sm2Strategy` is the only implementation: grade < 3 resets repetitions and the interval to one day; grade
  ≥ 3 steps 1 day, 6 days, then interval × ease; ease changes by
  `0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)` and never drops below 1.3 (`CardSchedule.MIN_EASE`).
- "Due today" is computed against the learner's own time zone with an injected `Clock`, so the day boundary
  is the learner's midnight, not the server's.
- Rules are pinned by example tests and jqwik properties: for any card and any grade ≥ 3 the interval never
  decreases, and the ease never goes below 1.3.

## Consequences

- An FSRS implementation can be added as a second strategy, chosen per learner or by configuration, without
  touching the REST layer or the events. The schedule fields it needs beyond SM-2 (stability, difficulty)
  would be a new migration.
- SM-2 is known to be harsh after a lapse; that is accepted for the MVP.

## Update (2026-11-21): FSRS added

- `FsrsStrategy` implements the FSRS-4.5 formulas with the published default weights (no per-learner fitting
  yet). Grades map to ratings 0–2 Again, 3 Hard, 4 Good, 5 Easy; the next interval is the day predicted recall
  falls to 90%.
- `CardSchedule` gained `stability` and `difficulty` (0 = never scheduled by FSRS), stored by the additive
  migration `V2__fsrs_memory_state.sql`. REST, events and `ReviewService` did not change.
- The strategy is chosen by `studyforge.scheduler` (`sm2` default, `fsrs`); an unknown value fails startup.
  A card already scheduled by SM-2 joins FSRS with its current interval as its stability, so no progress is
  lost. Switching back to SM-2 keeps the SM-2 fields (ease, repetitions) and drops the FSRS state.
