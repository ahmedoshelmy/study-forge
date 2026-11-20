package dev.studyforge.learning.adapter.messaging;

import dev.studyforge.contracts.ItemCompleted;
import dev.studyforge.contracts.ReviewRecorded;
import dev.studyforge.contracts.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;

/**
 * Domain events leave the service through Spring Modulith's event publication registry: the event
 * row is written in the same transaction as the card/roadmap change (transactional outbox), then
 * sent to Kafka after commit and marked complete. Unsent rows are resubmitted on restart, so
 * delivery is at least once; consumers de-duplicate by {@code eventId}.
 */
@Configuration(proxyBeanMethods = false)
class EventExternalization {

  static final int PARTITIONS = 3;

  @Bean
  EventExternalizationConfiguration eventExternalizationConfiguration() {
    return EventExternalizationConfiguration.externalizing()
        .selectByType(type -> type == ReviewRecorded.class || type == ItemCompleted.class)
        .route(
            ReviewRecorded.class,
            e -> RoutingTarget.forTarget(Topics.REVIEW_EVENTS).andKey(e.learnerId()))
        .route(
            ItemCompleted.class,
            e -> RoutingTarget.forTarget(Topics.PROGRESS_EVENTS).andKey(e.learnerId()))
        .build();
  }

  @Bean
  NewTopic reviewEventsTopic() {
    return TopicBuilder.name(Topics.REVIEW_EVENTS).partitions(PARTITIONS).replicas(1).build();
  }

  @Bean
  NewTopic progressEventsTopic() {
    return TopicBuilder.name(Topics.PROGRESS_EVENTS).partitions(PARTITIONS).replicas(1).build();
  }
}
