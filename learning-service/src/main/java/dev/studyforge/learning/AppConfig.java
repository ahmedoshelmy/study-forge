package dev.studyforge.learning;

import dev.studyforge.learning.domain.SchedulingStrategy;
import dev.studyforge.learning.domain.Sm2Strategy;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the pure domain into Spring: the clock and the scheduling strategy are swappable. */
@Configuration(proxyBeanMethods = false)
class AppConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  SchedulingStrategy schedulingStrategy() {
    return new Sm2Strategy();
  }
}
