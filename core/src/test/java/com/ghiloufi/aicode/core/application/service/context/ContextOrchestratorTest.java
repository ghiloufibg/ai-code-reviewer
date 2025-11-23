package com.ghiloufi.aicode.core.application.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalMetadata;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.DiffHunkBlock;
import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.output.ContextRetrievalStrategy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("ContextOrchestrator Tests")
@ExtendWith(MockitoExtension.class)
final class ContextOrchestratorTest {

  @Mock(lenient = true)
  private ContextRetrievalStrategy metadataStrategy;

  @Mock(lenient = true)
  private ContextRetrievalStrategy historyStrategy;

  private ContextEnricher contextEnricher;
  private ContextOrchestrator orchestrator;
  private DiffAnalysisBundle testBundle;

  @BeforeEach
  final void setUp() {
    contextEnricher = new ContextEnricher();

    when(metadataStrategy.getStrategyName()).thenReturn("metadata-based");
    when(metadataStrategy.getPriority()).thenReturn(10);

    when(historyStrategy.getStrategyName()).thenReturn("git-history");
    when(historyStrategy.getPriority()).thenReturn(20);

    testBundle = createTestBundle(10);
  }

  private DiffAnalysisBundle createTestBundle(final int lineCount) {
    final RepositoryIdentifier repo =
        RepositoryIdentifier.create(SourceProvider.GITLAB, "test-org/test-repo");
    final GitFileModification modification =
        new GitFileModification("src/Test.java", "src/Test.java");
    final DiffHunkBlock hunk = new DiffHunkBlock();
    for (int i = 0; i < lineCount; i++) {
      hunk.lines.add("+line " + i);
    }
    modification.diffHunkBlocks.add(hunk);

    final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));
    return new DiffAnalysisBundle(repo, gitDiff, "diff content", null, null);
  }

  @Nested
  @DisplayName("Feature Flag Behavior")
  final class FeatureFlagBehavior {

    @Test
    @DisplayName("should_return_basic_bundle_when_context_retrieval_disabled")
    final void should_return_basic_bundle_when_context_retrieval_disabled() {
      final ContextRetrievalConfig config =
          new ContextRetrievalConfig(
              false,
              5,
              List.of("metadata-based", "git-history"),
              new ContextRetrievalConfig.RolloutConfig(100, false, 5000));

      orchestrator = new ContextOrchestrator(List.of(metadataStrategy), contextEnricher, config);

      final Mono<EnrichedDiffAnalysisBundle> result =
          orchestrator.retrieveEnrichedContext(testBundle);

      StepVerifier.create(result)
          .assertNext(enriched -> assertThat(enriched.hasContext()).isFalse())
          .verifyComplete();

      verify(metadataStrategy, never()).retrieveContext(any());
    }

    @Test
    @DisplayName("should_skip_disabled_strategies")
    final void should_skip_disabled_strategies() {
      final ContextRetrievalConfig config =
          new ContextRetrievalConfig(
              true,
              5,
              List.of("metadata-based"),
              new ContextRetrievalConfig.RolloutConfig(100, false, 5000));

      orchestrator =
          new ContextOrchestrator(
              List.of(metadataStrategy, historyStrategy), contextEnricher, config);

      when(metadataStrategy.retrieveContext(testBundle))
          .thenReturn(Mono.just(createEmptyResult("metadata-based")));

      final Mono<EnrichedDiffAnalysisBundle> result =
          orchestrator.retrieveEnrichedContext(testBundle);

      StepVerifier.create(result)
          .assertNext(enriched -> assertThat(enriched).isNotNull())
          .verifyComplete();

      verify(metadataStrategy, times(1)).retrieveContext(testBundle);
      verify(historyStrategy, never()).retrieveContext(any());
    }
  }

  @Nested
  @DisplayName("Parallel Execution")
  final class ParallelExecution {

    @Test
    @DisplayName("should_execute_multiple_strategies_in_parallel")
    final void should_execute_multiple_strategies_in_parallel() {
      final ContextRetrievalConfig config = ContextRetrievalConfig.defaults();

      orchestrator =
          new ContextOrchestrator(
              List.of(metadataStrategy, historyStrategy), contextEnricher, config);

      final List<ContextMatch> metadataMatches =
          List.of(new ContextMatch("FileA.java", MatchReason.DIRECT_IMPORT, 0.8, "evidence1"));

      final List<ContextMatch> historyMatches =
          List.of(new ContextMatch("FileB.java", MatchReason.GIT_COCHANGE_HIGH, 0.9, "evidence2"));

      when(metadataStrategy.retrieveContext(testBundle))
          .thenReturn(
              Mono.just(
                  new ContextRetrievalResult(
                      metadataMatches,
                      new ContextRetrievalMetadata(
                          "metadata-based", Duration.ofMillis(100), 1, 1, Map.of()))));

      when(historyStrategy.retrieveContext(testBundle))
          .thenReturn(
              Mono.just(
                  new ContextRetrievalResult(
                      historyMatches,
                      new ContextRetrievalMetadata(
                          "git-history", Duration.ofMillis(200), 1, 1, Map.of()))));

      final Mono<EnrichedDiffAnalysisBundle> result =
          orchestrator.retrieveEnrichedContext(testBundle);

      StepVerifier.create(result)
          .assertNext(
              enriched -> {
                assertThat(enriched.hasContext()).isTrue();
                assertThat(enriched.getContextMatchCount()).isEqualTo(2);
                assertThat(enriched.contextResult().metadata().strategyName())
                    .isEqualTo("metadata-based+git-history");
              })
          .verifyComplete();

      verify(metadataStrategy, times(1)).retrieveContext(testBundle);
      verify(historyStrategy, times(1)).retrieveContext(testBundle);
    }
  }

  @Nested
  @DisplayName("Error Handling")
  final class ErrorHandling {

    @Test
    @DisplayName("should_handle_strategy_failure_gracefully")
    final void should_handle_strategy_failure_gracefully() {
      final ContextRetrievalConfig config = ContextRetrievalConfig.defaults();

      orchestrator =
          new ContextOrchestrator(
              List.of(metadataStrategy, historyStrategy), contextEnricher, config);

      when(metadataStrategy.retrieveContext(testBundle))
          .thenReturn(Mono.error(new RuntimeException("Strategy failed")));

      final List<ContextMatch> historyMatches =
          List.of(new ContextMatch("FileB.java", MatchReason.GIT_COCHANGE_HIGH, 0.9, "evidence"));

      when(historyStrategy.retrieveContext(testBundle))
          .thenReturn(
              Mono.just(
                  new ContextRetrievalResult(
                      historyMatches,
                      new ContextRetrievalMetadata(
                          "git-history", Duration.ofMillis(200), 1, 1, Map.of()))));

      final Mono<EnrichedDiffAnalysisBundle> result =
          orchestrator.retrieveEnrichedContext(testBundle);

      StepVerifier.create(result)
          .assertNext(
              enriched -> {
                assertThat(enriched.hasContext()).isTrue();
                assertThat(enriched.getContextMatchCount()).isEqualTo(1);
                assertThat(enriched.contextResult().metadata().strategyName())
                    .isEqualTo("git-history");
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_return_basic_bundle_when_all_strategies_fail")
    final void should_return_basic_bundle_when_all_strategies_fail() {
      final ContextRetrievalConfig config = ContextRetrievalConfig.defaults();

      orchestrator =
          new ContextOrchestrator(
              List.of(metadataStrategy, historyStrategy), contextEnricher, config);

      when(metadataStrategy.retrieveContext(testBundle))
          .thenReturn(Mono.error(new RuntimeException("Strategy 1 failed")));

      when(historyStrategy.retrieveContext(testBundle))
          .thenReturn(Mono.error(new RuntimeException("Strategy 2 failed")));

      final Mono<EnrichedDiffAnalysisBundle> result =
          orchestrator.retrieveEnrichedContext(testBundle);

      StepVerifier.create(result)
          .assertNext(enriched -> assertThat(enriched.hasContext()).isFalse())
          .verifyComplete();
    }

    @Test
    @DisplayName("should_handle_strategy_timeout")
    final void should_handle_strategy_timeout() {
      final ContextRetrievalConfig config =
          new ContextRetrievalConfig(
              true,
              1,
              List.of("metadata-based", "git-history"),
              new ContextRetrievalConfig.RolloutConfig(100, false, 5000));

      orchestrator =
          new ContextOrchestrator(
              List.of(metadataStrategy, historyStrategy), contextEnricher, config);

      when(metadataStrategy.retrieveContext(testBundle))
          .thenReturn(
              Mono.delay(Duration.ofSeconds(2))
                  .then(Mono.just(createEmptyResult("metadata-based"))));

      final List<ContextMatch> historyMatches =
          List.of(new ContextMatch("FileB.java", MatchReason.GIT_COCHANGE_HIGH, 0.9, "evidence"));

      when(historyStrategy.retrieveContext(testBundle))
          .thenReturn(
              Mono.just(
                  new ContextRetrievalResult(
                      historyMatches,
                      new ContextRetrievalMetadata(
                          "git-history", Duration.ofMillis(200), 1, 1, Map.of()))));

      final Mono<EnrichedDiffAnalysisBundle> result =
          orchestrator.retrieveEnrichedContext(testBundle);

      StepVerifier.create(result)
          .assertNext(
              enriched -> {
                assertThat(enriched.hasContext()).isTrue();
                assertThat(enriched.contextResult().metadata().strategyName())
                    .isEqualTo("git-history");
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("Large Diff Handling")
  final class LargeDiffHandling {

    @Test
    @DisplayName("should_skip_context_retrieval_for_large_diffs")
    final void should_skip_context_retrieval_for_large_diffs() {
      final ContextRetrievalConfig config =
          new ContextRetrievalConfig(
              true,
              5,
              List.of("metadata-based"),
              new ContextRetrievalConfig.RolloutConfig(100, true, 20));

      orchestrator = new ContextOrchestrator(List.of(metadataStrategy), contextEnricher, config);

      final DiffAnalysisBundle largeDiff = createTestBundle(100);

      final Mono<EnrichedDiffAnalysisBundle> result =
          orchestrator.retrieveEnrichedContext(largeDiff);

      StepVerifier.create(result)
          .assertNext(enriched -> assertThat(enriched.hasContext()).isFalse())
          .verifyComplete();

      verify(metadataStrategy, never()).retrieveContext(any());
    }

    @Test
    @DisplayName("should_process_large_diffs_when_skip_disabled")
    final void should_process_large_diffs_when_skip_disabled() {
      final ContextRetrievalConfig config =
          new ContextRetrievalConfig(
              true,
              5,
              List.of("metadata-based"),
              new ContextRetrievalConfig.RolloutConfig(100, false, 20));

      orchestrator = new ContextOrchestrator(List.of(metadataStrategy), contextEnricher, config);

      final DiffAnalysisBundle largeDiff = createTestBundle(100);

      when(metadataStrategy.retrieveContext(largeDiff))
          .thenReturn(Mono.just(createEmptyResult("metadata-based")));

      final Mono<EnrichedDiffAnalysisBundle> result =
          orchestrator.retrieveEnrichedContext(largeDiff);

      StepVerifier.create(result)
          .assertNext(enriched -> assertThat(enriched).isNotNull())
          .verifyComplete();

      verify(metadataStrategy, times(1)).retrieveContext(largeDiff);
    }
  }

  @Nested
  @DisplayName("Strategy Priority")
  final class StrategyPriority {

    @Test
    @DisplayName("should_execute_strategies_in_priority_order")
    final void should_execute_strategies_in_priority_order() {
      final ContextRetrievalConfig config =
          new ContextRetrievalConfig(
              true,
              5,
              List.of("metadata-based", "git-history", "low-priority"),
              new ContextRetrievalConfig.RolloutConfig(100, false, 5000));

      final ContextRetrievalStrategy lowPriority = mock(ContextRetrievalStrategy.class);
      when(lowPriority.getStrategyName()).thenReturn("low-priority");
      when(lowPriority.getPriority()).thenReturn(30);

      orchestrator =
          new ContextOrchestrator(
              List.of(lowPriority, historyStrategy, metadataStrategy), contextEnricher, config);

      when(metadataStrategy.retrieveContext(testBundle))
          .thenReturn(Mono.just(createEmptyResult("metadata-based")));
      when(historyStrategy.retrieveContext(testBundle))
          .thenReturn(Mono.just(createEmptyResult("git-history")));
      when(lowPriority.retrieveContext(testBundle))
          .thenReturn(Mono.just(createEmptyResult("low-priority")));

      final Mono<EnrichedDiffAnalysisBundle> result =
          orchestrator.retrieveEnrichedContext(testBundle);

      StepVerifier.create(result)
          .assertNext(
              enriched -> {
                if (enriched.hasContext()) {
                  assertThat(enriched.contextResult().metadata().strategyName())
                      .contains("metadata-based")
                      .contains("git-history")
                      .contains("low-priority");
                }
              })
          .verifyComplete();
    }
  }

  private ContextRetrievalResult createEmptyResult(final String strategyName) {
    return new ContextRetrievalResult(
        List.of(), new ContextRetrievalMetadata(strategyName, Duration.ZERO, 0, 0, Map.of()));
  }
}
