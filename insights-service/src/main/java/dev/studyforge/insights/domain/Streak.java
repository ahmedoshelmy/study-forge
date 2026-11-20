package dev.studyforge.insights.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.TreeSet;

/**
 * Days in a row with at least one review or completed item, counted on the learner's local
 * calendar.
 *
 * <p>Rules: the current streak ends today if the learner already studied today, otherwise it ends
 * yesterday (today is not over yet, so it cannot break the streak before local midnight). A day
 * without activity before that resets it to zero. {@code longest} is the best run ever.
 */
public record Streak(int current, int longest, boolean studiedToday) {

  public static Streak of(Collection<LocalDate> activeDays, LocalDate today) {
    var days = new TreeSet<>(activeDays);
    days.removeIf(d -> d.isAfter(today));
    boolean studiedToday = days.contains(today);

    int current = 0;
    for (var d = studiedToday ? today : today.minusDays(1); days.contains(d); d = d.minusDays(1)) {
      current++;
    }

    int longest = 0;
    int run = 0;
    LocalDate previous = null;
    for (var d : days) {
      run = previous != null && previous.plusDays(1).equals(d) ? run + 1 : 1;
      longest = Math.max(longest, run);
      previous = d;
    }
    return new Streak(current, longest, studiedToday);
  }
}
