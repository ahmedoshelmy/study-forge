package dev.studyforge.contracts;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A learner graded one card. Published by learning-service on {@link Topics#REVIEW_EVENTS}.
 *
 * <p>Versioned by additive change only: consumers ignore unknown fields, producers never remove or
 * rename one. The JSON schema lives in {@code contracts/review-recorded.schema.json}.
 *
 * @param eventId unique per fact; consumers use it to apply each event once
 * @param learnerId the Kafka key
 * @param zoneId the learner's time zone, so day boundaries (streaks) follow local midnight
 * @param grade 0 (blackout) to 5 (perfect), SM-2 scale
 * @param intervalDays the interval the scheduler chose after this review
 * @param nextDueOn the learner-local date the card is due again
 */
public record ReviewRecorded(
    UUID eventId,
    String learnerId,
    String zoneId,
    UUID roadmapId,
    UUID cardId,
    int grade,
    int previousIntervalDays,
    int intervalDays,
    double easeFactor,
    LocalDate nextDueOn,
    int durationSeconds,
    Instant reviewedAt) {}
