package com.ghiloufi.aicode.core.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentTask(
    String taskId,
    RepositoryIdentifier repository,
    ChangeRequestIdentifier changeRequest,
    AgentConfiguration configuration,
    AgentState state,
    Instant createdAt,
    String correlationId) {

  public AgentTask {
    Objects.requireNonNull(taskId, "taskId must not be null");
    Objects.requireNonNull(repository, "repository must not be null");
    Objects.requireNonNull(changeRequest, "changeRequest must not be null");
    Objects.requireNonNull(configuration, "configuration must not be null");
    Objects.requireNonNull(state, "state must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  public static AgentTask create(
      final RepositoryIdentifier repository,
      final ChangeRequestIdentifier changeRequest,
      final AgentConfiguration configuration,
      final String correlationId) {
    return new AgentTask(
        UUID.randomUUID().toString(),
        repository,
        changeRequest,
        configuration,
        AgentState.initial(),
        Instant.now(),
        correlationId);
  }

  public static AgentTask createWithDefaults(
      final RepositoryIdentifier repository,
      final ChangeRequestIdentifier changeRequest,
      final String correlationId) {
    return create(repository, changeRequest, AgentConfiguration.defaults(), correlationId);
  }

  public AgentTask withState(final AgentState newState) {
    return new AgentTask(
        taskId, repository, changeRequest, configuration, newState, createdAt, correlationId);
  }

  public AgentTask updateStatus(final AgentStatus newStatus) {
    return withState(state.withStatus(newStatus));
  }

  public AgentTask startAction(final AgentAction action) {
    return withState(state.withCurrentAction(action));
  }

  public AgentTask completeCurrentAction() {
    return withState(state.completeCurrentAction());
  }

  public AgentTask withLocalAnalysisResult(final LocalAnalysisResult result) {
    return withState(state.withLocalAnalysisResult(result));
  }

  public AgentTask withLlmReviewResult(final ReviewResult result) {
    return withState(state.withLlmReviewResult(result));
  }

  public AgentTask fail(final String errorMessage) {
    return withState(state.withError(errorMessage));
  }

  public AgentTask complete() {
    return withState(state.completed());
  }

  public boolean isTerminal() {
    return state.isTerminal();
  }

  public boolean isActive() {
    return state.isActive();
  }

  public AgentStatus currentStatus() {
    return state.status();
  }

  public boolean hasAnalysisResults() {
    return state.hasAnalysisResults();
  }

  public boolean hasLlmResults() {
    return state.hasLlmResults();
  }
}
