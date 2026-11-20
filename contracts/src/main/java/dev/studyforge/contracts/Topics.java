package dev.studyforge.contracts;

/**
 * Kafka topic names. Every record is keyed by the learner id so one learner's events stay ordered.
 */
public final class Topics {

  /** {@link ReviewRecorded} facts, one per graded card review. */
  public static final String REVIEW_EVENTS = "review-events";

  /** {@link ItemCompleted} facts, one per roadmap item marked done. */
  public static final String PROGRESS_EVENTS = "progress-events";

  private Topics() {}
}
