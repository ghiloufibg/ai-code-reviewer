package com.ghiloufi.aicode.infrastructure.testcontainer;

import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainer configuration for Ollama LLM integration testing.
 *
 * <p>Provides embedded LLM functionality for true integration tests without depending on external
 * LLM services.
 */
@TestConfiguration
@Profile("integration-test")
@Slf4j
public class OllamaTestContainer {

  private static final String OLLAMA_IMAGE = "ollama/ollama:0.1.48";
  private static final String MODEL_NAME = "codellama:7b-code";

  @Bean
  public GenericContainer<?> ollamaContainer() {
    GenericContainer<?> container =
        new GenericContainer<>(DockerImageName.parse(OLLAMA_IMAGE))
            .withExposedPorts(11434)
            .withReuse(true) // Reuse container across tests for performance
            .withStartupTimeout(Duration.ofMinutes(5));

    container.start();

    // Pull the model after container starts
    try {
      log.info("Pulling model {} in Ollama container...", MODEL_NAME);
      container.execInContainer("ollama", "pull", MODEL_NAME);
      log.info("Model {} successfully pulled", MODEL_NAME);
    } catch (IOException | InterruptedException e) {
      log.error("Failed to pull model {}", MODEL_NAME, e);
      throw new RuntimeException("Failed to initialize Ollama test container", e);
    }

    return container;
  }

  @Bean
  @Profile("integration-test")
  public String ollamaBaseUrl(GenericContainer<?> ollamaContainer) {
    return "http://" + ollamaContainer.getHost() + ":" + ollamaContainer.getFirstMappedPort();
  }

  @Bean
  @Profile("integration-test")
  public String ollamaModelName() {
    return MODEL_NAME;
  }
}
