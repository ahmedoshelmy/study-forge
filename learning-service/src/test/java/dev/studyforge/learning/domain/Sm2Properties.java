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

/** SM-2 invariants over random review histories (states reachable from a fresh card). */
class Sm2Properties {

  private static final LocalDate START = LocalDate.of(2026, 1, 1);
  private final Sm2Strategy sm2 = new Sm2Strategy();

  @Property
  void aPassingGradeNeverShortensTheInterval(
      @ForAll("histories") List<Integer> history, @ForAll @IntRange(min = 3, max = 5) int grade) {
    var state = replay(history);
    var next = sm2.next(state, Grade.of(grade), state.dueOn());
    assertThat(next.intervalDays()).isGreaterThanOrEqualTo(state.intervalDays());
  }

  @Property
  void easeNeverFallsBelowTheFloor(@ForAll("histories") List<Integer> history) {
    assertThat(replay(history).easeFactor()).isGreaterThanOrEqualTo(CardSchedule.MIN_EASE);
  }

  @Property
  void theNextReviewIsAlwaysInTheFuture(
      @ForAll("histories") List<Integer> history, @ForAll @IntRange(min = 0, max = 5) int grade) {
    var state = replay(history);
    var next = sm2.next(state, Grade.of(grade), state.dueOn());
    assertThat(next.dueOn()).isAfter(state.dueOn());
    assertThat(next.dueOn()).isEqualTo(state.dueOn().plusDays(next.intervalDays()));
  }

  @Property
  void aFailingGradeAlwaysResetsTheCard(
      @ForAll("histories") List<Integer> history, @ForAll @IntRange(min = 0, max = 2) int grade) {
    var next = sm2.next(replay(history), Grade.of(grade), START);
    assertThat(next.repetitions()).isZero();
    assertThat(next.intervalDays()).isEqualTo(1);
  }

  @Provide
  Arbitrary<List<Integer>> histories() {
    return Arbitraries.integers().between(0, 5).list().ofMaxSize(30);
  }

  private CardSchedule replay(List<Integer> grades) {
    var state = CardSchedule.fresh(START);
    for (int g : grades) {
      state = sm2.next(state, Grade.of(g), state.dueOn());
    }
    return state;
  }
}
