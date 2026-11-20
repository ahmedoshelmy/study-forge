package dev.studyforge.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AppConfigTest {

  @Test
  void schedulerIsChosenByName() {
    assertThat(AppConfig.strategyNamed("sm2").name()).isEqualTo("sm2");
    assertThat(AppConfig.strategyNamed(" FSRS ").name()).isEqualTo("fsrs");
  }

  @Test
  void unknownSchedulerFailsStartup() {
    assertThatThrownBy(() -> AppConfig.strategyNamed("leitner"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sm2 or fsrs");
  }
}
