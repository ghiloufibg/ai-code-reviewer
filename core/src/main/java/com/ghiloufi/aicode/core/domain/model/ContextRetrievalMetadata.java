package com.ghiloufi.aicode.core.domain.model;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record ContextRetrievalMetadata(
    String strategyName,
    Duration executionTime,
    int totalCandidates,
    int highConfidenceCount,
    Map<MatchReason, Integer> reasonDistribution) {

  public ContextRetrievalMetadata {
    Objects.requireNonNull(strategyName, "Strategy name cannot be null");
    Objects.requireNonNull(executionTime, "Execution time cannot be null");
    Objects.requireNonNull(reasonDistribution, "Reason distribution cannot be null");

    if (strategyName.isBlank()) {
      throw new IllegalArgumentException("Strategy name cannot be blank");
    }

    if (totalCandidates < 0) {
      throw new IllegalArgumentException(
          "Total candidates cannot be negative, got: " + totalCandidates);
    }

    if (highConfidenceCount < 0) {
      throw new IllegalArgumentException(
          "High confidence count cannot be negative, got: " + highConfidenceCount);
    }

    if (highConfidenceCount > totalCandidates) {
      throw new IllegalArgumentException(
          "High confidence count cannot exceed total candidates: "
              + highConfidenceCount
              + " > "
              + totalCandidates);
    }
  }

  public double getHighConfidencePercentage() {
    if (totalCandidates == 0) {
      return 0.0;
    }
    return (double) highConfidenceCount / totalCandidates * 100.0;
  }
}
