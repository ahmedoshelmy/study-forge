package dev.studyforge.insights.domain;

/**
 * Groups reviews by how long the card had rested before the review. Recall rate per bucket is the
 * learner's retention curve: it should drop slowly as intervals grow if the scheduler is tuned.
 */
public enum RetentionBucket {
  NEW("new", 0, 0),
  ONE_DAY("1 day", 1, 1),
  WEEK("2-6 days", 2, 6),
  FEW_WEEKS("7-20 days", 7, 20),
  LONG("21+ days", 21, Integer.MAX_VALUE);

  private final String label;
  private final int fromDays;
  private final int toDays;

  RetentionBucket(String label, int fromDays, int toDays) {
    this.label = label;
    this.fromDays = fromDays;
    this.toDays = toDays;
  }

  public String label() {
    return label;
  }

  public static RetentionBucket forInterval(int previousIntervalDays) {
    for (var bucket : values()) {
      if (previousIntervalDays >= bucket.fromDays && previousIntervalDays <= bucket.toDays) {
        return bucket;
      }
    }
    throw new IllegalArgumentException("negative interval " + previousIntervalDays);
  }
}
