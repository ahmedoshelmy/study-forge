package dev.studyforge.insights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.IntStream;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

class StreakProperties {

  private static final LocalDate TODAY = LocalDate.of(2026, 6, 30);

  @Property
  void currentNeverExceedsLongestAndLongestNeverExceedsActiveDays(
      @ForAll @Size(max = 60) Set<@IntRange(min = 0, max = 90) Integer> daysAgo) {
    var days = daysAgo.stream().map(TODAY::minusDays).toList();
    var streak = Streak.of(days, TODAY);
    assertThat(streak.current()).isLessThanOrEqualTo(streak.longest());
    assertThat(streak.longest()).isLessThanOrEqualTo(days.size());
  }

  @Property
  void studyingTodayExtendsYesterdaysStreakByOne(@ForAll @IntRange(min = 1, max = 30) int run) {
    var before = IntStream.rangeClosed(1, run).mapToObj(TODAY::minusDays).toList();
    var after = new ArrayList<>(before);
    after.add(TODAY);
    assertThat(Streak.of(after, TODAY).current()).isEqualTo(Streak.of(before, TODAY).current() + 1);
  }
}
