package com.ghiloufi.aicode.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.ghiloufi.aicode.core.application.service.audit.ContextAuditService;
import com.ghiloufi.aicode.core.domain.model.*;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ContextRetrievalSessionEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.repository.ContextRetrievalSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("Context Audit Persistence Integration Test")
final class ContextAuditPersistenceTest {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("ai_code_reviewer_test")
          .withUsername("test_user")
          .withPassword("test_password");

  @DynamicPropertySource
  static void configureProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired private ContextAuditService contextAuditService;

  @Autowired private ContextRetrievalSessionRepository sessionRepository;

  @Autowired
  private com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewJpaRepository
      reviewJpaRepository;

  @Test
  @DisplayName("should_persist_complete_context_audit_with_strategies_and_matches")
  final void should_persist_complete_context_audit_with_strategies_and_matches() {
    final UUID reviewId = createReviewInDatabase();
    final EnrichedDiffAnalysisBundle enrichedDiff = createEnrichedDiffWithContext();
    final String promptText = createSamplePrompt();
    final List<ContextAuditService.StrategyExecutionResult> strategyResults =
        createStrategyResults();

    contextAuditService.auditContextRetrieval(reviewId, enrichedDiff, promptText, strategyResults);

    final List<ContextRetrievalSessionEntity> sessions = sessionRepository.findByReviewId(reviewId);

    assertThat(sessions).hasSize(1);

    final ContextRetrievalSessionEntity session = sessions.get(0);
    assertThat(session.getReviewId()).isEqualTo(reviewId);
    assertThat(session.getTotalMatches()).isEqualTo(3);
    assertThat(session.getHighConfidenceMatches()).isEqualTo(2);
    assertThat(session.getStrategiesExecuted()).isEqualTo(2);
    assertThat(session.getStrategiesSucceeded()).isEqualTo(1);
    assertThat(session.getStrategiesFailed()).isEqualTo(1);
    assertThat(session.getPromptText()).isEqualTo(promptText);
    assertThat(session.getContextEnabled()).isTrue();
    assertThat(session.getSkippedDueToSize()).isFalse();

    assertThat(session.getStrategyExecutions()).hasSize(2);
    assertThat(session.getStrategyExecutions().get(0).getStrategyName())
        .isEqualTo("MetadataBasedStrategy");
    assertThat(session.getStrategyExecutions().get(0).getStatus())
        .isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(session.getStrategyExecutions().get(0).getMatches()).hasSize(3);
    assertThat(session.getStrategyExecutions().get(0).getReasonDistribution()).hasSize(2);

    assertThat(session.getStrategyExecutions().get(1).getStrategyName())
        .isEqualTo("HistoryBasedStrategy");
    assertThat(session.getStrategyExecutions().get(1).getStatus()).isEqualTo(ExecutionStatus.ERROR);
    assertThat(session.getStrategyExecutions().get(1).getErrorMessage())
        .isEqualTo("Timeout exceeded");
  }

  @Test
  @DisplayName("should_skip_audit_when_no_context_retrieved")
  final void should_skip_audit_when_no_context_retrieved() {
    final UUID reviewId = createReviewInDatabase();
    final EnrichedDiffAnalysisBundle enrichedDiff = createEnrichedDiffWithoutContext();
    final String promptText = "Simple prompt without context";
    final List<ContextAuditService.StrategyExecutionResult> strategyResults = List.of();

    contextAuditService.auditContextRetrieval(reviewId, enrichedDiff, promptText, strategyResults);

    final List<ContextRetrievalSessionEntity> sessions = sessionRepository.findByReviewId(reviewId);

    assertThat(sessions).isEmpty();
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
        new ContextMatch(
            "UserRepository.java", MatchReason.DIRECT_IMPORT, 0.95, "import UserRepository");
    final ContextMatch match2 =
        new ContextMatch("User.java", MatchReason.TYPE_REFERENCE, 0.85, "User type reference");
    final ContextMatch match3 =
        new ContextMatch("UserValidator.java", MatchReason.SIBLING_FILE, 0.70, "Same package");

    final ContextRetrievalMetadata metadata =
        new ContextRetrievalMetadata(
            "MetadataBasedStrategy",
            Duration.ofMillis(150),
            5,
            2,
            Map.of(MatchReason.DIRECT_IMPORT, 1, MatchReason.TYPE_REFERENCE, 1));

    final ContextRetrievalResult contextResult =
        new ContextRetrievalResult(List.of(match1, match2, match3), metadata);

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

  private String createSamplePrompt() {
    return """
                    [SYSTEM]
                    You are a code reviewer.
                    [/SYSTEM]

                    [DIFF]
                    diff --git a/UserService.java
                    [/DIFF]

                    [CONTEXT]
                    - UserRepository.java (confidence: 0.95, reason: Direct import)
                    - User.java (confidence: 0.85, reason: Type reference)
                    [/CONTEXT]
                    """;
  }

  private List<ContextAuditService.StrategyExecutionResult> createStrategyResults() {
    final ContextMatch match1 =
        new ContextMatch(
            "UserRepository.java", MatchReason.DIRECT_IMPORT, 0.95, "import UserRepository");
    final ContextMatch match2 =
        new ContextMatch("User.java", MatchReason.TYPE_REFERENCE, 0.85, "User type reference");
    final ContextMatch match3 =
        new ContextMatch("UserValidator.java", MatchReason.SIBLING_FILE, 0.70, "Same package");

    final ContextRetrievalMetadata metadata =
        new ContextRetrievalMetadata(
            "MetadataBasedStrategy",
            Duration.ofMillis(150),
            5,
            2,
            Map.of(MatchReason.DIRECT_IMPORT, 1, MatchReason.TYPE_REFERENCE, 1));

    final ContextRetrievalResult successResult =
        new ContextRetrievalResult(List.of(match1, match2, match3), metadata);

    final Instant startedAt = Instant.now().minusMillis(200);
    final Instant completedAt = Instant.now();

    final ContextAuditService.StrategyExecutionResult successExecution =
        ContextAuditService.StrategyExecutionResult.success(
            "MetadataBasedStrategy", successResult, 150L, startedAt, completedAt);

    final ContextAuditService.StrategyExecutionResult failedExecution =
        ContextAuditService.StrategyExecutionResult.failure(
            "HistoryBasedStrategy", "Timeout exceeded", 5000L, startedAt, completedAt);

    return List.of(successExecution, failedExecution);
  }

  private UUID createReviewInDatabase() {
    final com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewEntity review =
        com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewEntity.builder()
            .repositoryId("owner/repo")
            .changeRequestId("123")
            .provider("github")
            .status(com.ghiloufi.aicode.core.domain.model.ReviewState.PENDING)
            .llmProvider("anthropic")
            .llmModel("claude-3-opus")
            .build();

    final com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewEntity savedReview =
        reviewJpaRepository.save(review);
    return savedReview.getId();
  }
}
