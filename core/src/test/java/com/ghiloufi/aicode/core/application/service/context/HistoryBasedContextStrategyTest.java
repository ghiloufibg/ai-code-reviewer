package com.ghiloufi.aicode.core.application.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ghiloufi.aicode.core.domain.model.CoChangeAnalysisResult;
import com.ghiloufi.aicode.core.domain.model.CoChangeMetrics;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("HistoryBasedContextStrategy Tests")
final class HistoryBasedContextStrategyTest {

  private HistoryBasedContextStrategy strategy;
  private GitHistoryCoChangeAnalyzer mockAnalyzer;
  private RepositoryIdentifier testRepo;

  @BeforeEach
  final void setUp() {
    mockAnalyzer = mock(GitHistoryCoChangeAnalyzer.class);
    testRepo = RepositoryIdentifier.create(SourceProvider.GITLAB, "test-owner/test-repo");
    strategy = new HistoryBasedContextStrategy(mockAnalyzer);
  }

  @Nested
  @DisplayName("Strategy Identifier")
  final class StrategyIdentifier {

    @Test
    @DisplayName("should_return_git_history_as_strategy_name")
    final void should_return_git_history_as_strategy_name() {
      assertThat(strategy.getStrategyName()).isEqualTo("git-history");
    }

    @Test
    @DisplayName("should_return_priority_20")
    final void should_return_priority_20() {
      assertThat(strategy.getPriority()).isEqualTo(20);
    }
  }

  @Nested
  @DisplayName("Context Retrieval")
  final class ContextRetrieval {

    @Test
    @DisplayName("should_retrieve_context_from_multiple_changed_files")
    final void should_retrieve_context_from_multiple_changed_files() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification("src/UserService.java", "src/UserService.java"),
              new GitFileModification("src/UserController.java", "src/UserController.java"));

      final GitDiffDocument gitDiff = new GitDiffDocument(modifiedFiles);
      final DiffAnalysisBundle diffBundle =
          new DiffAnalysisBundle(testRepo, gitDiff, "rawDiffText", null, null);

      final List<CoChangeMetrics> metricsForService =
          List.of(
              new CoChangeMetrics("src/UserRepository.java", 10, 1.0),
              new CoChangeMetrics("src/UserMapper.java", 5, 0.5));

      final List<CoChangeMetrics> metricsForController =
          List.of(
              new CoChangeMetrics("src/UserRepository.java", 8, 1.0),
              new CoChangeMetrics("src/UserValidator.java", 4, 0.5));

      final CoChangeAnalysisResult resultForService =
          new CoChangeAnalysisResult("src/UserService.java", metricsForService, Map.of(), 10);

      final CoChangeAnalysisResult resultForController =
          new CoChangeAnalysisResult("src/UserController.java", metricsForController, Map.of(), 8);

      when(mockAnalyzer.analyzeCoChanges(eq(testRepo), eq("src/UserService.java")))
          .thenReturn(Mono.just(resultForService));

      when(mockAnalyzer.analyzeCoChanges(eq(testRepo), eq("src/UserController.java")))
          .thenReturn(Mono.just(resultForController));

      final Mono<ContextRetrievalResult> result = strategy.retrieveContext(diffBundle);

      StepVerifier.create(result)
          .assertNext(
              retrievalResult -> {
                assertThat(retrievalResult.matches()).isNotEmpty();
                assertThat(retrievalResult.metadata().strategyName()).isEqualTo("git-history");
                assertThat(retrievalResult.metadata().totalCandidates()).isGreaterThan(0);
              })
          .verifyComplete();

