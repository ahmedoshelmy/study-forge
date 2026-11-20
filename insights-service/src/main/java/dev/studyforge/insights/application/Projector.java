package dev.studyforge.insights.application;

import dev.studyforge.contracts.ItemCompleted;
import dev.studyforge.contracts.ReviewRecorded;
import dev.studyforge.insights.domain.RetentionBucket;
import java.time.DateTimeException;
import java.time.ZoneId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Folds events into the read models. Each event is applied exactly once: its id goes into {@code
 * processed_event} in the same transaction as the counters, so a redelivered event (the producer's
 * outbox is at-least-once) finds its id and changes nothing.
 */
@Service
public class Projector {

  private static final int GRADE_PASS = 3;

  private final ReadModels models;
  private final ApplicationEventPublisher events;

  public Projector(ReadModels models, ApplicationEventPublisher events) {
    this.models = models;
    this.events = events;
  }

  /** Returns false when the event was a duplicate. */
  @Transactional
  public boolean apply(ReviewRecorded e) {
    if (e.eventId() == null || e.learnerId() == null || e.reviewedAt() == null) {
      throw new InvalidEventException("ReviewRecorded without id, learner or time", null);
    }
    if (!models.markProcessed(e.eventId(), "ReviewRecorded")) {
      return false;
    }
    var zone = zone(e.zoneId());
    var day = e.reviewedAt().atZone(zone).toLocalDate();
    boolean recalled = e.grade() >= GRADE_PASS;
    models.rememberZone(e.learnerId(), zone);
    models.addReview(e.learnerId(), day, recalled, e.durationSeconds());
    models.countRetention(
        e.learnerId(), RetentionBucket.forInterval(e.previousIntervalDays()), recalled);
    if (e.cardId() != null && e.nextDueOn() != null) {
      models.scheduleCard(e.learnerId(), e.cardId(), e.nextDueOn());
    }
    events.publishEvent(new StatsChanged(e.learnerId()));
    return true;
  }

  /** Returns false when the event was a duplicate. */
  @Transactional
  public boolean apply(ItemCompleted e) {
    if (e.eventId() == null || e.learnerId() == null || e.completedAt() == null) {
      throw new InvalidEventException("ItemCompleted without id, learner or time", null);
    }
    if (!models.markProcessed(e.eventId(), "ItemCompleted")) {
      return false;
    }
    var zone = zone(e.zoneId());
    models.rememberZone(e.learnerId(), zone);
    models.addCompletion(
        e.learnerId(), e.completedAt().atZone(zone).toLocalDate(), Math.max(0, e.minutesSpent()));
    events.publishEvent(new StatsChanged(e.learnerId()));
    return true;
  }

  private static ZoneId zone(String zoneId) {
    try {
      return zoneId == null ? ZoneId.of("UTC") : ZoneId.of(zoneId);
    } catch (DateTimeException ex) {
      throw new InvalidEventException("unknown zone " + zoneId, ex);
    }
  }
}
