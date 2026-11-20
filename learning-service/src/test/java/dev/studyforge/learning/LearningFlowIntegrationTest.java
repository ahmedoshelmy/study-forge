package dev.studyforge.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.studyforge.contracts.Topics;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * HTTP in, Postgres + Kafka out (Testcontainers): a review is saved, scheduled by SM-2 and leaves
 * through the outbox as a ReviewRecorded keyed by learner; completing an item twice publishes one
 * ItemCompleted.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LearningFlowIntegrationTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvc mvc;

  @Autowired ConsumerFactory<String, String> consumers;

  @Test
  void reviewsAndCompletionsAreScheduledPersistedAndPublished() throws Exception {
    String learner = "learner-" + UUID.randomUUID();
    var roadmap =
        send(
            post("/api/roadmaps"),
            """
            {"learnerId": "%s", "zoneId": "Africa/Cairo", "title": "Event streaming",
             "stages": [{"title": "Basics", "items": [{"title": "Partitions", "minutes": 30},
                                                      {"title": "Consumer groups", "minutes": 45}]}]}
            """
                .formatted(learner),
            status().isCreated());
    String roadmapId = roadmap.get("id").asString();
    String itemId = roadmap.get("stages").get(0).get("items").get(0).get("id").asString();

    var card =
        send(
            post("/api/roadmaps/" + roadmapId + "/cards"),
            """
            {"itemId": "%s", "front": "What orders one learner's events?", "back": "The key"}
            """
                .formatted(itemId),
            status().isCreated());
    String cardId = card.get("id").asString();

    var due = send(get("/api/learners/" + learner + "/due"), null, status().isOk());
    assertThat(due).hasSize(1);

    var review =
        send(
            post("/api/cards/" + cardId + "/reviews"),
            "{\"grade\": 4, \"durationSeconds\": 12}",
            status().isOk());
    assertThat(review.get("card").get("intervalDays").asInt()).isEqualTo(1);
    assertThat(send(get("/api/learners/" + learner + "/due"), null, status().isOk())).isEmpty();

    send(
        post("/api/cards/" + cardId + "/reviews"),
        "{\"grade\": 7, \"durationSeconds\": 12}",
        status().isBadRequest());

    var completed =
        send(
            post("/api/roadmaps/" + roadmapId + "/items/" + itemId + "/complete"),
            null,
            status().isOk());
    assertThat(completed.get("progressPercent").asInt()).isEqualTo(50);
    send(
        post("/api/roadmaps/" + roadmapId + "/items/" + itemId + "/complete"),
        null,
        status().isOk());

    var reviews = drain(Topics.REVIEW_EVENTS, learner, 1);
    assertThat(reviews).hasSize(1);
    var event = JSON.readTree(reviews.getFirst().value());
    assertThat(event.get("cardId").asString()).isEqualTo(cardId);
    assertThat(event.get("grade").asInt()).isEqualTo(4);
    assertThat(event.get("zoneId").asString()).isEqualTo("Africa/Cairo");
    assertThat(event.get("reviewedAt").isString()).isTrue();

    var progress = drain(Topics.PROGRESS_EVENTS, learner, 1);
    assertThat(progress).hasSize(1);
    assertThat(JSON.readTree(progress.getFirst().value()).get("progressPercent").asInt())
        .isEqualTo(50);
  }

  @Test
  void unknownRoadmapsAreProblemDetails() throws Exception {
    var problem = send(get("/api/roadmaps/" + UUID.randomUUID()), null, status().isNotFound());
    assertThat(problem.get("status").asInt()).isEqualTo(404);
  }

  /** Reads the topic from the start for ~8 s (or until enough records) and keeps the learner's. */
  private List<ConsumerRecord<String, String>> drain(String topic, String learner, int expected) {
    var overrides = new Properties();
    overrides.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    overrides.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    overrides.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    var found = new ArrayList<ConsumerRecord<String, String>>();
    try (var consumer =
        consumers.createConsumer("test-" + UUID.randomUUID(), null, null, overrides)) {
      consumer.subscribe(List.of(topic));
      long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
      while (System.nanoTime() < deadline) {
        for (var record : consumer.poll(Duration.ofMillis(250))) {
          if (learner.equals(record.key())) {
            found.add(record);
          }
        }
        if (found.size() > expected) {
          break;
        }
      }
    }
    return found;
  }

  private JsonNode send(MockHttpServletRequestBuilder request, String body, ResultMatcher expect)
      throws Exception {
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    var response = mvc.perform(request).andExpect(expect).andReturn().getResponse();
    var text = response.getContentAsString();
    return text.isEmpty() ? JSON.nullNode() : JSON.readTree(text);
  }
}
