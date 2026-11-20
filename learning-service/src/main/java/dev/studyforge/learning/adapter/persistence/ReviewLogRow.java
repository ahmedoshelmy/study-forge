package dev.studyforge.learning.adapter.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Append-only log of every review (the source for re-scheduling with another strategy later). */
@Table("review_log")
record ReviewLogRow(
    @Id UUID id,
    @Version Long version,
    UUID cardId,
    int grade,
    int durationSeconds,
    int intervalBefore,
    int intervalAfter,
    double easeAfter,
    String strategy,
    Instant reviewedAt) {}
