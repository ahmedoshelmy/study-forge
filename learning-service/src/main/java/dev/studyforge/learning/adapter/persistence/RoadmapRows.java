package dev.studyforge.learning.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

/** Spring Data JDBC repository; only the port adapter in this package uses it. */
interface RoadmapRows extends ListCrudRepository<RoadmapRow, UUID> {

  List<RoadmapRow> findByLearnerIdOrderByCreatedAt(String learnerId);
}
