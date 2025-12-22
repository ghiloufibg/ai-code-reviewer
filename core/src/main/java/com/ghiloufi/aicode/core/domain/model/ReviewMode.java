package com.ghiloufi.aicode.core.domain.model;

import lombok.Getter;

@Getter
public enum ReviewMode {
  DIFF("Diff-only analysis via SCM API"),
  AGENTIC("Full repository checkout with static analysis and security scans");

  private final String description;

  ReviewMode(final String description) {
    this.description = description;
  }

  public boolean requiresContainerExecution() {
    return this == AGENTIC;
  }

  public static ReviewMode fromString(final String value) {
    if (value == null || value.isBlank()) {
      return DIFF;
    }

    for (final ReviewMode mode : values()) {
      if (mode.name().equalsIgnoreCase(value.trim())) {
        return mode;
      }
    }

    return DIFF;
  }
}
