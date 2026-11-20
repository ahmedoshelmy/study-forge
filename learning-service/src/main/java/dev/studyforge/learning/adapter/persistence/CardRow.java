package dev.studyforge.learning.adapter.persistence;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Spring Data JDBC aggregate: one card with its current schedule. */
@Table("card")
record CardRow(
    @Id UUID id,
    @Version Long version,
    UUID roadmapId,
    UUID itemId,
    String learnerId,
    String zoneId,
    String front,
    String back,
    int repetitions,
    double easeFactor,
    int intervalDays,
    LocalDate dueOn) {}
