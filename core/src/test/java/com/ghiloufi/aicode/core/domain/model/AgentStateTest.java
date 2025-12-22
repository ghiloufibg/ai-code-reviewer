package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class AgentStateTest {

  @Test
  void should_create_initial_state() {
    final var state = AgentState.initial();

    assertThat(state.status()).isEqualTo(AgentStatus.PENDING);
    assertThat(state.completedActions()).isEmpty();
    assertThat(state.currentAction()).isNull();
    assertThat(state.context()).isEmpty();
    assertThat(state.localAnalysisResult()).isNull();
    assertThat(state.llmReviewResult()).isNull();
    assertThat(state.lastUpdated()).isNotNull();
    assertThat(state.errorMessage()).isNull();
  }

  @Test
  void should_throw_when_status_is_null() {
    assertThatThrownBy(
            () -> new AgentState(null, List.of(), null, Map.of(), null, null, Instant.now(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("status");
  }

  @Test
  void should_throw_when_last_updated_is_null() {
    assertThatThrownBy(
            () ->
                new AgentState(
                    AgentStatus.PENDING, List.of(), null, Map.of(), null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("lastUpdated");
  }

  @Test
  void should_transition_status() {
    final var initial = AgentState.initial();
    final var updated = initial.withStatus(AgentStatus.CLONING);

    assertThat(updated.status()).isEqualTo(AgentStatus.CLONING);
    assertThat(updated.lastUpdated()).isAfterOrEqualTo(initial.lastUpdated());
  }

  @Test
  void should_set_current_action() {
    final var state = AgentState.initial();
    final var action = AgentAction.CloneRepository.started("url", "branch", "/dir");
    final var updated = state.withCurrentAction(action);

    assertThat(updated.currentAction()).isEqualTo(action);
  }

  @Test
  void should_complete_current_action() {
    final var action = AgentAction.CloneRepository.started("url", "branch", "/dir");
    final var stateWithAction = AgentState.initial().withCurrentAction(action);
    final var completed = stateWithAction.completeCurrentAction();

    assertThat(completed.currentAction()).isNull();
    assertThat(completed.completedActions()).hasSize(1);
    assertThat(completed.completedActions().get(0)).isEqualTo(action);
  }

  @Test
  void should_not_add_null_action_when_completing() {
    final var state = AgentState.initial();
    final var completed = state.completeCurrentAction();

    assertThat(completed.completedActions()).isEmpty();
  }

  @Test
  void should_set_local_analysis_result() {
    final var metadata = AnalysisMetadata.started("image", "container", "main", "abc123");
    final var analysisResult = LocalAnalysisResult.empty(metadata);

    final var state = AgentState.initial().withLocalAnalysisResult(analysisResult);

    assertThat(state.localAnalysisResult()).isEqualTo(analysisResult);
    assertThat(state.hasAnalysisResults()).isTrue();
  }

  @Test
  void should_set_llm_review_result() {
    final var reviewResult = ReviewResult.builder().summary("Test summary").build();

    final var state = AgentState.initial().withLlmReviewResult(reviewResult);

    assertThat(state.llmReviewResult()).isEqualTo(reviewResult);
    assertThat(state.hasLlmResults()).isTrue();
  }

  @Test
  void should_add_context_value() {
    final var state =
        AgentState.initial().withContextValue("key1", "value1").withContextValue("key2", 42);

    assertThat(state.context()).containsEntry("key1", "value1");
    assertThat(state.context()).containsEntry("key2", 42);
  }

  @Test
  void should_get_typed_context_value() {
    final var state =
        AgentState.initial().withContextValue("name", "test").withContextValue("count", 10);

    assertThat(state.getContextValue("name", String.class)).isEqualTo("test");
    assertThat(state.getContextValue("count", Integer.class)).isEqualTo(10);
    assertThat(state.getContextValue("missing", String.class)).isNull();
  }

  @Test
  void should_throw_when_context_value_type_mismatch() {
    final var state = AgentState.initial().withContextValue("count", 10);

    assertThatThrownBy(() -> state.getContextValue("count", String.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("count")
        .hasMessageContaining("String");
  }

  @Test
  void should_set_error_state() {
    final var state = AgentState.initial().withError("Something went wrong");

    assertThat(state.status()).isEqualTo(AgentStatus.FAILED);
    assertThat(state.errorMessage()).isEqualTo("Something went wrong");
    assertThat(state.isTerminal()).isTrue();
  }

  @Test
  void should_complete_state() {
    final var state = AgentState.initial().completed();

    assertThat(state.status()).isEqualTo(AgentStatus.COMPLETED);
    assertThat(state.currentAction()).isNull();
    assertThat(state.errorMessage()).isNull();
    assertThat(state.isTerminal()).isTrue();
  }

  @Test
  void should_identify_terminal_state() {
    assertThat(AgentState.initial().isTerminal()).isFalse();
    assertThat(AgentState.initial().completed().isTerminal()).isTrue();
    assertThat(AgentState.initial().withError("error").isTerminal()).isTrue();
  }

  @Test
  void should_identify_active_state() {
    assertThat(AgentState.initial().isActive()).isFalse();
    assertThat(AgentState.initial().withStatus(AgentStatus.ANALYZING).isActive()).isTrue();
    assertThat(AgentState.initial().completed().isActive()).isFalse();
  }

  @Test
  void should_count_completed_actions() {
    final var action1 = AgentAction.CloneRepository.started("url", "branch", "/dir");
    final var action2 = AgentAction.RunTests.started("mvn test");

    final var state =
        AgentState.initial()
            .withCurrentAction(action1)
            .completeCurrentAction()
            .withCurrentAction(action2)
            .completeCurrentAction();

    assertThat(state.completedActionsCount()).isEqualTo(2);
  }

  @Test
  void should_check_completed_action_type() {
    final var action = AgentAction.CloneRepository.started("url", "branch", "/dir");
    final var state = AgentState.initial().withCurrentAction(action).completeCurrentAction();

    assertThat(state.hasCompletedAction("CLONE_REPOSITORY")).isTrue();
    assertThat(state.hasCompletedAction("RUN_TESTS")).isFalse();
  }

  @Test
  void should_make_defensive_copy_of_completed_actions() {
    final var mutableList =
        new ArrayList<AgentAction>(
            List.of(AgentAction.CloneRepository.started("url", "branch", "/dir")));

    final var state =
        new AgentState(
            AgentStatus.PENDING, mutableList, null, Map.of(), null, null, Instant.now(), null);

    mutableList.clear();

    assertThat(state.completedActions()).hasSize(1);
  }

  @Test
  void should_make_defensive_copy_of_context() {
    final var mutableMap = new HashMap<>(Map.of("key", (Object) "value"));

    final var state =
        new AgentState(
            AgentStatus.PENDING, List.of(), null, mutableMap, null, null, Instant.now(), null);

    mutableMap.clear();

    assertThat(state.context()).containsEntry("key", "value");
  }

  @Test
  void should_handle_null_collections() {
    final var state =
        new AgentState(AgentStatus.PENDING, null, null, null, null, null, Instant.now(), null);

    assertThat(state.completedActions()).isEmpty();
    assertThat(state.context()).isEmpty();
  }
}
