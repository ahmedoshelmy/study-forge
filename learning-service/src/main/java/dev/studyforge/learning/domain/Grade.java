package dev.studyforge.learning.domain;

/**
 * How well the learner recalled a card, on the SM-2 scale: 0 blackout, 1 wrong but familiar, 2
 * wrong but easy once seen, 3 right with effort, 4 right after hesitation, 5 perfect.
 */
public record Grade(int value) {

  public static final int MIN = 0;
  public static final int MAX = 5;
  public static final int PASS = 3;

  public Grade {
    if (value < MIN || value > MAX) {
      throw new DomainException("grade must be between 0 and 5, got " + value);
    }
  }

  public static Grade of(int value) {
    return new Grade(value);
  }

  /** A grade of 3 or more counts as recalled; lower grades reset the card. */
  public boolean recalled() {
    return value >= PASS;
  }
}
