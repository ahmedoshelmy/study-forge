package dev.studyforge.learning.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * FSRS (Free Spaced Repetition Scheduler), following the FSRS-4.5 formulas with their published
 * default weights. Unlike SM-2, each card carries a memory model:
 *
 * <ul>
 *   <li><b>Stability</b> S: days until the probability of recall falls to 90%.
 *   <li><b>Difficulty</b> D: 1–10, nudged by every grade and pulled back towards a default.
 *   <li><b>Retrievability</b> R: the predicted recall today, from the days since the last review
 *       and S. Reviewing a card when R is low (overdue) and still recalling it grows S the most.
 * </ul>
 *
 * <p>Grades map to FSRS ratings: 0–2 Again, 3 Hard, 4 Good, 5 Easy. The next interval is the day
 * recall is predicted to fall to {@link #DESIRED_RETENTION}. A card first reviewed by SM-2 joins
 * FSRS with its current interval as stability, so switching strategies keeps its progress.
 */
public final class FsrsStrategy implements SchedulingStrategy {

  /** FSRS-4.5 default parameters w0..w16. */
  static final double[] W = {
    0.4872, 1.4003, 3.7145, 13.8206, 5.1618, 1.2298, 0.8975, 0.031, 1.6474, 0.1367, 1.0461, 2.1072,
    0.0793, 0.3246, 1.587, 0.2272, 2.8755
  };

  static final double DESIRED_RETENTION = 0.9;
  static final int MAX_INTERVAL_DAYS = 36_500;
  private static final double DECAY = -0.5;
  private static final double FACTOR = 19.0 / 81.0;

  private static final int AGAIN = 1;
  private static final int HARD = 2;
  private static final int GOOD = 3;
  private static final int EASY = 4;

  @Override
  public String name() {
    return "fsrs";
  }

  @Override
  public CardSchedule next(CardSchedule current, Grade grade, LocalDate reviewedOn) {
    int rating = rating(grade);
    double stability;
    double difficulty;
    if (current.hasMemoryState() || current.intervalDays() > 0) {
      double s = current.hasMemoryState() ? current.stability() : current.intervalDays();
      double d = current.hasMemoryState() ? current.difficulty() : initialDifficulty(GOOD);
      double r = retrievability(elapsedDays(current, reviewedOn), s);
      difficulty = nextDifficulty(d, rating);
      stability = rating == AGAIN ? forgetStability(d, s, r) : recallStability(d, s, r, rating);
    } else {
      stability = W[rating - 1];
      difficulty = initialDifficulty(rating);
    }
    int interval = interval(stability);
    int repetitions = rating == AGAIN ? 0 : current.repetitions() + 1;
    return new CardSchedule(
        repetitions,
        current.easeFactor(),
        interval,
        reviewedOn.plusDays(interval),
        stability,
        difficulty);
  }

  /** 0–2 Again, 3 Hard, 4 Good, 5 Easy. */
  static int rating(Grade grade) {
    return switch (grade.value()) {
      case 0, 1, 2 -> AGAIN;
      case 3 -> HARD;
      case 4 -> GOOD;
      default -> EASY;
    };
  }

  /** Predicted probability of recall after {@code elapsedDays} with stability {@code s}. */
  static double retrievability(long elapsedDays, double s) {
    return Math.pow(1 + FACTOR * elapsedDays / s, DECAY);
  }

  /** Days until recall is predicted to fall to the desired retention, at least one. */
  static int interval(double stability) {
    double days = stability / FACTOR * (Math.pow(DESIRED_RETENTION, 1 / DECAY) - 1);
    return (int) Math.min(MAX_INTERVAL_DAYS, Math.max(1, Math.round(days)));
  }

  static double initialDifficulty(int rating) {
    return clampDifficulty(W[4] - (rating - GOOD) * W[5]);
  }

  static double nextDifficulty(double d, int rating) {
    double moved = d - W[6] * (rating - GOOD);
    double reverted = W[7] * initialDifficulty(GOOD) + (1 - W[7]) * moved;
    return clampDifficulty(reverted);
  }

  static double recallStability(double d, double s, double r, int rating) {
    double hardPenalty = rating == HARD ? W[15] : 1;
    double easyBonus = rating == EASY ? W[16] : 1;
    double growth =
        Math.exp(W[8])
            * (11 - d)
            * Math.pow(s, -W[9])
            * (Math.exp(W[10] * (1 - r)) - 1)
            * hardPenalty
            * easyBonus;
    return s * (growth + 1);
  }

  static double forgetStability(double d, double s, double r) {
    double forgotten =
        W[11] * Math.pow(d, -W[12]) * (Math.pow(s + 1, W[13]) - 1) * Math.exp(W[14] * (1 - r));
    return Math.min(forgotten, s);
  }

  private static long elapsedDays(CardSchedule current, LocalDate reviewedOn) {
    LocalDate lastReview = current.dueOn().minusDays(current.intervalDays());
    return Math.max(0, ChronoUnit.DAYS.between(lastReview, reviewedOn));
  }

  private static double clampDifficulty(double d) {
    return Math.min(10, Math.max(1, d));
  }
}
