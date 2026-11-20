package dev.studyforge.insights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreakTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

  @Test
  void countsBackFromTodayWhenTheLearnerStudiedToday() {
    var streak = Streak.of(List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY);
    assertThat(streak.current()).isEqualTo(3);
    assertThat(streak.studiedToday()).isTrue();
  }

  @Test
  void todayIsNotOverSoYesterdaysRunStillCounts() {
    var streak = Streak.of(List.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY);
    assertThat(streak.current()).isEqualTo(2);
    assertThat(streak.studiedToday()).isFalse();
  }

  @Test
  void aMissedDayResetsTheCurrentStreakButNotTheLongest() {
    var days =
        List.of(
            TODAY.minusDays(2),
            TODAY.minusDays(6),
            TODAY.minusDays(7),
            TODAY.minusDays(8),
            TODAY.minusDays(9));
    var streak = Streak.of(days, TODAY);
    assertThat(streak.current()).isZero();
    assertThat(streak.longest()).isEqualTo(4);
  }

  @Test
  void noActivityIsAZeroStreak() {
    assertThat(Streak.of(List.of(), TODAY)).isEqualTo(new Streak(0, 0, false));
  }
}
