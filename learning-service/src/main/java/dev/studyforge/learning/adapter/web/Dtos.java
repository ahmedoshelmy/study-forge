package dev.studyforge.learning.adapter.web;

import dev.studyforge.learning.application.RoadmapService;
import dev.studyforge.learning.domain.Card;
import dev.studyforge.learning.domain.Roadmap;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** HTTP request and response shapes, kept apart from the domain records. */
final class Dtos {

  private Dtos() {}

  record CreateRoadmap(
      @NotBlank @Size(max = 64) String learnerId,
      @Size(max = 64) String zoneId,
      @NotBlank @Size(max = 200) String title,
      @NotEmpty @Size(max = 50) List<@Valid CreateStage> stages) {

    RoadmapService.NewRoadmap toInput() {
      return new RoadmapService.NewRoadmap(
          learnerId,
          zoneId,
          title,
          stages.stream()
              .map(s ->
                  new RoadmapService.NewStage(
                      s.title(),
                      s.items().stream()
                          .map(i -> new RoadmapService.NewItem(i.title(), i.minutes()))
                          .toList()))
              .toList());
    }
  }

  record CreateStage(
      @NotBlank @Size(max = 200) String title, @Size(max = 200) List<@Valid CreateItem> items) {
    CreateStage {
      items = items == null ? List.of() : items;
    }
  }

  record CreateItem(@NotBlank @Size(max = 200) String title, @Min(0) @Max(10_000) int minutes) {}

  record CreateCard(
      UUID itemId,
      @NotBlank @Size(max = 500) String front,
      @NotBlank @Size(max = 2000) String back) {}

  record RecordReview(@Min(0) @Max(5) int grade, @Min(0) @Max(3600) int durationSeconds) {}

  record RoadmapView(
      UUID id,
      String learnerId,
      String zoneId,
      String title,
      int progressPercent,
      List<StageView> stages) {

    static RoadmapView of(Roadmap r) {
      return new RoadmapView(
          r.id(),
          r.learnerId(),
          r.zone().getId(),
          r.title(),
          r.progressPercent(),
          r.stages().stream()
              .map(s ->
                  new StageView(
                      s.id(),
                      s.title(),
                      s.items().stream()
                          .map(i ->
                              new ItemView(
                                  i.id(), i.title(), i.estimatedMinutes(), i.completedAt()))
                          .toList()))
              .toList());
    }
  }

  record StageView(UUID id, String title, List<ItemView> items) {}

  record ItemView(UUID id, String title, int minutes, Instant completedAt) {}

  record CardView(
      UUID id,
      UUID roadmapId,
      UUID itemId,
      String front,
      String back,
      int repetitions,
      double easeFactor,
      int intervalDays,
      LocalDate dueOn) {

    static CardView of(Card c) {
      var s = c.schedule();
      return new CardView(
          c.id(),
          c.roadmapId(),
          c.itemId(),
          c.front(),
          c.back(),
          s.repetitions(),
          s.easeFactor(),
          s.intervalDays(),
          s.dueOn());
    }
  }

  record ReviewView(UUID reviewId, int grade, CardView card) {

    static ReviewView of(Card.Review r) {
      return new ReviewView(r.id(), r.grade().value(), CardView.of(r.card()));
    }
  }
}
