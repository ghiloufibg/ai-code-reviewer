package com.ghiloufi.aicode.core.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AgentState(
    AgentStatus status,
    List<AgentAction> completedActions,
    AgentAction currentAction,
    Map<String, Object> context,
    LocalAnalysisResult localAnalysisResult,
    ReviewResult llmReviewResult,
    Instant lastUpdated,
    String errorMessage) {

  public AgentState {
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(lastUpdated, "lastUpdated must not be null");
    completedActions = completedActions != null ? List.copyOf(completedActions) : List.of();
    context = context != null ? Map.copyOf(context) : Map.of();
  }

  public static AgentState initial() {
    return new AgentState(
        AgentStatus.PENDING, List.of(), null, Map.of(), null, null, Instant.now(), null);
  }

  public AgentState withStatus(final AgentStatus newStatus) {
    return new AgentState(
        newStatus,
        completedActions,
        currentAction,
        context,
        localAnalysisResult,
        llmReviewResult,
        Instant.now(),
        errorMessage);
  }

  public AgentState withCurrentAction(final AgentAction action) {
    return new AgentState(
        status,
        completedActions,
        action,
        context,
        localAnalysisResult,
        llmReviewResult,
        Instant.now(),
        errorMessage);
  }

  public AgentState completeCurrentAction() {
    if (currentAction == null) {
      return this;
    }

    final List<AgentAction> updatedCompleted =
        java.util.stream.Stream.concat(
                completedActions.stream(), java.util.stream.Stream.of(currentAction))
            .toList();

    return new AgentState(
        status,
        updatedCompleted,
        null,
        context,
        localAnalysisResult,
        llmReviewResult,
        Instant.now(),
        errorMessage);
  }

  public AgentState withLocalAnalysisResult(final LocalAnalysisResult result) {
    return new AgentState(
        status,
        completedActions,
        currentAction,
        context,
        result,
        llmReviewResult,
        Instant.now(),
        errorMessage);
  }

  public AgentState withLlmReviewResult(final ReviewResult result) {
    return new AgentState(
        status,
        completedActions,
        currentAction,
        context,
        localAnalysisResult,
        result,
        Instant.now(),
        errorMessage);
  }

  public AgentState withContextValue(final String key, final Object value) {
    final Map<String, Object> updatedContext =
        java.util.stream.Stream.concat(
                context.entrySet().stream(), java.util.stream.Stream.of(Map.entry(key, value)))
            .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return new AgentState(
        status,
        completedActions,
        currentAction,
        updatedContext,
        localAnalysisResult,
        llmReviewResult,
        Instant.now(),
        errorMessage);
  }

  public AgentState withError(final String message) {
    return new AgentState(
        AgentStatus.FAILED,
        completedActions,
        currentAction,
        context,
        localAnalysisResult,
        llmReviewResult,
        Instant.now(),
        message);
  }

  public AgentState completed() {
    return new AgentState(
        AgentStatus.COMPLETED,
        completedActions,
        null,
        context,
        localAnalysisResult,
        llmReviewResult,
        Instant.now(),
        null);
  }

  public boolean isTerminal() {
    return status.isTerminal();
  }

  public boolean isActive() {
    return status.isActive();
  }

  public int completedActionsCount() {
    return completedActions.size();
  }

  public boolean hasCompletedAction(final String actionType) {
    return completedActions.stream().anyMatch(a -> a.actionType().equals(actionType));
  }

  public boolean hasAnalysisResults() {
    return localAnalysisResult != null;
  }

  public boolean hasLlmResults() {
    return llmReviewResult != null;
  }

  @SuppressWarnings("unchecked")
  public <T> T getContextValue(final String key, final Class<T> type) {
    final Object value = context.get(key);
    if (value == null) {
      return null;
    }
    if (!type.isInstance(value)) {
      throw new IllegalStateException(
          "Context value for key '" + key + "' is not of type " + type.getName());
    }
    return (T) value;
  }
}
