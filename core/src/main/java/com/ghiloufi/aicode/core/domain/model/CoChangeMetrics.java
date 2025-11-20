package com.ghiloufi.aicode.core.domain.model;

import java.util.Objects;

public record CoChangeMetrics(String filePath, int coChangeCount, double normalizedFrequency) {

  private static final double HIGH_FREQUENCY_THRESHOLD = 0.70;
  private static final double MEDIUM_FREQUENCY_THRESHOLD = 0.40;

  public CoChangeMetrics {
    Objects.requireNonNull(filePath, "File path cannot be null");

    if (filePath.isBlank()) {
      throw new IllegalArgumentException("File path cannot be blank");
    }

    if (coChangeCount < 0) {
      throw new IllegalArgumentException("Co-change count cannot be negative");
    }

    if (normalizedFrequency < 0.0 || normalizedFrequency > 1.0) {
      throw new IllegalArgumentException("Normalized frequency must be between 0.0 and 1.0");
    }
  }

  public boolean isHighFrequency() {
    return normalizedFrequency >= HIGH_FREQUENCY_THRESHOLD;
  }

  public boolean isMediumFrequency() {
    return normalizedFrequency >= MEDIUM_FREQUENCY_THRESHOLD
        && normalizedFrequency < HIGH_FREQUENCY_THRESHOLD;
  }

  public MatchReason getMatchReason() {
    return isHighFrequency() ? MatchReason.GIT_COCHANGE_HIGH : MatchReason.GIT_COCHANGE_MEDIUM;
  }

  public double calculateConfidence() {
    final MatchReason reason = getMatchReason();
    return reason.getBaseConfidence() * normalizedFrequency;
  }

  public String formatEvidence() {
    return String.format("Co-changed %d times", coChangeCount);
  }
}
