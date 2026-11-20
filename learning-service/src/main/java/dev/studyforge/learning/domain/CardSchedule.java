package dev.studyforge.learning.domain;

import java.time.LocalDate;

/**
 * Where a card stands in its review cycle.
 *
 * @param repetitions successful reviews in a row (reset to 0 by a lapse)
 * @param easeFactor SM-2 "E-Factor", never below {@link #MIN_EASE}
 * @param intervalDays days between the last review and {@code dueOn}; 0 for a card never reviewed
 * @param dueOn learner-local date the card should be reviewed next
 */
public record CardSchedule(int repetitions, double easeFactor, int intervalDays, LocalDate dueOn) {

  public static final double MIN_EASE = 1.3;
  public static final double START_EASE = 2.5;

  public CardSchedule {
    if (repetitions < 0 || intervalDays < 0) {
      throw new DomainException("repetitions and interval cannot be negative");
    }
    if (easeFactor < MIN_EASE) {
      throw new DomainException("ease factor cannot drop below " + MIN_EASE);
    }
  }

  /** A new card is due the day it is created. */
  public static CardSchedule fresh(LocalDate today) {
    return new CardSchedule(0, START_EASE, 0, today);
  }

  public boolean dueOn(LocalDate day) {
    return !dueOn.isAfter(day);
  }
}
