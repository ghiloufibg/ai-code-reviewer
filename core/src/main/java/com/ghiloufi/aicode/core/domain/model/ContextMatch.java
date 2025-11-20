package com.ghiloufi.aicode.core.domain.model;

import java.util.Objects;

public record ContextMatch(
    String filePath, MatchReason reason, double confidence, String evidence) {

  public ContextMatch {
    Objects.requireNonNull(filePath, "File path cannot be null");
    Objects.requireNonNull(reason, "Reason cannot be null");
    Objects.requireNonNull(evidence, "Evidence cannot be null");

    if (filePath.isBlank()) {
      throw new IllegalArgumentException("File path cannot be blank");
    }

    if (confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException(
          "Confidence must be between 0.0 and 1.0, got: " + confidence);
    }
  }

  public boolean isHighConfidence() {
    return confidence >= 0.75;
  }
}
