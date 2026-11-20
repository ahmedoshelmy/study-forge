package dev.studyforge.insights.application;

import dev.studyforge.insights.domain.LearnerStats.BucketCount;
import dev.studyforge.insights.domain.LearnerStats.DailyActivity;
import dev.studyforge.insights.domain.RetentionBucket;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Port: the insights read models (insights-service's own database). */
public interface ReadModels {

  /** Records the event id; false if it was already processed (the caller then skips it). */
  boolean markProcessed(UUID eventId, String type);

  void rememberZone(String learnerId, ZoneId zone);

  Optional<ZoneId> zoneOf(String learnerId);

  void addReview(String learnerId, LocalDate day, boolean recalled, int seconds);

  void addCompletion(String learnerId, LocalDate day, int minutes);

  void countRetention(String learnerId, RetentionBucket bucket, boolean recalled);

  void scheduleCard(String learnerId, UUID cardId, LocalDate dueOn);

  List<DailyActivity> activity(String learnerId);

  List<BucketCount> retention(String learnerId);

  Map<LocalDate, Long> dueByDay(String learnerId, LocalDate until);

  void deadLetter(String topic, String key, String payload, String error);

  List<DeadLetter> deadLetters(int limit);

  /** A message that could not be applied, parked for a human (or a replay tool). */
  record DeadLetter(long id, String topic, String key, String payload, String error) {}
}
