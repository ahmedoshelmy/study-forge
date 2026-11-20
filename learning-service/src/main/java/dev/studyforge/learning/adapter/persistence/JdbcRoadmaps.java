package dev.studyforge.learning.adapter.persistence;

import dev.studyforge.learning.adapter.persistence.RoadmapRow.ItemRow;
import dev.studyforge.learning.adapter.persistence.RoadmapRow.StageRow;
import dev.studyforge.learning.application.Roadmaps;
import dev.studyforge.learning.domain.Roadmap;
import dev.studyforge.learning.domain.RoadmapItem;
import dev.studyforge.learning.domain.Stage;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Maps the pure {@link Roadmap} aggregate to and from Spring Data JDBC rows. */
@Repository
class JdbcRoadmaps implements Roadmaps {

  private final RoadmapRows rows;

  JdbcRoadmaps(RoadmapRows rows) {
    this.rows = rows;
  }

  @Override
  public Optional<Roadmap> find(UUID id) {
    return rows.findById(id).map(JdbcRoadmaps::toDomain);
  }

  @Override
  public List<Roadmap> ofLearner(String learnerId) {
    return rows.findByLearnerIdOrderByCreatedAt(learnerId).stream()
        .map(JdbcRoadmaps::toDomain)
        .toList();
  }

  @Override
  public Roadmap save(Roadmap roadmap) {
    Long version = rows.findById(roadmap.id()).map(RoadmapRow::version).orElse(null);
    var stages = new ArrayList<StageRow>();
    var items = new ArrayList<ItemRow>();
    for (var stage : roadmap.stages()) {
      stages.add(new StageRow(stage.id(), stage.title()));
      for (var item : stage.items()) {
        items.add(
            new ItemRow(
                item.id(), stage.id(), item.title(), item.estimatedMinutes(), item.completedAt()));
      }
    }
    var row =
        new RoadmapRow(
            roadmap.id(),
            version,
            roadmap.learnerId(),
            roadmap.zone().getId(),
            roadmap.title(),
            roadmap.createdAt(),
            stages,
            items);
    return toDomain(rows.save(row));
  }

  private static Roadmap toDomain(RoadmapRow row) {
    var stages =
        row.stages().stream()
            .map(s ->
                new Stage(
                    s.stageId(),
                    s.title(),
                    row.items().stream()
                        .filter(i -> i.stageId().equals(s.stageId()))
                        .map(i ->
                            new RoadmapItem(
                                i.itemId(), i.title(), i.estimatedMinutes(), i.completedAt()))
                        .toList()))
            .toList();
    return new Roadmap(
        row.id(), row.learnerId(), ZoneId.of(row.zoneId()), row.title(), stages, row.createdAt());
  }
}
