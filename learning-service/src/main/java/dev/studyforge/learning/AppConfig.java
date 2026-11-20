package dev.studyforge.learning;

import dev.studyforge.learning.domain.FsrsStrategy;
import dev.studyforge.learning.domain.SchedulingStrategy;
import dev.studyforge.learning.domain.Sm2Strategy;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the pure domain into Spring: the clock and the scheduling strategy are swappable. */
@Configuration(proxyBeanMethods = false)
class AppConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  /** {@code studyforge.scheduler=sm2} (default) or {@code fsrs}; anything else fails startup. */
  @Bean
  SchedulingStrategy schedulingStrategy(@Value("${studyforge.scheduler:sm2}") String scheduler) {
    return strategyNamed(scheduler);
  }

  static SchedulingStrategy strategyNamed(String scheduler) {
    return switch (scheduler.trim().toLowerCase(java.util.Locale.ROOT)) {
      case "sm2" -> new Sm2Strategy();
      case "fsrs" -> new FsrsStrategy();
      default ->
          throw new IllegalArgumentException(
              "studyforge.scheduler must be sm2 or fsrs, got '" + scheduler + "'");
    };
  }
}
