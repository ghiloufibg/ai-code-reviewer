package com.ghiloufi.aicode.core.domain.model;

import java.time.Instant;
import lombok.Getter;

@Getter
public enum ReviewState {
  PENDING("Review pending execution"),
  IN_PROGRESS("Review in progress"),
  COMPLETED("Review completed successfully"),
  FAILED("Review failed");

  private final String description;

  ReviewState(final String description) {
    this.description = description;
  }

  public boolean isTerminal() {
    return this == COMPLETED || this == FAILED;
  }

  public boolean isActive() {
    return this == PENDING || this == IN_PROGRESS;
  }

  public record StateTransition(ReviewState state, Instant timestamp) {

    public static StateTransition now(final ReviewState state) {
      return new StateTransition(state, Instant.now());
    }
  }
}
