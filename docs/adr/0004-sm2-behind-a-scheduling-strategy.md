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
