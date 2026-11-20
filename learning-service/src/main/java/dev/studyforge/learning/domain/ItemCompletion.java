package dev.studyforge.learning.domain;

/** What happened when the learner ticked an item: a new fact, or nothing (already done). */
public sealed interface ItemCompletion {

  Roadmap roadmap();

  /** The item was open and is now done; the caller publishes ItemCompleted. */
  record Completed(Roadmap roadmap, RoadmapItem item) implements ItemCompletion {}

  /** Ticking a done item again is a no-op, so retries from the UI never double count. */
  record AlreadyDone(Roadmap roadmap, RoadmapItem item) implements ItemCompletion {}
}
