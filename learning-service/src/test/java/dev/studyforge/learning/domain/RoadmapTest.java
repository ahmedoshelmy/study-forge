package dev.studyforge.learning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoadmapTest {

  private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

  private static Roadmap kafkaRoadmap() {
    return new Roadmap(
        UUID.randomUUID(),
        "learner-1",
        ZoneId.of("Africa/Cairo"),
        "Kafka in a month",
        List.of(
            new Stage(
                UUID.randomUUID(),
                "Basics",
                List.of(
                    RoadmapItem.planned("Topics and partitions", 30),
                    RoadmapItem.planned("Consumer groups", 45))),
            new Stage(
                UUID.randomUUID(),
                "Reliability",
                List.of(
                    RoadmapItem.planned("Idempotent producer", 30),
                    RoadmapItem.planned("Retry topics and DLT", 60)))),
        NOW);
  }

  @Test
  void progressCountsDoneItemsAcrossStages() {
    var roadmap = kafkaRoadmap();
    assertThat(roadmap.progressPercent()).isZero();

    var first = roadmap.stages().get(0).items().get(0).id();
    var third = roadmap.stages().get(1).items().get(0).id();
    var after = roadmap.complete(first, NOW).roadmap().complete(third, NOW).roadmap();

    assertThat(after.progressPercent()).isEqualTo(50);
  }

  @Test
  void completingTwiceIsANoOp() {
    var roadmap = kafkaRoadmap();
    var item = roadmap.stages().get(0).items().get(1).id();
    var once = roadmap.complete(item, NOW);
    assertThat(once).isInstanceOf(ItemCompletion.Completed.class);

    var twice = once.roadmap().complete(item, NOW.plusSeconds(60));
    assertThat(twice).isInstanceOf(ItemCompletion.AlreadyDone.class);
    assertThat(twice.roadmap()).isEqualTo(once.roadmap());
  }

  @Test
  void unknownItemsAreNotFound() {
    assertThatThrownBy(() -> kafkaRoadmap().complete(UUID.randomUUID(), NOW))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void aCardReviewUsesTheLearnersLocalDate() {
    var cairo = ZoneId.of("Africa/Cairo");
    var card =
        new Card(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            "learner-1",
            cairo,
            "What keeps one learner's events in order?",
            "The record key",
            CardSchedule.fresh(LocalDate.of(2026, 3, 1)));
    // 23:30 UTC on 1 March is already 2 March in Cairo, so the card is due on 3 March local time.
    var review =
        card.review(Grade.of(5), 20, Instant.parse("2026-03-01T23:30:00Z"), new Sm2Strategy());
    assertThat(review.card().schedule().dueOn()).isEqualTo(LocalDate.of(2026, 3, 3));
  }
}
