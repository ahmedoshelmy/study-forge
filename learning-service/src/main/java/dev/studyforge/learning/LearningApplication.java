package dev.studyforge.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Roadmaps, cards and review scheduling. Publishes ReviewRecorded and ItemCompleted to Kafka. */
@SpringBootApplication
public class LearningApplication {

  public static void main(String[] args) {
    SpringApplication.run(LearningApplication.class, args);
  }
}
