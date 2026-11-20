package dev.studyforge.insights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import dev.studyforge.insights.domain.LearnerStats.BucketCount;
import dev.studyforge.insights.domain.LearnerStats.DailyActivity;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LearnerStatsTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

  @Test
  void weeklyMinutesCoverTheLastSevenLocalDaysIncludingEmptyOnes() {
    var stats =
        LearnerStats.assemble(
            "l",
            TODAY,
            List.of(
                new DailyActivity(TODAY, 3, 2, 150, 1, 30),
                new DailyActivity(TODAY.minusDays(8), 1, 1, 60, 0, 0)),
            List.of(),
            Map.of());
    assertThat(stats.weeklyMinutes()).hasSize(7);
    assertThat(stats.weeklyMinutes().getFirst().day()).isEqualTo(TODAY.minusDays(6));
    assertThat(stats.weeklyMinutes().getLast().value()).isEqualTo(33);
    assertThat(stats.totalReviews()).isEqualTo(4);
    assertThat(stats.itemsCompleted()).isEqualTo(1);
  }

  @Test
  void retentionIsRecalledOverReviewedPerBucketAndEmptyBucketsHaveNoPercent() {
    var stats =
        LearnerStats.assemble(
            "l", TODAY, List.of(), List.of(new BucketCount(RetentionBucket.WEEK, 4, 3)), Map.of());
    assertThat(stats.retention()).hasSize(RetentionBucket.values().length);
    assertThat(stats.retention().get(RetentionBucket.WEEK.ordinal()).percent()).isEqualTo(75);
    assertThat(stats.retention().get(RetentionBucket.NEW.ordinal()).percent()).isNull();
  }

  @Test
  void overdueCardsAreFoldedIntoTodayOfTheForecast() {
    var stats =
        LearnerStats.assemble(
            "l",
            TODAY,
            List.of(),
            List.of(),
            Map.of(TODAY.minusDays(3), 2L, TODAY, 1L, TODAY.plusDays(6), 5L));
    assertThat(stats.forecast()).hasSize(14);
    assertThat(stats.forecast().get(0).value()).isEqualTo(3);
    assertThat(stats.forecast().get(6).value()).isEqualTo(5);
  }

  @Test
  void bucketsFollowThePreviousInterval() {
    assertThat(RetentionBucket.forInterval(0)).isEqualTo(RetentionBucket.NEW);
    assertThat(RetentionBucket.forInterval(6)).isEqualTo(RetentionBucket.WEEK);
    assertThat(RetentionBucket.forInterval(400)).isEqualTo(RetentionBucket.LONG);
  }
}
