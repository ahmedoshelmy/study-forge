package dev.studyforge.insights;

import java.util.Arrays;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

/** Throwaway Postgres and Redpanda (Kafka API) per test context; needs Docker or Podman. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  static final String REDPANDA_IMAGE = "redpandadata/redpanda:v24.3.6";

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
  }

  @Bean
  @ServiceConnection
  RedpandaContainer redpandaContainer() {
    return new SmallRedpanda();
  }

  /** The module asks Seastar for 1 GB; half of that is plenty for a test broker. */
  static final class SmallRedpanda extends RedpandaContainer {
    SmallRedpanda() {
      super(REDPANDA_IMAGE);
    }

    @Override
    protected void configure() {
      super.configure();
      setCommand(
          Arrays.stream(getCommandParts())
              .map(part -> part.startsWith("--memory=") ? "--memory=512M" : part)
              .toArray(String[]::new));
    }
  }
}
