package com.ghiloufi.aicode.core.domain.model;

import java.time.Instant;
import lombok.Getter;

@Getter
public enum AgentStatus {
  PENDING("Agent task pending execution", false, false),
  CLONING("Cloning repository into container", true, false),
  ANALYZING("Running static analysis and security scans", true, false),
  REASONING("LLM analyzing findings and generating insights", true, false),
  PUBLISHING("Publishing review comments to SCM", true, false),
  COMPLETED("Agent review completed successfully", false, true),
  FAILED("Agent review failed", false, true);

  private final String description;
  private final boolean active;
  private final boolean terminal;

  AgentStatus(final String description, final boolean active, final boolean terminal) {
    this.description = description;
    this.active = active;
    this.terminal = terminal;
  }

  public boolean canTransitionTo(final AgentStatus target) {
    if (this.terminal) {
      return false;
    }

    return switch (this) {
      case PENDING -> target == CLONING || target == FAILED;
      case CLONING -> target == ANALYZING || target == FAILED;
      case ANALYZING -> target == REASONING || target == FAILED;
      case REASONING -> target == PUBLISHING || target == FAILED;
      case PUBLISHING -> target == COMPLETED || target == FAILED;
      case COMPLETED, FAILED -> false;
    };
  }

  public record StateTransition(
      AgentStatus from, AgentStatus to, Instant timestamp, String reason) {

    public StateTransition {
      if (from != null && !from.canTransitionTo(to)) {
        throw new IllegalStateException("Invalid state transition from " + from + " to " + to);
      }
    }

    public static StateTransition initial(final AgentStatus status) {
      return new StateTransition(null, status, Instant.now(), "Initial state");
    }

    public static StateTransition now(
        final AgentStatus from, final AgentStatus to, final String reason) {
      return new StateTransition(from, to, Instant.now(), reason);
    }
  }
}
