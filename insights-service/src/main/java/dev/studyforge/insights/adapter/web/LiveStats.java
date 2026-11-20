package dev.studyforge.insights.adapter.web;

import dev.studyforge.insights.application.StatsChanged;
import dev.studyforge.insights.application.StatsQueries;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Server-Sent Events: each open dashboard gets the learner's stats again whenever a projection
 * transaction for that learner commits. In-memory, so it fans out per instance; with several
 * instances each one consumes every partition's changes only for its own partitions, which is why a
 * scaled deployment would broadcast via Redis pub/sub (see README).
 */
@Component
class LiveStats {

  private static final long TIMEOUT = Duration.ofMinutes(30).toMillis();

  private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
  private final StatsQueries queries;

  LiveStats(StatsQueries queries) {
    this.queries = queries;
  }

  SseEmitter subscribe(String learnerId) {
    var emitter = new SseEmitter(TIMEOUT);
    var list = emitters.computeIfAbsent(learnerId, k -> new CopyOnWriteArrayList<>());
    list.add(emitter);
    Runnable remove = () -> list.remove(emitter);
    emitter.onCompletion(remove);
    emitter.onTimeout(remove);
    emitter.onError(e -> remove.run());
    send(emitter, learnerId);
    return emitter;
  }

  @TransactionalEventListener
  void onStatsChanged(StatsChanged changed) {
    var list = emitters.get(changed.learnerId());
    if (list == null || list.isEmpty()) {
      return;
    }
    list.forEach(emitter -> send(emitter, changed.learnerId()));
  }

  private void send(SseEmitter emitter, String learnerId) {
    try {
      emitter.send(SseEmitter.event().name("stats").data(StatsView.of(queries.stats(learnerId))));
    } catch (IOException | IllegalStateException e) {
      emitter.completeWithError(e);
    }
  }
}
