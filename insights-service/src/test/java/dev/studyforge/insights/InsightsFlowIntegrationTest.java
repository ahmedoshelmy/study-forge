package dev.studyforge.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.studyforge.contracts.Topics;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Kafka in, read model out (Testcontainers Postgres + Redpanda). The payloads start from the
 * producer's published examples in the contracts module, so this doubles as the consumer side of
 * the contract test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class InsightsFlowIntegrationTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Duration WAIT = Duration.ofSeconds(30);

  @Autowired MockMvc mvc;
  @Autowired KafkaTemplate<String, String> kafka;

  @Test
  void eventsBuildTheReadModelAndADuplicateIsAppliedOnce() throws Exception {
    String learner = "learner-" + UUID.randomUUID();
    var first = example("review-recorded", learner);
    var second = example("review-recorded", learner);
    second
        .put("previousIntervalDays", 6)
        .put("grade", 2)
        .put("cardId", UUID.randomUUID().toString());
    var completed = example("item-completed", learner);

    send(Topics.REVIEW_EVENTS, learner, first);
    send(Topics.REVIEW_EVENTS, learner, first);
    send(Topics.REVIEW_EVENTS, learner, second);
    send(Topics.PROGRESS_EVENTS, learner, completed);

    await()
        .atMost(WAIT)
        .untilAsserted(() -> {
          var stats = stats(learner);
          assertThat(stats.get("totalReviews").asInt()).isEqualTo(2);
          assertThat(stats.get("itemsCompleted").asInt()).isEqualTo(1);
        });

    var stats = stats(learner);
    var retention = stats.get("retention");
    assertThat(retention.get(1).get("percent").asInt()).as("1-day bucket").isEqualTo(100);
    assertThat(retention.get(2).get("percent").asInt()).as("2-6 day bucket").isZero();
    assertThat(stats.get("streak").get("longest").asInt()).isEqualTo(1);
    assertThat(stats.get("forecast")).hasSize(14);
  }

  @Test
  void aPoisonMessageSkipsRetriesAndIsParkedFromTheDeadLetterTopic() throws Exception {
    String learner = "poison-" + UUID.randomUUID();
    kafka.send(Topics.REVIEW_EVENTS, learner, "{not json").get();

    await()
        .atMost(WAIT)
        .untilAsserted(() -> {
          var body =
              mvc.perform(get("/api/insights/dead-letters"))
                  .andExpect(status().isOk())
                  .andReturn()
                  .getResponse()
                  .getContentAsString();
          var parked = JSON.readTree(body);
          assertThat(parked.valueStream().map(n -> n.get("key").asString())).contains(learner);
        });
    assertThat(stats(learner).get("totalReviews").asInt()).isZero();
  }

  private void send(String topic, String key, JsonNode event) throws Exception {
    kafka.send(topic, key, JSON.writeValueAsString(event)).get();
  }

  private JsonNode stats(String learner) throws Exception {
    var body =
        mvc.perform(get("/api/insights/learners/" + learner + "/stats"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JSON.readTree(body);
  }

  /** The producer's example payload with a fresh event id and our learner. */
  private static ObjectNode example(String name, String learner) throws IOException {
    try (InputStream in =
        InsightsFlowIntegrationTest.class.getResourceAsStream(
            "/contracts/" + name + ".example.json")) {
      assertThat(in).as(name).isNotNull();
      var node = (ObjectNode) JSON.readTree(in);
      return node.put("eventId", UUID.randomUUID().toString()).put("learnerId", learner);
    }
  }
}
