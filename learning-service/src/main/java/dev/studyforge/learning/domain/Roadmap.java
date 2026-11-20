package dev.studyforge.learning.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A learner's plan: ordered stages of items. The aggregate root for progress. */
public record Roadmap(
    UUID id, String learnerId, ZoneId zone, String title, List<Stage> stages, Instant createdAt) {

  public Roadmap {
    Objects.requireNonNull(id);
    Objects.requireNonNull(zone);
    Objects.requireNonNull(createdAt);
    if (learnerId == null || learnerId.isBlank()) {
      throw new DomainException("a roadmap belongs to a learner");
    }
    if (title == null || title.isBlank()) {
      throw new DomainException("a roadmap needs a title");
    }
    stages = List.copyOf(stages);
    if (stages.isEmpty()) {
      throw new DomainException("a roadmap needs at least one stage");
    }
  }

  /** Whole-roadmap progress, 0 to 100, weighted by item count (an empty roadmap is 0%). */
  public int progressPercent() {
    long total = stages.stream().mapToLong(s -> s.items().size()).sum();
    if (total == 0) {
      return 0;
    }
    long done = stages.stream().mapToLong(Stage::doneCount).sum();
    return (int) Math.floorDiv(done * 100, total);
  }

  public ItemCompletion complete(UUID itemId, Instant at) {
    var updated = new ArrayList<Stage>(stages.size());
    RoadmapItem found = null;
    for (var stage : stages) {
      var items = new ArrayList<RoadmapItem>(stage.items().size());
      for (var item : stage.items()) {
        if (item.id().equals(itemId)) {
          if (item.done()) {
            return new ItemCompletion.AlreadyDone(this, item);
          }
          found = item.completed(at);
          items.add(found);
        } else {
          items.add(item);
        }
      }
      updated.add(new Stage(stage.id(), stage.title(), items));
    }
    if (found == null) {
      throw new NotFoundException("item", itemId);
    }
    return new ItemCompletion.Completed(
        new Roadmap(id, learnerId, zone, title, updated, createdAt), found);
  }

  public boolean contains(UUID itemId) {
    return stages.stream().flatMap(s -> s.items().stream()).anyMatch(i -> i.id().equals(itemId));
  }
}
