package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

final class AgentTaskTest {

  private static final RepositoryIdentifier REPO =
      RepositoryIdentifier.create(SourceProvider.GITHUB, "org/repo");
  private static final ChangeRequestIdentifier PR =
      ChangeRequestIdentifier.create(SourceProvider.GITHUB, 123);

  @Test
  void should_create_task_with_defaults() {
    final var task = AgentTask.createWithDefaults(REPO, PR, "correlation-123");

    assertThat(task.taskId()).isNotNull();
    assertThat(task.repository()).isEqualTo(REPO);
    assertThat(task.changeRequest()).isEqualTo(PR);
    assertThat(task.configuration()).isNotNull();
    assertThat(task.state()).isNotNull();
    assertThat(task.state().status()).isEqualTo(AgentStatus.PENDING);
    assertThat(task.createdAt()).isNotNull();
    assertThat(task.correlationId()).isEqualTo("correlation-123");
  }

  @Test
  void should_create_task_with_custom_configuration() {
    final var config = AgentConfiguration.defaults();
    final var task = AgentTask.create(REPO, PR, config, "correlation-456");

    assertThat(task.configuration()).isEqualTo(config);
  }

  @Test
  void should_throw_when_task_id_is_null() {
    final var state = AgentState.initial();
    final var config = AgentConfiguration.defaults();

    assertThatThrownBy(() -> new AgentTask(null, REPO, PR, config, state, Instant.now(), "corr"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("taskId");
  }

  @Test
  void should_throw_when_repository_is_null() {
    final var state = AgentState.initial();
    final var config = AgentConfiguration.defaults();

    assertThatThrownBy(() -> new AgentTask("id", null, PR, config, state, Instant.now(), "corr"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("repository");
  }

  @Test
  void should_throw_when_change_request_is_null() {
    final var state = AgentState.initial();
    final var config = AgentConfiguration.defaults();

    assertThatThrownBy(() -> new AgentTask("id", REPO, null, config, state, Instant.now(), "corr"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("changeRequest");
  }

  @Test
  void should_throw_when_configuration_is_null() {
    final var state = AgentState.initial();

    assertThatThrownBy(() -> new AgentTask("id", REPO, PR, null, state, Instant.now(), "corr"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("configuration");
  }

  @Test
  void should_throw_when_state_is_null() {
    final var config = AgentConfiguration.defaults();

    assertThatThrownBy(() -> new AgentTask("id", REPO, PR, config, null, Instant.now(), "corr"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("state");
  }

  @Test
  void should_throw_when_created_at_is_null() {
    final var state = AgentState.initial();
    final var config = AgentConfiguration.defaults();

    assertThatThrownBy(() -> new AgentTask("id", REPO, PR, config, state, null, "corr"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("createdAt");
  }

  @Test
  void should_update_status() {
    final var task = AgentTask.createWithDefaults(REPO, PR, "corr");
    final var updated = task.updateStatus(AgentStatus.CLONING);

    assertThat(updated.currentStatus()).isEqualTo(AgentStatus.CLONING);
    assertThat(updated.taskId()).isEqualTo(task.taskId());
  }

  @Test
  void should_start_action() {
    final var task = AgentTask.createWithDefaults(REPO, PR, "corr");
    final var action = AgentAction.CloneRepository.started("url", "branch", "/dir");
    final var updated = task.startAction(action);

    assertThat(updated.state().currentAction()).isEqualTo(action);
  }

  @Test
  void should_complete_current_action() {
    final var action = AgentAction.CloneRepository.started("url", "branch", "/dir");
    final var task = AgentTask.createWithDefaults(REPO, PR, "corr").startAction(action);
    final var completed = task.completeCurrentAction();

    assertThat(completed.state().currentAction()).isNull();
    assertThat(completed.state().completedActions()).hasSize(1);
  }

  @Test
  void should_set_local_analysis_result() {
    final var metadata = AnalysisMetadata.started("image", "container", "main", "abc123");
    final var analysisResult = LocalAnalysisResult.empty(metadata);
    final var task = AgentTask.createWithDefaults(REPO, PR, "corr");

    final var updated = task.withLocalAnalysisResult(analysisResult);

    assertThat(updated.hasAnalysisResults()).isTrue();
    assertThat(updated.state().localAnalysisResult()).isEqualTo(analysisResult);
  }

  @Test
  void should_set_llm_review_result() {
    final var reviewResult = ReviewResult.builder().summary("Summary").build();
    final var task = AgentTask.createWithDefaults(REPO, PR, "corr");

    final var updated = task.withLlmReviewResult(reviewResult);

    assertThat(updated.hasLlmResults()).isTrue();
    assertThat(updated.state().llmReviewResult()).isEqualTo(reviewResult);
  }

  @Test
  void should_fail_task() {
    final var task = AgentTask.createWithDefaults(REPO, PR, "corr");
    final var failed = task.fail("Clone timeout");

    assertThat(failed.currentStatus()).isEqualTo(AgentStatus.FAILED);
    assertThat(failed.state().errorMessage()).isEqualTo("Clone timeout");
    assertThat(failed.isTerminal()).isTrue();
  }

  @Test
  void should_complete_task() {
    final var task = AgentTask.createWithDefaults(REPO, PR, "corr");
    final var completed = task.complete();

    assertThat(completed.currentStatus()).isEqualTo(AgentStatus.COMPLETED);
    assertThat(completed.isTerminal()).isTrue();
  }

  @Test
  void should_identify_terminal_state() {
    final var pending = AgentTask.createWithDefaults(REPO, PR, "corr");
    final var completed = pending.complete();
    final var failed = pending.fail("error");

    assertThat(pending.isTerminal()).isFalse();
    assertThat(completed.isTerminal()).isTrue();
    assertThat(failed.isTerminal()).isTrue();
  }

  @Test
  void should_identify_active_state() {
    final var pending = AgentTask.createWithDefaults(REPO, PR, "corr");
    final var cloning = pending.updateStatus(AgentStatus.CLONING);
    final var completed = pending.complete();

    assertThat(pending.isActive()).isFalse();
    assertThat(cloning.isActive()).isTrue();
    assertThat(completed.isActive()).isFalse();
  }

  @Test
  void should_detect_missing_results() {
    final var task = AgentTask.createWithDefaults(REPO, PR, "corr");

    assertThat(task.hasAnalysisResults()).isFalse();
    assertThat(task.hasLlmResults()).isFalse();
  }

  @Test
  void should_preserve_immutability_on_state_updates() {
    final var original = AgentTask.createWithDefaults(REPO, PR, "corr");
    final var updated = original.updateStatus(AgentStatus.CLONING);

    assertThat(original.currentStatus()).isEqualTo(AgentStatus.PENDING);
    assertThat(updated.currentStatus()).isEqualTo(AgentStatus.CLONING);
  }
}
