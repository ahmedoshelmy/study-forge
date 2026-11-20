package dev.studyforge.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A learner finished one roadmap item. Published by learning-service on {@link
 * Topics#PROGRESS_EVENTS}. Schema: {@code contracts/item-completed.schema.json}.
 *
 * @param progressPercent the whole roadmap's progress after this item, 0 to 100
 */
public record ItemCompleted(
    UUID eventId,
    String learnerId,
    String zoneId,
    UUID roadmapId,
    UUID itemId,
    String itemTitle,
    int minutesSpent,
    int progressPercent,
    Instant completedAt) {}
