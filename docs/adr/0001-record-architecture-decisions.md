# 1. Record architecture decisions

Date: 2026-11-21 · Status: accepted

## Context

Study Forge is small, but it makes structural choices (a message log between services, one database per
service, a pluggable scheduling algorithm) that a reader will want explained. Decisions get lost in commit
messages.

## Decision

Keep lightweight Architecture Decision Records in `docs/adr`, one file per decision, numbered, with context,
decision and consequences. Superseded records stay, marked as such.

## Consequences

New structural choices come with an ADR in the same change.
