package dev.studyforge.learning.application;

import dev.studyforge.learning.domain.Roadmap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port: roadmap persistence. */
public interface Roadmaps {

  Optional<Roadmap> find(UUID id);

  List<Roadmap> ofLearner(String learnerId);

  Roadmap save(Roadmap roadmap);
}
