# 3. Two services, database per service

Date: 2026-11-21 · Status: accepted

## Context

The other demo apps in this family are modular monoliths, which is the right default for a product this
size. Study Forge is deliberately the small-microservices example: it shows what changes when the write side
and the read side are separate deployables.

## Decision

- **learning-service** (port 8080) owns roadmaps, stages, items, cards, the review log and scheduling. It is
  the only writer of learning data and publishes `ReviewRecorded` and `ItemCompleted`.
- **insights-service** (port 8081) owns read models only, built from those events, and serves them over
  REST plus a Server-Sent Events stream of live stats.
- Each service has its **own database** (`learning`, `insights`) with its own Flyway migrations. Locally they
  share one Postgres instance to save memory; nothing else is shared. Neither service reads the other's tables.
- The event records live in a small `contracts` module with a JSON Schema per event, checked by a contract test,
  and the consumer is a tolerant reader (unknown fields ignored) so the producer can add fields.
- One Maven reactor, one Dockerfile parameterised by `SERVICE`, one compose file.

## Consequences

- The insights side is eventually consistent: after a review, stats update within milliseconds normally, and
  the web dashboard listens on SSE rather than polling. The smoke test polls with a bound.
- Schema changes to events must be additive, or versioned with a new type.
- Two JVMs cost more memory than one; each runs with `-XX:MaxRAMPercentage=60` under a 512 MB limit.
