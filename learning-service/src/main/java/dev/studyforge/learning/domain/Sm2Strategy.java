package dev.studyforge.learning.domain;

import java.time.LocalDate;

/**
 * SuperMemo-2 (Wozniak, 1987).
 *
 * <ul>
 *   <li>Recalled (grade &gt;= 3): interval 1 day, then 6 days, then previous interval times the
 *       ease factor (rounded, never shorter than before).
 *   <li>Lapse (grade &lt; 3): repetitions reset, the card comes back tomorrow.
 *   <li>Ease moves by {@code 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)} on every review and never
 *       drops below 1.3, so hard cards still grow (slowly).
 * </ul>
 */
public final class Sm2Strategy implements SchedulingStrategy {

  @Override
  public String name() {
    return "sm2";
  }

  @Override
  public CardSchedule next(CardSchedule current, Grade grade, LocalDate reviewedOn) {
    double ease = nextEase(current.easeFactor(), grade.value());
    if (!grade.recalled()) {
      return new CardSchedule(0, ease, 1, reviewedOn.plusDays(1));
    }
    int repetitions = current.repetitions() + 1;
    int interval =
        switch (repetitions) {
          case 1 -> 1;
          case 2 -> 6;
          default ->
              Math.max(current.intervalDays(), (int) Math.round(current.intervalDays() * ease));
        };
    return new CardSchedule(repetitions, ease, interval, reviewedOn.plusDays(interval));
  }

  static double nextEase(double ease, int quality) {
    int miss = Grade.MAX - quality;
    double next = ease + (0.1 - miss * (0.08 + miss * 0.02));
    double rounded = Math.round(next * 100) / 100.0;
    return Math.max(CardSchedule.MIN_EASE, rounded);
  }
}
