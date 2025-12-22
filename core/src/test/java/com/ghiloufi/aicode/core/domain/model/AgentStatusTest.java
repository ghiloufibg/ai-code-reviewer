package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

final class AgentStatusTest {

  @Test
  void should_have_all_expected_statuses() {
    assertThat(AgentStatus.values())
        .containsExactly(
            AgentStatus.PENDING,
            AgentStatus.CLONING,
            AgentStatus.ANALYZING,
            AgentStatus.REASONING,
            AgentStatus.PUBLISHING,
            AgentStatus.COMPLETED,
            AgentStatus.FAILED);
  }

  @Test
  void should_identify_terminal_states() {
    assertThat(AgentStatus.COMPLETED.isTerminal()).isTrue();
    assertThat(AgentStatus.FAILED.isTerminal()).isTrue();

    assertThat(AgentStatus.PENDING.isTerminal()).isFalse();
    assertThat(AgentStatus.CLONING.isTerminal()).isFalse();
    assertThat(AgentStatus.ANALYZING.isTerminal()).isFalse();
    assertThat(AgentStatus.REASONING.isTerminal()).isFalse();
    assertThat(AgentStatus.PUBLISHING.isTerminal()).isFalse();
  }

  @Test
  void should_identify_active_states() {
    assertThat(AgentStatus.CLONING.isActive()).isTrue();
    assertThat(AgentStatus.ANALYZING.isActive()).isTrue();
    assertThat(AgentStatus.REASONING.isActive()).isTrue();
    assertThat(AgentStatus.PUBLISHING.isActive()).isTrue();

    assertThat(AgentStatus.PENDING.isActive()).isFalse();
    assertThat(AgentStatus.COMPLETED.isActive()).isFalse();
    assertThat(AgentStatus.FAILED.isActive()).isFalse();
  }

  @Test
  void should_return_description() {
    assertThat(AgentStatus.PENDING.getDescription()).isEqualTo("Agent task pending execution");
    assertThat(AgentStatus.CLONING.getDescription()).isEqualTo("Cloning repository into container");
    assertThat(AgentStatus.ANALYZING.getDescription())
        .isEqualTo("Running static analysis and security scans");
    assertThat(AgentStatus.REASONING.getDescription())
        .isEqualTo("LLM analyzing findings and generating insights");
    assertThat(AgentStatus.PUBLISHING.getDescription())
        .isEqualTo("Publishing review comments to SCM");
    assertThat(AgentStatus.COMPLETED.getDescription())
        .isEqualTo("Agent review completed successfully");
    assertThat(AgentStatus.FAILED.getDescription()).isEqualTo("Agent review failed");
  }

  @Test
  void should_allow_transition_from_pending_to_cloning() {
    assertThat(AgentStatus.PENDING.canTransitionTo(AgentStatus.CLONING)).isTrue();
  }

  @Test
  void should_allow_transition_from_pending_to_failed() {
    assertThat(AgentStatus.PENDING.canTransitionTo(AgentStatus.FAILED)).isTrue();
  }

  @Test
  void should_allow_transition_from_cloning_to_analyzing() {
    assertThat(AgentStatus.CLONING.canTransitionTo(AgentStatus.ANALYZING)).isTrue();
  }

  @Test
  void should_allow_transition_from_analyzing_to_reasoning() {
    assertThat(AgentStatus.ANALYZING.canTransitionTo(AgentStatus.REASONING)).isTrue();
  }

  @Test
  void should_allow_transition_from_reasoning_to_publishing() {
    assertThat(AgentStatus.REASONING.canTransitionTo(AgentStatus.PUBLISHING)).isTrue();
  }

  @Test
  void should_allow_transition_from_publishing_to_completed() {
    assertThat(AgentStatus.PUBLISHING.canTransitionTo(AgentStatus.COMPLETED)).isTrue();
  }

  @Test
  void should_allow_transition_to_failed_from_any_non_terminal_state() {
    assertThat(AgentStatus.PENDING.canTransitionTo(AgentStatus.FAILED)).isTrue();
    assertThat(AgentStatus.CLONING.canTransitionTo(AgentStatus.FAILED)).isTrue();
    assertThat(AgentStatus.ANALYZING.canTransitionTo(AgentStatus.FAILED)).isTrue();
    assertThat(AgentStatus.REASONING.canTransitionTo(AgentStatus.FAILED)).isTrue();
    assertThat(AgentStatus.PUBLISHING.canTransitionTo(AgentStatus.FAILED)).isTrue();
  }

  @Test
  void should_not_allow_transition_from_terminal_states() {
    assertThat(AgentStatus.COMPLETED.canTransitionTo(AgentStatus.PENDING)).isFalse();
    assertThat(AgentStatus.COMPLETED.canTransitionTo(AgentStatus.FAILED)).isFalse();
    assertThat(AgentStatus.FAILED.canTransitionTo(AgentStatus.PENDING)).isFalse();
    assertThat(AgentStatus.FAILED.canTransitionTo(AgentStatus.COMPLETED)).isFalse();
  }

  @Test
  void should_not_allow_skipping_states() {
    assertThat(AgentStatus.PENDING.canTransitionTo(AgentStatus.ANALYZING)).isFalse();
    assertThat(AgentStatus.CLONING.canTransitionTo(AgentStatus.PUBLISHING)).isFalse();
    assertThat(AgentStatus.ANALYZING.canTransitionTo(AgentStatus.COMPLETED)).isFalse();
  }

  @Test
  void should_create_initial_state_transition() {
    final var transition = AgentStatus.StateTransition.initial(AgentStatus.PENDING);

    assertThat(transition.from()).isNull();
    assertThat(transition.to()).isEqualTo(AgentStatus.PENDING);
    assertThat(transition.reason()).isEqualTo("Initial state");
    assertThat(transition.timestamp()).isNotNull();
  }

  @Test
  void should_create_state_transition() {
    final var transition =
        AgentStatus.StateTransition.now(AgentStatus.PENDING, AgentStatus.CLONING, "Starting clone");

    assertThat(transition.from()).isEqualTo(AgentStatus.PENDING);
    assertThat(transition.to()).isEqualTo(AgentStatus.CLONING);
    assertThat(transition.reason()).isEqualTo("Starting clone");
    assertThat(transition.timestamp()).isNotNull();
  }

  @Test
  void should_throw_for_invalid_state_transition() {
    assertThatThrownBy(
            () ->
                new AgentStatus.StateTransition(
                    AgentStatus.PENDING, AgentStatus.ANALYZING, Instant.now(), "Invalid"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid state transition");
  }

  @Test
  void should_allow_transition_to_failed_in_state_transition() {
    final var transition =
        AgentStatus.StateTransition.now(AgentStatus.CLONING, AgentStatus.FAILED, "Clone failed");

    assertThat(transition.to()).isEqualTo(AgentStatus.FAILED);
  }
}
