package dev.studyforge.insights.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Everything the dashboard shows for one learner, computed from the read-model rows. */
public record LearnerStats(
    String learnerId,
    LocalDate today,
    long totalReviews,
    long itemsCompleted,
    Streak streak,
    List<DayValue> weeklyMinutes,
    List<Retention> retention,
    List<DayValue> forecast) {

  public static final int WEEK = 7;
  public static final int FORECAST_DAYS = 14;

  /** A value on a calendar day (minutes studied, or cards due). */
  public record DayValue(LocalDate day, long value) {}

  /** Recall rate for one interval bucket; {@code percent} is null when there are no reviews. */
  public record Retention(String bucket, long reviews, long recalled, Integer percent) {}

  /** One learner-local day of activity, as stored by the projection. */
  public record DailyActivity(
      LocalDate day, long reviews, long recalled, long seconds, long itemsCompleted, long minutes) {

    boolean active() {
      return reviews > 0 || itemsCompleted > 0;
    }

    long totalMinutes() {
      return Math.round(seconds / 60.0) + minutes;
    }
  }

  /** Recall counts per bucket, as stored by the projection. */
  public record BucketCount(RetentionBucket bucket, long reviews, long recalled) {}

  public static LearnerStats assemble(
      String learnerId,
      LocalDate today,
      Collection<DailyActivity> days,
      Collection<BucketCount> buckets,
      Map<LocalDate, Long> dueByDay) {
    var byDay = new HashMap<LocalDate, DailyActivity>();
    days.forEach(d -> byDay.put(d.day(), d));

    var weekly = new ArrayList<DayValue>(WEEK);
    for (int i = WEEK - 1; i >= 0; i--) {
      var day = today.minusDays(i);
      var activity = byDay.get(day);
      weekly.add(new DayValue(day, activity == null ? 0 : activity.totalMinutes()));
    }

    var retention = new ArrayList<Retention>();
    for (var bucket : RetentionBucket.values()) {
      var count =
          buckets.stream()
              .filter(b -> b.bucket() == bucket)
              .findFirst()
              .orElse(new BucketCount(bucket, 0, 0));
      Integer percent =
          count.reviews() == 0
              ? null
              : (int) Math.round(100.0 * count.recalled() / count.reviews());
      retention.add(new Retention(bucket.label(), count.reviews(), count.recalled(), percent));
    }

    var forecast = new ArrayList<DayValue>(FORECAST_DAYS);
    long overdue =
        dueByDay.entrySet().stream()
            .filter(e -> e.getKey().isBefore(today))
            .mapToLong(Map.Entry::getValue)
            .sum();
    for (int i = 0; i < FORECAST_DAYS; i++) {
      var day = today.plusDays(i);
      long due = dueByDay.getOrDefault(day, 0L) + (i == 0 ? overdue : 0);
      forecast.add(new DayValue(day, due));
    }

    var activeDays = days.stream().filter(DailyActivity::active).map(DailyActivity::day).toList();
    return new LearnerStats(
        learnerId,
        today,
        days.stream().mapToLong(DailyActivity::reviews).sum(),
        days.stream().mapToLong(DailyActivity::itemsCompleted).sum(),
        Streak.of(activeDays, today),
        weekly,
        retention,
        forecast);
  }
}
