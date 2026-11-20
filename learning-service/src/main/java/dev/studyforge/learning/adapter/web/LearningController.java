package dev.studyforge.learning.adapter.web;

import dev.studyforge.learning.adapter.web.Dtos.CardView;
import dev.studyforge.learning.adapter.web.Dtos.CreateCard;
import dev.studyforge.learning.adapter.web.Dtos.CreateRoadmap;
import dev.studyforge.learning.adapter.web.Dtos.RecordReview;
import dev.studyforge.learning.adapter.web.Dtos.ReviewView;
import dev.studyforge.learning.adapter.web.Dtos.RoadmapView;
import dev.studyforge.learning.application.ReviewService;
import dev.studyforge.learning.application.RoadmapService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST API of learning-service. OpenAPI at /v3/api-docs, UI at /swagger-ui.html. */
@RestController
@RequestMapping("/api")
class LearningController {

  private final RoadmapService roadmaps;
  private final ReviewService reviews;

  LearningController(RoadmapService roadmaps, ReviewService reviews) {
    this.roadmaps = roadmaps;
    this.reviews = reviews;
  }

  @PostMapping("/roadmaps")
  ResponseEntity<RoadmapView> create(@Valid @RequestBody CreateRoadmap body) {
    var created = RoadmapView.of(roadmaps.create(body.toInput()));
    return ResponseEntity.created(URI.create("/api/roadmaps/" + created.id())).body(created);
  }

  @GetMapping("/roadmaps/{id}")
  RoadmapView get(@PathVariable UUID id) {
    return RoadmapView.of(roadmaps.get(id));
  }

  @GetMapping("/learners/{learnerId}/roadmaps")
  List<RoadmapView> ofLearner(@PathVariable String learnerId) {
    return roadmaps.ofLearner(learnerId).stream().map(RoadmapView::of).toList();
  }

  @PostMapping("/roadmaps/{id}/items/{itemId}/complete")
  RoadmapView complete(@PathVariable UUID id, @PathVariable UUID itemId) {
    return RoadmapView.of(roadmaps.completeItem(id, itemId));
  }

  @PostMapping("/roadmaps/{id}/cards")
  ResponseEntity<CardView> addCard(@PathVariable UUID id, @Valid @RequestBody CreateCard body) {
    var card = CardView.of(reviews.addCard(id, body.itemId(), body.front(), body.back()));
    return ResponseEntity.created(URI.create("/api/cards/" + card.id())).body(card);
  }

  @GetMapping("/roadmaps/{id}/cards")
  List<CardView> cards(@PathVariable UUID id) {
    return reviews.ofRoadmap(id).stream().map(CardView::of).toList();
  }

  @GetMapping("/learners/{learnerId}/due")
  List<CardView> due(@PathVariable String learnerId) {
    return reviews.dueToday(learnerId).stream().map(CardView::of).toList();
  }

  @PostMapping("/cards/{id}/reviews")
  ReviewView review(@PathVariable UUID id, @Valid @RequestBody RecordReview body) {
    return ReviewView.of(reviews.record(id, body.grade(), body.durationSeconds()));
  }
}
