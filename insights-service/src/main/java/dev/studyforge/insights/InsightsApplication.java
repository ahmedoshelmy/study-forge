package dev.studyforge.insights;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Consumes learning events into read models and serves the dashboard (REST + SSE). */
@SpringBootApplication
public class InsightsApplication {

  public static void main(String[] args) {
    SpringApplication.run(InsightsApplication.class, args);
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
