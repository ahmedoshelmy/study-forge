package dev.studyforge.learning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FsrsStrategyTest {

  private static final LocalDate DAY = LocalDate.of(2026, 3, 1);
  private final FsrsStrategy fsrs = new FsrsStrategy();

  @Test
  void firstReviewTakesTheInitialStabilityForTheRating() {
    var fresh = CardSchedule.fresh(DAY);

    var again = fsrs.next(fresh, Grade.of(1), DAY);
    var hard = fsrs.next(fresh, Grade.of(3), DAY);
    var good = fsrs.next(fresh, Grade.of(4), DAY);
    var easy = fsrs.next(fresh, Grade.of(5), DAY);

    assertThat(again.stability()).isEqualTo(FsrsStrategy.W[0]);
    assertThat(again.intervalDays()).isEqualTo(1);
    assertThat(again.repetitions()).isZero();
    assertThat(hard.intervalDays()).isEqualTo(1);
    assertThat(good.intervalDays()).isEqualTo(4);
    assertThat(good.dueOn()).isEqualTo(DAY.plusDays(4));
    assertThat(easy.intervalDays()).isEqualTo(14);
    assertThat(good.difficulty()).isEqualTo(FsrsStrategy.W[4]);
    assertThat(easy.difficulty()).isLessThan(good.difficulty());
    assertThat(again.difficulty()).isGreaterThan(good.difficulty());
  }

  @Test
  void atTheTargetIntervalRecallIsNinetyPercent() {
    assertThat(FsrsStrategy.retrievability(10, 10.0)).isCloseTo(0.9, within(1e-9));
    assertThat(FsrsStrategy.interval(10.0)).isEqualTo(10);
  }

  @Test
  void aSecondGoodReviewOnTheDueDateRoughlyQuadruplesStability() {
    var first = fsrs.next(CardSchedule.fresh(DAY), Grade.of(4), DAY);
    var second = fsrs.next(first, Grade.of(4), first.dueOn());

    assertThat(second.stability()).isBetween(14.0, 16.0);
    assertThat(second.intervalDays()).isBetween(14, 16);
    assertThat(second.repetitions()).isEqualTo(2);
  }

  @Test
  void aLapseShrinksStabilityAndResetsTheStreak() {
    var learned = fsrs.next(CardSchedule.fresh(DAY), Grade.of(5), DAY);
    var lapsed = fsrs.next(learned, Grade.of(0), learned.dueOn());

    assertThat(lapsed.stability()).isLessThan(learned.stability());
    assertThat(lapsed.repetitions()).isZero();
    assertThat(lapsed.difficulty()).isGreaterThan(learned.difficulty());
  }

  @Test
  void recallingAnOverdueCardGrowsStabilityMoreThanRecallingItOnTime() {
    var first = fsrs.next(CardSchedule.fresh(DAY), Grade.of(4), DAY);

    var onTime = fsrs.next(first, Grade.of(4), first.dueOn());
    var late = fsrs.next(first, Grade.of(4), first.dueOn().plusDays(10));

    assertThat(late.stability()).isGreaterThan(onTime.stability());
  }

  @Test
  void easierGradesNeverGiveShorterIntervals() {
    var first = fsrs.next(CardSchedule.fresh(DAY), Grade.of(4), DAY);
    var hard = fsrs.next(first, Grade.of(3), first.dueOn());
    var good = fsrs.next(first, Grade.of(4), first.dueOn());
    var easy = fsrs.next(first, Grade.of(5), first.dueOn());

    assertThat(hard.intervalDays()).isLessThanOrEqualTo(good.intervalDays());
    assertThat(good.intervalDays()).isLessThan(easy.intervalDays());
  }

  @Test
  void aCardScheduledBySm2KeepsItsProgressWhenSwitchingToFsrs() {
    var sm2Card = new CardSchedule(3, 2.6, 15, DAY.plusDays(15));

    var next = fsrs.next(sm2Card, Grade.of(4), sm2Card.dueOn());

    assertThat(next.stability()).isGreaterThan(15.0);
    assertThat(next.intervalDays()).isGreaterThan(15);
    assertThat(next.easeFactor()).isEqualTo(2.6);
    assertThat(next.repetitions()).isEqualTo(4);
  }

  @Test
  void gradesMapToFsrsRatings() {
    assertThat(FsrsStrategy.rating(Grade.of(0))).isEqualTo(1);
    assertThat(FsrsStrategy.rating(Grade.of(2))).isEqualTo(1);
    assertThat(FsrsStrategy.rating(Grade.of(3))).isEqualTo(2);
    assertThat(FsrsStrategy.rating(Grade.of(4))).isEqualTo(3);
    assertThat(FsrsStrategy.rating(Grade.of(5))).isEqualTo(4);
    assertThat(fsrs.name()).isEqualTo("fsrs");
  }
}
