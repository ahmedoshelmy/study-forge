package dev.studyforge.insights.application;

import dev.studyforge.insights.domain.LearnerStats;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the dashboard numbers for "today" in the learner's own time zone. */
@Service
public class StatsQueries {

  private final ReadModels models;
  private final Clock clock;

  public StatsQueries(ReadModels models, Clock clock) {
    this.models = models;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public LearnerStats stats(String learnerId) {
    var zone = models.zoneOf(learnerId).orElse(ZoneId.of("UTC"));
    var today = clock.instant().atZone(zone).toLocalDate();
    return LearnerStats.assemble(
        learnerId,
        today,
        models.activity(learnerId),
        models.retention(learnerId),
        models.dueByDay(learnerId, today.plusDays(LearnerStats.FORECAST_DAYS)));
  }

  @Transactional(readOnly = true)
  public List<ReadModels.DeadLetter> deadLetters() {
    return models.deadLetters(50);
  }
}
