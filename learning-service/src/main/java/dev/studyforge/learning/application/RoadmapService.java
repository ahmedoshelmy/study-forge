package dev.studyforge.learning.application;

import dev.studyforge.contracts.ItemCompleted;
import dev.studyforge.learning.domain.DomainException;
import dev.studyforge.learning.domain.ItemCompletion;
import dev.studyforge.learning.domain.NotFoundException;
import dev.studyforge.learning.domain.Roadmap;
import dev.studyforge.learning.domain.RoadmapItem;
import dev.studyforge.learning.domain.Stage;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases around roadmaps. Events go through the transactional outbox (Spring Modulith). */
@Service
public class RoadmapService {

  private final Roadmaps roadmaps;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public RoadmapService(Roadmaps roadmaps, ApplicationEventPublisher events, Clock clock) {
    this.roadmaps = roadmaps;
    this.events = events;
    this.clock = clock;
  }

  /** Input for a new roadmap: stage titles with their items. */
  public record NewRoadmap(String learnerId, String zoneId, String title, List<NewStage> stages) {}

  public record NewStage(String title, List<NewItem> items) {}

  public record NewItem(String title, int estimatedMinutes) {}

  @Transactional
  public Roadmap create(NewRoadmap input) {
    var stages =
        input.stages().stream()
            .map(s ->
                new Stage(
                    UUID.randomUUID(),
                    s.title(),
                    s.items().stream()
                        .map(i -> RoadmapItem.planned(i.title(), i.estimatedMinutes()))
                        .toList()))
            .toList();
    var roadmap =
        new Roadmap(
            UUID.randomUUID(),
            input.learnerId(),
            Zones.parse(input.zoneId()),
            input.title(),
            stages,
            clock.instant());
    return roadmaps.save(roadmap);
  }

  @Transactional(readOnly = true)
  public Roadmap get(UUID id) {
    return roadmaps.find(id).orElseThrow(() -> new NotFoundException("roadmap", id));
  }

  @Transactional(readOnly = true)
  public List<Roadmap> ofLearner(String learnerId) {
    return roadmaps.ofLearner(learnerId);
  }

  /** Marks an item done and publishes ItemCompleted once; repeating the call changes nothing. */
  @Transactional
  public Roadmap completeItem(UUID roadmapId, UUID itemId) {
    var completion = get(roadmapId).complete(itemId, clock.instant());
    if (completion instanceof ItemCompletion.Completed(Roadmap updated, RoadmapItem item)) {
      var saved = roadmaps.save(updated);
      events.publishEvent(
          new ItemCompleted(
              UUID.randomUUID(),
              saved.learnerId(),
              saved.zone().getId(),
              saved.id(),
              item.id(),
              item.title(),
              item.estimatedMinutes(),
              saved.progressPercent(),
              item.completedAt()));
      return saved;
    }
    return completion.roadmap();
  }

  /** Small helper so a bad zone id is a 400, not a 500. */
  static final class Zones {
    private Zones() {}

    static ZoneId parse(String zoneId) {
      try {
        return ZoneId.of(zoneId == null || zoneId.isBlank() ? "UTC" : zoneId);
      } catch (DateTimeException e) {
        throw new DomainException("unknown time zone " + zoneId);
      }
    }
  }
}
