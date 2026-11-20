package dev.studyforge.learning.application;

import dev.studyforge.contracts.ReviewRecorded;
import dev.studyforge.learning.domain.Card;
import dev.studyforge.learning.domain.CardSchedule;
import dev.studyforge.learning.domain.DomainException;
import dev.studyforge.learning.domain.Grade;
import dev.studyforge.learning.domain.NotFoundException;
import dev.studyforge.learning.domain.SchedulingStrategy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cards and reviews. Recording a review saves the card, logs it and publishes ReviewRecorded. */
@Service
public class ReviewService {

  private final Cards cards;
  private final RoadmapService roadmaps;
  private final SchedulingStrategy strategy;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public ReviewService(
      Cards cards,
      RoadmapService roadmaps,
      SchedulingStrategy strategy,
      ApplicationEventPublisher events,
      Clock clock) {
    this.cards = cards;
    this.roadmaps = roadmaps;
    this.strategy = strategy;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public Card addCard(UUID roadmapId, UUID itemId, String front, String back) {
    var roadmap = roadmaps.get(roadmapId);
    if (itemId != null && !roadmap.contains(itemId)) {
      throw new NotFoundException("item", itemId);
    }
    LocalDate today = clock.instant().atZone(roadmap.zone()).toLocalDate();
    return cards.save(
        new Card(
            UUID.randomUUID(),
            roadmapId,
            itemId,
            roadmap.learnerId(),
            roadmap.zone(),
            front,
            back,
            CardSchedule.fresh(today)));
  }

  @Transactional(readOnly = true)
  public List<Card> ofRoadmap(UUID roadmapId) {
    roadmaps.get(roadmapId);
    return cards.ofRoadmap(roadmapId);
  }

  /** Cards due today in each card's own time zone ("today" ends at the learner's midnight). */
  @Transactional(readOnly = true)
  public List<Card> dueToday(String learnerId) {
    var now = clock.instant();
    LocalDate latestAnywhere = now.atZone(ZoneOffset.MAX).toLocalDate();
    return cards.dueBy(learnerId, latestAnywhere).stream()
        .filter(c -> c.schedule().dueOn(now.atZone(c.zone()).toLocalDate()))
        .sorted(Comparator.comparing((Card c) -> c.schedule().dueOn()))
        .toList();
  }

  @Transactional
  public Card.Review record(UUID cardId, int grade, int durationSeconds) {
    if (durationSeconds > 3600) {
      throw new DomainException("a single review cannot take more than an hour");
    }
    var card = cards.find(cardId).orElseThrow(() -> new NotFoundException("card", cardId));
    var review = card.review(Grade.of(grade), durationSeconds, clock.instant(), strategy);
    cards.save(review.card());
    cards.log(review, strategy.name());
    var after = review.card().schedule();
    events.publishEvent(
        new ReviewRecorded(
            UUID.randomUUID(),
            card.learnerId(),
            card.zone().getId(),
            card.roadmapId(),
            card.id(),
            grade,
            review.before().intervalDays(),
            after.intervalDays(),
            after.easeFactor(),
            after.dueOn(),
            durationSeconds,
            review.reviewedAt()));
    return review;
  }
}
