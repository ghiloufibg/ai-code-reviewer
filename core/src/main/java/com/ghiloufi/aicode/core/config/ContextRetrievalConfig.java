package com.ghiloufi.aicode.core.config;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "context-retrieval")
public record ContextRetrievalConfig(
    boolean enabled,
    int strategyTimeoutSeconds,
    List<String> enabledStrategies,
    RolloutConfig rollout) {

  public ContextRetrievalConfig {
    Objects.requireNonNull(enabledStrategies, "Enabled strategies cannot be null");
    Objects.requireNonNull(rollout, "Rollout config cannot be null");

    if (strategyTimeoutSeconds <= 0) {
      throw new IllegalArgumentException("Strategy timeout must be positive");
    }
  }

  public record RolloutConfig(int percentage, boolean skipLargeDiffs, int maxDiffLines) {

    public RolloutConfig {
      if (percentage < 0 || percentage > 100) {
        throw new IllegalArgumentException("Percentage must be between 0 and 100");
      }

      if (maxDiffLines <= 0) {
        throw new IllegalArgumentException("Max diff lines must be positive");
      }
    }
  }

  public static ContextRetrievalConfig defaults() {
    return new ContextRetrievalConfig(
        true, 5, List.of("metadata-based", "git-history"), new RolloutConfig(100, true, 5000));
  }

  public boolean isStrategyEnabled(final String strategyName) {
    return enabled && enabledStrategies.contains(strategyName);
  }
}
