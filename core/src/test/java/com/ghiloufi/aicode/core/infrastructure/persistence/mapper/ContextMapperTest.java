package com.ghiloufi.aicode.core.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.*;

import com.ghiloufi.aicode.core.domain.model.*;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Context Mapper Unit Tests")
final class ContextMapperTest {

  private ContextMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ContextMapper();
  }

  @Test
  @DisplayName("should_map_all_match_reason_enum_values")
  final void should_map_all_match_reason_enum_values() {
    assertThat(mapper.toMatchReasonEnum(MatchReason.FILE_REFERENCE))
        .isEqualTo(MatchReasonEnum.FILE_REFERENCE);
    assertThat(mapper.toMatchReasonEnum(MatchReason.SIBLING_FILE))
        .isEqualTo(MatchReasonEnum.SIBLING_FILE);
    assertThat(mapper.toMatchReasonEnum(MatchReason.GIT_COCHANGE_HIGH))
        .isEqualTo(MatchReasonEnum.GIT_COCHANGE_HIGH);
    assertThat(mapper.toMatchReasonEnum(MatchReason.GIT_COCHANGE_MEDIUM))
        .isEqualTo(MatchReasonEnum.GIT_COCHANGE_MEDIUM);
    assertThat(mapper.toMatchReasonEnum(MatchReason.SAME_PACKAGE))
        .isEqualTo(MatchReasonEnum.SAME_PACKAGE);
    assertThat(mapper.toMatchReasonEnum(MatchReason.RELATED_LAYER))
        .isEqualTo(MatchReasonEnum.RELATED_LAYER);
    assertThat(mapper.toMatchReasonEnum(MatchReason.TEST_COUNTERPART))
        .isEqualTo(MatchReasonEnum.TEST_COUNTERPART);
    assertThat(mapper.toMatchReasonEnum(MatchReason.PARENT_PACKAGE))
        .isEqualTo(MatchReasonEnum.PARENT_PACKAGE);
    assertThat(mapper.toMatchReasonEnum(MatchReason.DIRECT_IMPORT))
        .isEqualTo(MatchReasonEnum.DIRECT_IMPORT);
    assertThat(mapper.toMatchReasonEnum(MatchReason.TYPE_REFERENCE))
        .isEqualTo(MatchReasonEnum.TYPE_REFERENCE);
    assertThat(mapper.toMatchReasonEnum(MatchReason.METHOD_CALL))
        .isEqualTo(MatchReasonEnum.METHOD_CALL);
    assertThat(mapper.toMatchReasonEnum(MatchReason.RAG_SEMANTIC))
        .isEqualTo(MatchReasonEnum.RAG_SEMANTIC);
  }

  @Test
  @DisplayName("should_map_context_match_to_entity_with_high_confidence")
  final void should_map_context_match_to_entity_with_high_confidence() {
    final ContextMatch domainMatch =
        new ContextMatch("UserService.java", MatchReason.DIRECT_IMPORT, 0.95, "import UserService");

    final ContextMatchEntity entity = mapper.toMatchEntity(domainMatch, true);

    assertThat(entity.getId()).isNotNull();
    assertThat(entity.getFilePath()).isEqualTo("UserService.java");
    assertThat(entity.getMatchReason()).isEqualTo(MatchReasonEnum.DIRECT_IMPORT);
    assertThat(entity.getConfidence()).isEqualTo(BigDecimal.valueOf(0.95));
    assertThat(entity.getEvidence()).isEqualTo("import UserService");
    assertThat(entity.getIsHighConfidence()).isTrue();
    assertThat(entity.getIncludedInPrompt()).isTrue();
    assertThat(entity.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("should_map_context_match_to_entity_with_low_confidence")
  final void should_map_context_match_to_entity_with_low_confidence() {
    final ContextMatch domainMatch =
        new ContextMatch("Helper.java", MatchReason.SIBLING_FILE, 0.65, "same directory");

    final ContextMatchEntity entity = mapper.toMatchEntity(domainMatch, false);

    assertThat(entity.getIsHighConfidence()).isFalse();
    assertThat(entity.getIncludedInPrompt()).isFalse();
    assertThat(entity.getConfidence()).isEqualTo(BigDecimal.valueOf(0.65));
  }

  @Test
  @DisplayName("should_map_reason_distribution_to_entity")
  final void should_map_reason_distribution_to_entity() {
    final ContextReasonDistributionEntity entity =
        mapper.toReasonDistributionEntity(MatchReason.DIRECT_IMPORT, 5);

    assertThat(entity.getId()).isNotNull();
    assertThat(entity.getMatchReason()).isEqualTo(MatchReasonEnum.DIRECT_IMPORT);
    assertThat(entity.getCount()).isEqualTo(5);
  }

  @Test
  @DisplayName("should_map_successful_strategy_execution_to_entity")
  final void should_map_successful_strategy_execution_to_entity() {
    final ContextMatch match1 =
        new ContextMatch("File1.java", MatchReason.DIRECT_IMPORT, 0.95, "evidence1");
    final ContextMatch match2 =
        new ContextMatch("File2.java", MatchReason.TYPE_REFERENCE, 0.85, "evidence2");

    final ContextRetrievalMetadata metadata =
        new ContextRetrievalMetadata(
            "TestStrategy",
            Duration.ofMillis(150),
            10,
            2,
            Map.of(MatchReason.DIRECT_IMPORT, 1, MatchReason.TYPE_REFERENCE, 1));

    final ContextRetrievalResult result =
        new ContextRetrievalResult(List.of(match1, match2), metadata);

    final Instant startedAt = Instant.now();
    final Instant completedAt = startedAt.plusMillis(150);

    final ContextStrategyExecutionEntity entity =
        mapper.toStrategyExecutionEntity("TestStrategy", 0, result, 150L, startedAt, completedAt);

    assertThat(entity.getId()).isNotNull();
    assertThat(entity.getStrategyName()).isEqualTo("TestStrategy");
    assertThat(entity.getExecutionOrder()).isEqualTo(0);
    assertThat(entity.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(entity.getExecutionTimeMs()).isEqualTo(150L);
    assertThat(entity.getTotalCandidates()).isEqualTo(10);
    assertThat(entity.getMatchesFound()).isEqualTo(2);
    assertThat(entity.getHighConfidenceCount()).isEqualTo(2);
    assertThat(entity.getErrorMessage()).isNull();
    assertThat(entity.getStartedAt()).isEqualTo(startedAt);
    assertThat(entity.getCompletedAt()).isEqualTo(completedAt);
    assertThat(entity.getMatches()).hasSize(2);
    assertThat(entity.getReasonDistribution()).hasSize(2);
  }

  @Test
  @DisplayName("should_map_failed_strategy_execution_to_entity")
  final void should_map_failed_strategy_execution_to_entity() {
    final Instant startedAt = Instant.now();
    final Instant completedAt = startedAt.plusMillis(5000);

    final ContextStrategyExecutionEntity entity =
        mapper.toFailedStrategyExecutionEntity(
            "FailedStrategy", 1, "Timeout exceeded", 5000L, startedAt, completedAt);

    assertThat(entity.getId()).isNotNull();
    assertThat(entity.getStrategyName()).isEqualTo("FailedStrategy");
    assertThat(entity.getExecutionOrder()).isEqualTo(1);
    assertThat(entity.getStatus()).isEqualTo(ExecutionStatus.ERROR);
    assertThat(entity.getExecutionTimeMs()).isEqualTo(5000L);
    assertThat(entity.getTotalCandidates()).isNull();
    assertThat(entity.getMatchesFound()).isNull();
    assertThat(entity.getHighConfidenceCount()).isNull();
    assertThat(entity.getErrorMessage()).isEqualTo("Timeout exceeded");
    assertThat(entity.getStartedAt()).isEqualTo(startedAt);
    assertThat(entity.getCompletedAt()).isEqualTo(completedAt);
  }

  @Test
  @DisplayName("should_map_enriched_diff_to_session_entity")
  final void should_map_enriched_diff_to_session_entity() {
    final UUID reviewId = UUID.randomUUID();
    final EnrichedDiffAnalysisBundle enrichedDiff = createEnrichedDiffWithContext();
    final String promptText = "Test prompt text";

    final ContextRetrievalSessionEntity entity =
        mapper.toSessionEntity(reviewId, enrichedDiff, promptText);

    assertThat(entity.getId()).isNotNull();
    assertThat(entity.getReviewId()).isEqualTo(reviewId);
    assertThat(entity.getCreatedAt()).isNotNull();
    assertThat(entity.getDiffFileCount()).isEqualTo(1);
    assertThat(entity.getDiffLineCount()).isGreaterThan(0);
    assertThat(entity.getTotalMatches()).isEqualTo(2);
    assertThat(entity.getHighConfidenceMatches()).isEqualTo(2);
    assertThat(entity.getContextEnabled()).isTrue();
    assertThat(entity.getSkippedDueToSize()).isFalse();
    assertThat(entity.getPromptText()).isEqualTo(promptText);
    assertThat(entity.getPromptSizeBytes()).isEqualTo(promptText.getBytes().length);
  }

  @Test
  @DisplayName("should_map_enriched_diff_without_context_to_session_entity")
  final void should_map_enriched_diff_without_context_to_session_entity() {
    final UUID reviewId = UUID.randomUUID();
    final EnrichedDiffAnalysisBundle enrichedDiff = createEnrichedDiffWithoutContext();
    final String promptText = "Simple prompt";

    final ContextRetrievalSessionEntity entity =
        mapper.toSessionEntity(reviewId, enrichedDiff, promptText);

    assertThat(entity.getTotalMatches()).isEqualTo(0);
    assertThat(entity.getHighConfidenceMatches()).isEqualTo(0);
    assertThat(entity.getContextEnabled()).isFalse();
  }

  private EnrichedDiffAnalysisBundle createEnrichedDiffWithContext() {
    final RepositoryIdentifier repoId =
        RepositoryIdentifier.create(SourceProvider.GITHUB, "owner/repo");

    final DiffHunkBlock hunk = new DiffHunkBlock(1, 1, 10, 12);
    hunk.lines = List.of("+code");

    final GitFileModification modification =
        new GitFileModification("UserService.java", "UserService.java");
    modification.diffHunkBlocks = List.of(hunk);
    final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));

    final DiffAnalysisBundle basicBundle =
        new DiffAnalysisBundle(repoId, gitDiff, "diff --git a/UserService.java");

    final ContextMatch match1 =
        new ContextMatch("UserRepository.java", MatchReason.DIRECT_IMPORT, 0.95, "import");
    final ContextMatch match2 =
        new ContextMatch("User.java", MatchReason.TYPE_REFERENCE, 0.85, "reference");

    final ContextRetrievalMetadata metadata =
        new ContextRetrievalMetadata(
            "TestStrategy",
            Duration.ofMillis(150),
            5,
            2,
            Map.of(MatchReason.DIRECT_IMPORT, 1, MatchReason.TYPE_REFERENCE, 1));

    final ContextRetrievalResult contextResult =
        new ContextRetrievalResult(List.of(match1, match2), metadata);

    return new EnrichedDiffAnalysisBundle(basicBundle).withContext(contextResult);
  }

  private EnrichedDiffAnalysisBundle createEnrichedDiffWithoutContext() {
    final RepositoryIdentifier repoId =
        RepositoryIdentifier.create(SourceProvider.GITHUB, "owner/repo2");

    final DiffHunkBlock hunk = new DiffHunkBlock(1, 1, 5, 6);
    hunk.lines = List.of("+code");

    final GitFileModification modification =
        new GitFileModification("SimpleFile.java", "SimpleFile.java");
    modification.diffHunkBlocks = List.of(hunk);
    final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));

    final DiffAnalysisBundle basicBundle =
        new DiffAnalysisBundle(repoId, gitDiff, "diff --git a/SimpleFile.java");

    return new EnrichedDiffAnalysisBundle(basicBundle);
  }
}
