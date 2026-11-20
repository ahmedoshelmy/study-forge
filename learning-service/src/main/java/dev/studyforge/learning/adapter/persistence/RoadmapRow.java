package dev.studyforge.learning.adapter.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/** Spring Data JDBC aggregate: roadmap row plus its stage and item rows (one level each). */
@Table("roadmap")
record RoadmapRow(
    @Id UUID id,
    @Version Long version,
    String learnerId,
    String zoneId,
    String title,
    Instant createdAt,
    @MappedCollection(idColumn = "roadmap_id", keyColumn = "position") List<StageRow> stages,
    @MappedCollection(idColumn = "roadmap_id", keyColumn = "position") List<ItemRow> items) {

  @Table("stage")
  record StageRow(UUID stageId, String title) {}

  @Table("roadmap_item")
  record ItemRow(
      UUID itemId, UUID stageId, String title, int estimatedMinutes, Instant completedAt) {}
}
