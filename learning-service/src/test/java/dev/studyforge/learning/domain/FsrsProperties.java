package dev.studyforge.learning.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

/** FSRS invariants over random review histories (each review on its due date or later). */
class FsrsProperties {

  private static final LocalDate START = LocalDate.of(2026, 1, 1);
  private final FsrsStrategy fsrs = new FsrsStrategy();

  @Property
  void memoryStateStaysInRange(@ForAll("histories") List<Integer> history) {
    var state = replay(history);
    if (!history.isEmpty()) {
      assertThat(state.stability()).isPositive();
      assertThat(state.difficulty()).isBetween(1.0, 10.0);
      assertThat(state.intervalDays()).isBetween(1, FsrsStrategy.MAX_INTERVAL_DAYS);
    }
  }

  @Property
  void recallingADueCardNeverLowersStability(
      @ForAll("histories") List<Integer> history, @ForAll @IntRange(min = 3, max = 5) int grade) {
    var state = replay(history);
    var next = fsrs.next(state, Grade.of(grade), state.dueOn());
    assertThat(next.stability()).isGreaterThanOrEqualTo(state.stability());
  }

  @Property
  void forgettingNeverRaisesStability(
      @ForAll("histories") List<Integer> history, @ForAll @IntRange(min = 0, max = 2) int grade) {
    var state = replay(history);
    if (state.hasMemoryState()) {
      var next = fsrs.next(state, Grade.of(grade), state.dueOn());
      assertThat(next.stability()).isLessThanOrEqualTo(state.stability());
      assertThat(next.repetitions()).isZero();
    }
  }

  @Property
  void theNextReviewIsAlwaysAfterTheReviewDay(
      @ForAll("histories") List<Integer> history,
      @ForAll @IntRange(min = 0, max = 5) int grade,
      @ForAll @IntRange(min = 0, max = 60) int daysLate) {
    var state = replay(history);
    var reviewedOn = state.dueOn().plusDays(daysLate);
    var next = fsrs.next(state, Grade.of(grade), reviewedOn);
    assertThat(next.dueOn()).isAfter(reviewedOn);
    assertThat(next.dueOn()).isEqualTo(reviewedOn.plusDays(next.intervalDays()));
  }

  @Provide
  Arbitrary<List<Integer>> histories() {
    return Arbitraries.integers().between(0, 5).list().ofMaxSize(30);
  }

  private CardSchedule replay(List<Integer> grades) {
    var state = CardSchedule.fresh(START);
    for (int g : grades) {
      state = fsrs.next(state, Grade.of(g), state.dueOn());
    }
    return state;
  }
}
