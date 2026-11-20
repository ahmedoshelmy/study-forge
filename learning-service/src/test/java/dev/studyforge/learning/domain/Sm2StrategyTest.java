package dev.studyforge.learning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class Sm2StrategyTest {

  private static final LocalDate DAY = LocalDate.of(2026, 3, 1);
  private final Sm2Strategy sm2 = new Sm2Strategy();

  @Test
  void aFreshCardGoesOneDayThenSixDaysThenGrowsByEase() {
    var first = sm2.next(CardSchedule.fresh(DAY), Grade.of(4), DAY);
    assertThat(first.intervalDays()).isEqualTo(1);
    assertThat(first.dueOn()).isEqualTo(DAY.plusDays(1));

    var second = sm2.next(first, Grade.of(4), first.dueOn());
    assertThat(second.intervalDays()).isEqualTo(6);

    var third = sm2.next(second, Grade.of(4), second.dueOn());
    assertThat(third.intervalDays()).isEqualTo(15);
    assertThat(third.repetitions()).isEqualTo(3);
  }

  @Test
  void aLapseResetsRepetitionsAndBringsTheCardBackTomorrow() {
    var learned = new CardSchedule(4, 2.6, 40, DAY);
    var lapsed = sm2.next(learned, Grade.of(1), DAY);
    assertThat(lapsed.repetitions()).isZero();
    assertThat(lapsed.intervalDays()).isEqualTo(1);
    assertThat(lapsed.dueOn()).isEqualTo(DAY.plusDays(1));
    assertThat(lapsed.easeFactor()).isLessThan(2.6);
  }

  @ParameterizedTest(name = "grade {0} moves ease 2.5 to {1}")
  @CsvSource({"5, 2.6", "4, 2.5", "3, 2.36", "2, 2.18", "1, 1.96", "0, 1.7"})
  void easeFollowsTheSm2Formula(int grade, double expected) {
    assertThat(Sm2Strategy.nextEase(2.5, grade)).isEqualTo(expected);
  }

  @Test
  void easeNeverDropsBelowTheFloor() {
    assertThat(Sm2Strategy.nextEase(1.31, 0)).isEqualTo(CardSchedule.MIN_EASE);
  }

  @Test
  void gradesOutsideZeroToFiveAreRejected() {
    assertThatThrownBy(() -> Grade.of(6)).isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> Grade.of(-1)).isInstanceOf(DomainException.class);
  }
}
