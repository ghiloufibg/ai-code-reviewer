package com.ghiloufi.aicode.core.domain.model;

import java.time.Duration;
import java.util.Objects;

public record AgentConfiguration(
    TestExecutionConfig testExecution, DockerConfig docker, AggregationConfig aggregation) {

  public AgentConfiguration {
    Objects.requireNonNull(testExecution, "testExecution must not be null");
    Objects.requireNonNull(docker, "docker must not be null");
    Objects.requireNonNull(aggregation, "aggregation must not be null");
  }

  public static AgentConfiguration defaults() {
    return new AgentConfiguration(
        TestExecutionConfig.disabled(), DockerConfig.defaults(), AggregationConfig.defaults());
  }

  public static AgentConfiguration withTestsEnabled() {
    return new AgentConfiguration(
        TestExecutionConfig.defaults(), DockerConfig.defaults(), AggregationConfig.defaults());
  }

  public record TestExecutionConfig(
      boolean enabled, boolean autoDetect, String command, Duration timeout) {

    public TestExecutionConfig {
      Objects.requireNonNull(timeout, "timeout must not be null");
    }

    public static TestExecutionConfig defaults() {
      return new TestExecutionConfig(true, true, null, Duration.ofMinutes(10));
    }

    public static TestExecutionConfig withCommand(final String command) {
      return new TestExecutionConfig(true, false, command, Duration.ofMinutes(10));
    }

    public static TestExecutionConfig disabled() {
      return new TestExecutionConfig(false, false, null, Duration.ZERO);
    }
  }

  public record DockerConfig(
      String image, long memoryLimitBytes, double cpuLimit, Duration timeout, boolean autoRemove) {

    public DockerConfig {
      Objects.requireNonNull(image, "image must not be null");
      Objects.requireNonNull(timeout, "timeout must not be null");
      if (memoryLimitBytes <= 0) {
        throw new IllegalArgumentException("memoryLimitBytes must be positive");
      }
      if (cpuLimit <= 0) {
        throw new IllegalArgumentException("cpuLimit must be positive");
      }
    }

    public static DockerConfig defaults() {
      return new DockerConfig(
          "ai-code-reviewer-analysis:latest",
          2L * 1024 * 1024 * 1024, // 2GB
          2.0,
          Duration.ofMinutes(10),
          true);
    }
  }

  public record AggregationConfig(
      boolean deduplicationEnabled,
      double similarityThreshold,
      double minConfidence,
      int maxIssuesPerFile) {

    public AggregationConfig {
      if (similarityThreshold < 0 || similarityThreshold > 1) {
        throw new IllegalArgumentException("similarityThreshold must be between 0 and 1");
      }
      if (minConfidence < 0 || minConfidence > 1) {
        throw new IllegalArgumentException("minConfidence must be between 0 and 1");
      }
      if (maxIssuesPerFile <= 0) {
        throw new IllegalArgumentException("maxIssuesPerFile must be positive");
      }
    }

    public static AggregationConfig defaults() {
      return new AggregationConfig(true, 0.85, 0.7, 10);
    }
  }
}
