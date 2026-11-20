package dev.studyforge.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.studyforge.contracts.ReviewRecorded;
import dev.studyforge.insights.application.InvalidEventException;
import dev.studyforge.insights.application.Projector;
import dev.studyforge.insights.application.ReadModels;
import dev.studyforge.insights.application.StatsChanged;
import dev.studyforge.insights.domain.LearnerStats.BucketCount;
import dev.studyforge.insights.domain.LearnerStats.DailyActivity;
import dev.studyforge.insights.domain.RetentionBucket;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Projection rules without Kafka or Postgres: local day boundaries and once-only application. */
class ProjectorTest {

  private final FakeReadModels models = new FakeReadModels();
  private final List<Object> published = new ArrayList<>();
  private final Projector projector = new Projector(models, published::add);

  private static ReviewRecorded review(UUID id, String zone, Instant at) {
    return new ReviewRecorded(
        id,
        "l1",
        zone,
        UUID.randomUUID(),
        UUID.randomUUID(),
        4,
        1,
        6,
        2.5,
        LocalDate.of(2026, 3, 8),
        30,
        at);
  }

  @Test
  void theReviewDayIsTheLearnersLocalDate() {
    // 22:30 UTC on 1 March is 00:30 on 2 March in Cairo.
    projector.apply(
        review(UUID.randomUUID(), "Africa/Cairo", Instant.parse("2026-03-01T22:30:00Z")));
    assertThat(models.reviewDays).containsExactly(LocalDate.of(2026, 3, 2));
    assertThat(published).containsExactly(new StatsChanged("l1"));
  }

  @Test
  void aDuplicateEventIdIsSkipped() {
    var id = UUID.randomUUID();
    assertThat(projector.apply(review(id, "UTC", Instant.parse("2026-03-01T10:00:00Z")))).isTrue();
    assertThat(projector.apply(review(id, "UTC", Instant.parse("2026-03-01T10:00:00Z")))).isFalse();
    assertThat(models.reviewDays).hasSize(1);
  }

  @Test
  void eventsWithoutIdsAreInvalidNotRetried() {
    assertThatThrownBy(() -> projector.apply(review(null, "UTC", Instant.now())))
        .isInstanceOf(InvalidEventException.class);
    assertThatThrownBy(() -> projector.apply(review(UUID.randomUUID(), "Mars/Base", Instant.now())))
        .isInstanceOf(InvalidEventException.class);
  }

  /** In-memory read models: just enough to observe what the projector wrote. */
  static final class FakeReadModels implements ReadModels {
    final Set<UUID> processed = new HashSet<>();
    final List<LocalDate> reviewDays = new ArrayList<>();

    @Override
    public boolean markProcessed(UUID eventId, String type) {
      return processed.add(eventId);
    }

    @Override
    public void rememberZone(String learnerId, ZoneId zone) {}

    @Override
    public Optional<ZoneId> zoneOf(String learnerId) {
      return Optional.empty();
    }

    @Override
    public void addReview(String learnerId, LocalDate day, boolean recalled, int seconds) {
      reviewDays.add(day);
    }

    @Override
    public void addCompletion(String learnerId, LocalDate day, int minutes) {}

    @Override
    public void countRetention(String learnerId, RetentionBucket bucket, boolean recalled) {}

    @Override
    public void scheduleCard(String learnerId, UUID cardId, LocalDate dueOn) {}

    @Override
    public List<DailyActivity> activity(String learnerId) {
      return List.of();
    }

    @Override
    public List<BucketCount> retention(String learnerId) {
      return List.of();
    }

    @Override
    public Map<LocalDate, Long> dueByDay(String learnerId, LocalDate until) {
      return Map.of();
    }

    @Override
    public void deadLetter(String topic, String key, String payload, String error) {}

    @Override
    public List<DeadLetter> deadLetters(int limit) {
      return List.of();
    }
  }
}
