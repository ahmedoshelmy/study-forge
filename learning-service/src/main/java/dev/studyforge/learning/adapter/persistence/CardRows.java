package dev.studyforge.learning.adapter.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

/** Spring Data JDBC repository; only the port adapter in this package uses it. */
interface CardRows extends ListCrudRepository<CardRow, UUID> {

  List<CardRow> findByRoadmapIdOrderByFront(UUID roadmapId);

  List<CardRow> findByLearnerIdAndDueOnLessThanEqual(String learnerId, LocalDate latest);
}
