package dev.studyforge.insights.adapter.messaging;

import dev.studyforge.contracts.ItemCompleted;
import dev.studyforge.contracts.ReviewRecorded;
import dev.studyforge.contracts.Topics;
import dev.studyforge.insights.application.InvalidEventException;
import dev.studyforge.insights.application.Projector;
import dev.studyforge.insights.application.ReadModels;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kafka consumers (group {@code insights}). Transient failures (database down) are retried on
 * delayed retry topics without blocking the partition; messages that can never succeed (bad JSON,
 * missing ids) skip the retries. Both end on the {@code -dlt} topic, parked in {@code dead_letter}.
 */
@Component
class EventListeners {

  private static final Logger log = LoggerFactory.getLogger(EventListeners.class);

  /** Tolerant reader: unknown fields are ignored so producers can add fields (additive schema). */
  private static final JsonMapper JSON =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private final Projector projector;
  private final ReadModels models;

  EventListeners(Projector projector, ReadModels models) {
    this.projector = projector;
    this.models = models;
  }

  @RetryableTopic(
      attempts = "${insights.retry.attempts:4}",
      backOff = @BackOff(delayString = "${insights.retry.delay-ms:1000}", multiplier = 2.0),
      exclude = InvalidEventException.class,
      dltStrategy = DltStrategy.FAIL_ON_ERROR,
      numPartitions = "3",
      replicationFactor = "1")
  @KafkaListener(topics = Topics.REVIEW_EVENTS, groupId = "insights", concurrency = "3")
  void onReview(ConsumerRecord<String, String> record) {
    boolean applied = projector.apply(parse(record.value(), ReviewRecorded.class));
    log.debug("review event at offset {} applied={}", record.offset(), applied);
  }

  @RetryableTopic(
      attempts = "${insights.retry.attempts:4}",
      backOff = @BackOff(delayString = "${insights.retry.delay-ms:1000}", multiplier = 2.0),
      exclude = InvalidEventException.class,
      dltStrategy = DltStrategy.FAIL_ON_ERROR,
      numPartitions = "3",
      replicationFactor = "1")
  @KafkaListener(topics = Topics.PROGRESS_EVENTS, groupId = "insights", concurrency = "3")
  void onProgress(ConsumerRecord<String, String> record) {
    boolean applied = projector.apply(parse(record.value(), ItemCompleted.class));
    log.debug("progress event at offset {} applied={}", record.offset(), applied);
  }

  @DltHandler
  void onDeadLetter(
      ConsumerRecord<String, String> record,
      @Header(name = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String error) {
    log.warn("parking message from {} key {}: {}", record.topic(), record.key(), error);
    models.deadLetter(record.topic(), record.key(), record.value(), error);
  }

  static <T> T parse(String json, Class<T> type) {
    if (json == null || json.isBlank()) {
      throw new InvalidEventException("empty " + type.getSimpleName() + " message", null);
    }
    try {
      return JSON.readValue(json, type);
    } catch (JacksonException e) {
      throw new InvalidEventException("unreadable " + type.getSimpleName(), e);
    }
  }
}
