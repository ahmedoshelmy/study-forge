package dev.studyforge.learning.domain;

import java.util.List;
import java.util.UUID;

/** An ordered group of items ("Basics", "Deep dive", "Project"). */
public record Stage(UUID id, String title, List<RoadmapItem> items) {

  public Stage {
    if (title == null || title.isBlank()) {
      throw new DomainException("a stage needs a title");
    }
    items = List.copyOf(items);
  }

  public long doneCount() {
    return items.stream().filter(RoadmapItem::done).count();
  }
}
