package com.ghiloufi.aicode.core.application.service.audit;

import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ContextRetrievalSessionEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ContextStrategyExecutionEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.mapper.ContextMapper;
import com.ghiloufi.aicode.core.infrastructure.persistence.repository.ContextRetrievalSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContextAuditService {

  private final ContextRetrievalSessionRepository sessionRepository;
  private final ContextMapper mapper;

  @Transactional
  public void auditContextRetrieval(
      final UUID reviewId,
      final EnrichedDiffAnalysisBundle enrichedDiff,
      final String promptText,
      final List<StrategyExecutionResult> strategyResults) {

    Objects.requireNonNull(reviewId, "Review ID cannot be null");
    Objects.requireNonNull(enrichedDiff, "Enriched diff cannot be null");
    Objects.requireNonNull(promptText, "Prompt text cannot be null");
    Objects.requireNonNull(strategyResults, "Strategy results cannot be null");

    if (!enrichedDiff.hasContext()) {
      log.debug("Skipping context audit for review {} - no context retrieved", reviewId);
      return;
    }

    log.debug(
        "Auditing context retrieval for review {} with {} strategy executions",
        reviewId,
        strategyResults.size());

    final ContextRetrievalSessionEntity session =
        mapper.toSessionEntity(reviewId, enrichedDiff, promptText);

    final long totalExecutionTimeMs =
        strategyResults.stream().mapToLong(StrategyExecutionResult::executionTimeMs).sum();

    final int strategiesSucceeded =
        (int) strategyResults.stream().filter(StrategyExecutionResult::success).count();

    final int strategiesFailed = strategyResults.size() - strategiesSucceeded;

    session.setTotalExecutionTimeMs(totalExecutionTimeMs);
    session.setStrategiesExecuted(strategyResults.size());
    session.setStrategiesSucceeded(strategiesSucceeded);
    session.setStrategiesFailed(strategiesFailed);

    int executionOrder = 0;
    for (final StrategyExecutionResult result : strategyResults) {
      final ContextStrategyExecutionEntity executionEntity;

      if (result.success()) {
        executionEntity =
            mapper.toStrategyExecutionEntity(
                result.strategyName(),
                executionOrder++,
                result.contextResult(),
                result.executionTimeMs(),
                result.startedAt(),
                result.completedAt());
      } else {
        executionEntity =
            mapper.toFailedStrategyExecutionEntity(
                result.strategyName(),
                executionOrder++,
                result.errorMessage(),
                result.executionTimeMs(),
                result.startedAt(),
                result.completedAt());
      }

      session.addStrategyExecution(executionEntity);
    }

    sessionRepository.save(session);

    log.info(
        "Context audit saved for review {} - {} matches from {} strategies ({} succeeded, {} failed)",
        reviewId,
        enrichedDiff.getContextMatchCount(),
        strategyResults.size(),
        strategiesSucceeded,
        strategiesFailed);
  }

  public record StrategyExecutionResult(
      String strategyName,
      boolean success,
      com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult contextResult,
      String errorMessage,
      long executionTimeMs,
      Instant startedAt,
      Instant completedAt) {

    public StrategyExecutionResult {
      Objects.requireNonNull(strategyName, "Strategy name cannot be null");
      Objects.requireNonNull(startedAt, "Started at cannot be null");
      Objects.requireNonNull(completedAt, "Completed at cannot be null");

      if (success && contextResult == null) {
        throw new IllegalArgumentException(
            "Context result cannot be null for successful execution");
      }

      if (!success && errorMessage == null) {
        throw new IllegalArgumentException("Error message cannot be null for failed execution");
      }
    }

    public static StrategyExecutionResult success(
        final String strategyName,
        final com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult contextResult,
        final long executionTimeMs,
        final Instant startedAt,
        final Instant completedAt) {
      return new StrategyExecutionResult(
          strategyName, true, contextResult, null, executionTimeMs, startedAt, completedAt);
    }

    public static StrategyExecutionResult failure(
        final String strategyName,
        final String errorMessage,
        final long executionTimeMs,
        final Instant startedAt,
        final Instant completedAt) {
      return new StrategyExecutionResult(
          strategyName, false, null, errorMessage, executionTimeMs, startedAt, completedAt);
    }
  }
}
