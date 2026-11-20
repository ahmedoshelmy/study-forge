# 2. Kafka (Redpanda) for learning events, not RabbitMQ

Date: 2026-11-21 · Status: accepted

## Context

`learning-service` records reviews and completed roadmap items; `insights-service` turns them into streaks,
retention curves, weekly minutes and a due-card forecast. The sibling .NET demo apps use RabbitMQ through
MassTransit, which is a good fit for commands and work queues: a message is delivered, acknowledged and gone.

Insights are different. They are *projections of history*: a new read model (say, "average ease per stage")
should be buildable from every review ever recorded, and the per-learner numbers only make sense if one
learner's events are applied in the order they happened.

## Decision

Use Kafka semantics, run locally and in CI as a single Redpanda container (Kafka API, no ZooKeeper/KRaft
setup, 256 MB in dev mode).

- Topics `review-events` and `progress-events`, 3 partitions, **keyed by learnerId**: all events of one
  learner land on one partition and are consumed in order, while different learners spread across consumers.
- The producer is idempotent (`acks=all`, `enable.idempotence=true`), fed by Spring Modulith's event
  publication registry (a transactional outbox), so an event is written only if the review was committed and
  is re-sent after a crash.
- `insights-service` consumes as group `insights` with Spring Kafka `@RetryableTopic`: transient failures
  go to delayed retry topics (exponential back-off) without blocking the partition; unreadable messages skip
  the retries; both end on the `-dlt` topic and are parked in a `dead_letter` table, visible at
  `GET /api/insights/dead-letters`.
- Delivery is at-least-once, so the projector is idempotent: a `processed_event` table keyed by event id
  turns a redelivered event into a no-op inside the same transaction as the read-model update.

## Consequences

- Replay is possible: reset the consumer group offset (or start a new group) to rebuild read models.
- Retry topics trade strict per-key ordering for availability on failure: a retried event can be applied
  after a later one for the same learner. The read models here are commutative counters and sets of days, so
  this is acceptable; a future order-sensitive projection would need blocking retries instead.
- One more piece of infrastructure than a modular monolith would need. That is the point of this demo
  (see ADR 0003), and Redpanda keeps the footprint small.
