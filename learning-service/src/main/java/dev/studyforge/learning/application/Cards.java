package dev.studyforge.learning.application;

import dev.studyforge.learning.domain.Card;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port: card persistence and the review log. */
public interface Cards {

  Optional<Card> find(UUID id);

  List<Card> ofRoadmap(UUID roadmapId);

  /** Cards of the learner due on or before {@code latest}, oldest first. */
  List<Card> dueBy(String learnerId, LocalDate latest);

  Card save(Card card);

  void log(Card.Review review, String strategy);
}
