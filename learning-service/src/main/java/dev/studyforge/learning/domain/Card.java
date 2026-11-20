package dev.studyforge.learning.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/** A question/answer pair the learner reviews, attached to a roadmap (and optionally an item). */
public record Card(
    UUID id,
    UUID roadmapId,
    UUID itemId,
    String learnerId,
    ZoneId zone,
    String front,
    String back,
    CardSchedule schedule) {

  public Card {
    Objects.requireNonNull(id);
    Objects.requireNonNull(roadmapId);
    Objects.requireNonNull(zone);
    if (front == null || front.isBlank() || back == null || back.isBlank()) {
      throw new DomainException("a card needs a front and a back");
    }
  }

  /** Applies one review; the review day is the learner's local date, not the server's. */
  public Review review(Grade grade, int durationSeconds, Instant at, SchedulingStrategy strategy) {
    if (durationSeconds < 0) {
      throw new DomainException("duration cannot be negative");
    }
    LocalDate today = at.atZone(zone).toLocalDate();
    var after = strategy.next(schedule, grade, today);
    var reviewed = new Card(id, roadmapId, itemId, learnerId, zone, front, back, after);
    return new Review(UUID.randomUUID(), reviewed, schedule, grade, durationSeconds, at);
  }

  /** The outcome of reviewing a card once. */
  public record Review(
      UUID id,
      Card card,
      CardSchedule before,
      Grade grade,
      int durationSeconds,
      Instant reviewedAt) {}
}
