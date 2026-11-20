package dev.studyforge.learning.adapter.persistence;

import dev.studyforge.learning.application.Cards;
import dev.studyforge.learning.domain.Card;
import dev.studyforge.learning.domain.CardSchedule;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Maps {@link Card} to Spring Data JDBC rows and appends the review log. */
@Repository
class JdbcCards implements Cards {

  private final CardRows rows;
  private final ReviewLogRows log;

  JdbcCards(CardRows rows, ReviewLogRows log) {
    this.rows = rows;
    this.log = log;
  }

  @Override
  public Optional<Card> find(UUID id) {
    return rows.findById(id).map(JdbcCards::toDomain);
  }

  @Override
  public List<Card> ofRoadmap(UUID roadmapId) {
    return rows.findByRoadmapIdOrderByFront(roadmapId).stream().map(JdbcCards::toDomain).toList();
  }

  @Override
  public List<Card> dueBy(String learnerId, LocalDate latest) {
    return rows.findByLearnerIdAndDueOnLessThanEqual(learnerId, latest).stream()
        .map(JdbcCards::toDomain)
        .toList();
  }

  @Override
  public Card save(Card card) {
    Long version = rows.findById(card.id()).map(CardRow::version).orElse(null);
    var s = card.schedule();
    var row =
        new CardRow(
            card.id(),
            version,
            card.roadmapId(),
            card.itemId(),
            card.learnerId(),
            card.zone().getId(),
            card.front(),
            card.back(),
            s.repetitions(),
            s.easeFactor(),
            s.intervalDays(),
            s.dueOn());
    return toDomain(rows.save(row));
  }

  @Override
  public void log(Card.Review review, String strategy) {
    var after = review.card().schedule();
    log.save(
        new ReviewLogRow(
            review.id(),
            null,
            review.card().id(),
            review.grade().value(),
            review.durationSeconds(),
            review.before().intervalDays(),
            after.intervalDays(),
            after.easeFactor(),
            strategy,
            review.reviewedAt()));
  }

  private static Card toDomain(CardRow row) {
    return new Card(
        row.id(),
        row.roadmapId(),
        row.itemId(),
        row.learnerId(),
        ZoneId.of(row.zoneId()),
        row.front(),
        row.back(),
        new CardSchedule(row.repetitions(), row.easeFactor(), row.intervalDays(), row.dueOn()));
  }
}
