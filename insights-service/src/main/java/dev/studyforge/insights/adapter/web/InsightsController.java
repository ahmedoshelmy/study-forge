package dev.studyforge.insights.adapter.web;

import dev.studyforge.insights.application.ReadModels;
import dev.studyforge.insights.application.StatsQueries;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Read side REST + SSE. OpenAPI at /v3/api-docs. */
@RestController
@RequestMapping("/api/insights")
class InsightsController {

  private final StatsQueries queries;
  private final LiveStats live;

  InsightsController(StatsQueries queries, LiveStats live) {
    this.queries = queries;
    this.live = live;
  }

  @GetMapping("/learners/{learnerId}/stats")
  StatsView stats(@PathVariable String learnerId) {
    return StatsView.of(queries.stats(learnerId));
  }

  @GetMapping(path = "/learners/{learnerId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter stream(@PathVariable String learnerId) {
    return live.subscribe(learnerId);
  }

  @GetMapping("/dead-letters")
  List<ReadModels.DeadLetter> deadLetters() {
    return queries.deadLetters();
  }
}
