package dev.studyforge.insights.adapter.web;

import dev.studyforge.insights.domain.LearnerStats;
import dev.studyforge.insights.domain.Streak;
import java.time.LocalDate;
import java.util.List;

/** JSON shape of the dashboard (decoupled from the domain record). */
record StatsView(
    String learnerId,
    LocalDate today,
    long totalReviews,
    long itemsCompleted,
    Streak streak,
    List<LearnerStats.DayValue> weeklyMinutes,
    List<LearnerStats.Retention> retention,
    List<LearnerStats.DayValue> forecast) {

  static StatsView of(LearnerStats s) {
    return new StatsView(
        s.learnerId(),
        s.today(),
        s.totalReviews(),
        s.itemsCompleted(),
        s.streak(),
        s.weeklyMinutes(),
        s.retention(),
        s.forecast());
  }
}