      verify(mockAnalyzer, times(2)).analyzeCoChanges(eq(testRepo), any(String.class));
    }

    @Test
    @DisplayName("should_deduplicate_matches_keeping_highest_confidence")
    final void should_deduplicate_matches_keeping_highest_confidence() {
      final List<GitFileModification> modifiedFiles =
          List.of(
              new GitFileModification("src/UserService.java", "src/UserService.java"),
              new GitFileModification("src/UserController.java", "src/UserController.java"));

      final GitDiffDocument gitDiff = new GitDiffDocument(modifiedFiles);
      final DiffAnalysisBundle diffBundle =
          new DiffAnalysisBundle(testRepo, gitDiff, "rawDiffText", null, null);

      final List<CoChangeMetrics> metricsForService =
          List.of(new CoChangeMetrics("src/UserRepository.java", 10, 1.0));

      final List<CoChangeMetrics> metricsForController =
          List.of(new CoChangeMetrics("src/UserRepository.java", 5, 0.5));

      final CoChangeAnalysisResult resultForService =
          new CoChangeAnalysisResult("src/UserService.java", metricsForService, Map.of(), 10);

      final CoChangeAnalysisResult resultForController =
          new CoChangeAnalysisResult("src/UserController.java", metricsForController, Map.of(), 5);

      when(mockAnalyzer.analyzeCoChanges(eq(testRepo), eq("src/UserService.java")))
          .thenReturn(Mono.just(resultForService));

      when(mockAnalyzer.analyzeCoChanges(eq(testRepo), eq("src/UserController.java")))
          .thenReturn(Mono.just(resultForController));

      final Mono<ContextRetrievalResult> result = strategy.retrieveContext(diffBundle);

      StepVerifier.create(result)
          .assertNext(
              retrievalResult -> {
                final List<String> filePaths =
                    retrievalResult.matches().stream().map(m -> m.filePath()).toList();

                assertThat(filePaths).contains("src/UserRepository.java");
                final long count =
                    filePaths.stream().filter(p -> p.equals("src/UserRepository.java")).count();
                assertThat(count).isEqualTo(1);

                final double repoConfidence =
                    retrievalResult.matches().stream()
                        .filter(m -> m.filePath().equals("src/UserRepository.java"))
                        .findFirst()
                        .orElseThrow()
                        .confidence();

                assertThat(repoConfidence).isGreaterThanOrEqualTo(0.70);
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_return_empty_result_when_no_co_changes_found")
    final void should_return_empty_result_when_no_co_changes_found() {
      final List<GitFileModification> modifiedFiles =
          List.of(new GitFileModification("src/UserService.java", "src/UserService.java"));

      final GitDiffDocument gitDiff = new GitDiffDocument(modifiedFiles);
      final DiffAnalysisBundle diffBundle =
          new DiffAnalysisBundle(testRepo, gitDiff, "rawDiffText", null, null);

      final CoChangeAnalysisResult emptyResult =
          new CoChangeAnalysisResult("src/UserService.java", List.of(), Map.of(), 0);

      when(mockAnalyzer.analyzeCoChanges(eq(testRepo), eq("src/UserService.java")))
          .thenReturn(Mono.just(emptyResult));

      final Mono<ContextRetrievalResult> result = strategy.retrieveContext(diffBundle);

      StepVerifier.create(result)
          .assertNext(
              retrievalResult -> {
                assertThat(retrievalResult.matches()).isEmpty();
                assertThat(retrievalResult.metadata().totalCandidates()).isZero();
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_handle_analyzer_errors")
    final void should_handle_analyzer_errors() {
      final List<GitFileModification> modifiedFiles =
          List.of(new GitFileModification("src/UserService.java", "src/UserService.java"));

      final GitDiffDocument gitDiff = new GitDiffDocument(modifiedFiles);
      final DiffAnalysisBundle diffBundle =
          new DiffAnalysisBundle(testRepo, gitDiff, "rawDiffText", null, null);

      when(mockAnalyzer.analyzeCoChanges(any(), any()))
          .thenReturn(Mono.error(new RuntimeException("Analysis failed")));

      final Mono<ContextRetrievalResult> result = strategy.retrieveContext(diffBundle);

      StepVerifier.create(result).expectError(RuntimeException.class).verify();
    }
  }
}
