package dev.studyforge.learning.domain;

import java.time.Instant;
import java.util.UUID;

/** One thing to study: a chapter, a video, a kata. {@code completedAt} is null until done. */
public record RoadmapItem(UUID id, String title, int estimatedMinutes, Instant completedAt) {

  public RoadmapItem {
    if (title == null || title.isBlank()) {
      throw new DomainException("an item needs a title");
    }
    if (estimatedMinutes < 0) {
      throw new DomainException("estimated minutes cannot be negative");
    }
  }

  public static RoadmapItem planned(String title, int estimatedMinutes) {
    return new RoadmapItem(UUID.randomUUID(), title, estimatedMinutes, null);
  }

  public boolean done() {
    return completedAt != null;
  }

  RoadmapItem completed(Instant at) {
    return new RoadmapItem(id, title, estimatedMinutes, at);
  }
}
