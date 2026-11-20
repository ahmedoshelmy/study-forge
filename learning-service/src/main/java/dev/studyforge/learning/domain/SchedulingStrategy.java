package dev.studyforge.learning.domain;

import java.time.LocalDate;

/**
 * Picks the next review date for a card. SM-2 is the shipped implementation; FSRS (which models
 * stability and difficulty per card) can be dropped in behind the same port without touching the
 * review flow or the events.
 */
public interface SchedulingStrategy {

  /** Short name stored with every review log row, so a later strategy switch stays auditable. */
  String name();

  CardSchedule next(CardSchedule current, Grade grade, LocalDate reviewedOn);
}
