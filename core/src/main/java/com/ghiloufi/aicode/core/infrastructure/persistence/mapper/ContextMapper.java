package com.ghiloufi.aicode.core.infrastructure.persistence.mapper;

import com.ghiloufi.aicode.core.domain.model.*;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ContextMapper {

  public MatchReasonEnum toMatchReasonEnum(final MatchReason domainReason) {
    return switch (domainReason) {
      case FILE_REFERENCE -> MatchReasonEnum.FILE_REFERENCE;
      case SIBLING_FILE -> MatchReasonEnum.SIBLING_FILE;
      case GIT_COCHANGE_HIGH -> MatchReasonEnum.GIT_COCHANGE_HIGH;
      case GIT_COCHANGE_MEDIUM -> MatchReasonEnum.GIT_COCHANGE_MEDIUM;
      case SAME_PACKAGE -> MatchReasonEnum.SAME_PACKAGE;
      case RELATED_LAYER -> MatchReasonEnum.RELATED_LAYER;
      case TEST_COUNTERPART -> MatchReasonEnum.TEST_COUNTERPART;
      case PARENT_PACKAGE -> MatchReasonEnum.PARENT_PACKAGE;
      case DIRECT_IMPORT -> MatchReasonEnum.DIRECT_IMPORT;
      case TYPE_REFERENCE -> MatchReasonEnum.TYPE_REFERENCE;
      case METHOD_CALL -> MatchReasonEnum.METHOD_CALL;
      case RAG_SEMANTIC -> MatchReasonEnum.RAG_SEMANTIC;
    };
  }

  public ContextMatchEntity toMatchEntity(
      final ContextMatch domainMatch, final boolean includedInPrompt) {
    return new ContextMatchEntity(
        UUID.randomUUID(),
        domainMatch.filePath(),
        toMatchReasonEnum(domainMatch.reason()),
        BigDecimal.valueOf(domainMatch.confidence()),
        domainMatch.evidence(),
        domainMatch.isHighConfidence(),
        includedInPrompt,
        Instant.now());
  }

  public ContextReasonDistributionEntity toReasonDistributionEntity(
      final MatchReason reason, final Integer count) {
    return new ContextReasonDistributionEntity(UUID.randomUUID(), toMatchReasonEnum(reason), count);
  }

  public ContextStrategyExecutionEntity toStrategyExecutionEntity(
      final String strategyName,
      final int executionOrder,
      final ContextRetrievalResult result,
      final long executionTimeMs,
      final Instant startedAt,
      final Instant completedAt) {

    final ContextStrategyExecutionEntity entity =
        new ContextStrategyExecutionEntity(
            UUID.randomUUID(),
            strategyName,
            executionOrder,
            ExecutionStatus.SUCCESS,
            executionTimeMs,
            result.metadata().totalCandidates(),
            result.getTotalMatches(),
            result.metadata().highConfidenceCount(),
            null,
            startedAt,
            completedAt);

    for (final ContextMatch match : result.matches()) {
      final ContextMatchEntity matchEntity = toMatchEntity(match, true);
      entity.addMatch(matchEntity);
    }

    for (final Map.Entry<MatchReason, Integer> entry :
        result.metadata().reasonDistribution().entrySet()) {
      final ContextReasonDistributionEntity distributionEntity =
          toReasonDistributionEntity(entry.getKey(), entry.getValue());
      entity.addReasonDistribution(distributionEntity);
    }

    return entity;
  }

  public ContextStrategyExecutionEntity toFailedStrategyExecutionEntity(
      final String strategyName,
      final int executionOrder,
      final String errorMessage,
      final long executionTimeMs,
      final Instant startedAt,
      final Instant completedAt) {

    return new ContextStrategyExecutionEntity(
        UUID.randomUUID(),
        strategyName,
        executionOrder,
        ExecutionStatus.ERROR,
        executionTimeMs,
        null,
        null,
        null,
        errorMessage,
        startedAt,
        completedAt);
  }

  public ContextRetrievalSessionEntity toSessionEntity(
      final UUID reviewId, final EnrichedDiffAnalysisBundle enrichedDiff, final String promptText) {

    final boolean hasContext = enrichedDiff.hasContext();
    final int totalMatches = hasContext ? enrichedDiff.getContextMatchCount() : 0;
    final int highConfidenceMatches =
        hasContext ? enrichedDiff.contextResult().get().getHighConfidenceMatches().size() : 0;

    return new ContextRetrievalSessionEntity(
        UUID.randomUUID(),
        reviewId,
        Instant.now(),
        0L,
        0,
        0,
        0,
        enrichedDiff.getModifiedFileCount(),
        enrichedDiff.getTotalLineCount(),
        totalMatches,
        highConfidenceMatches,
        hasContext,
        false,
        promptText,
        promptText.getBytes().length);
  }
}
