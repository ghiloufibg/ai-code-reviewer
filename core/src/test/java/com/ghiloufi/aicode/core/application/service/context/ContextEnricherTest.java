package com.ghiloufi.aicode.core.application.service.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalMetadata;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ContextEnricher Tests")
final class ContextEnricherTest {

  private ContextEnricher contextEnricher;
  private DiffAnalysisBundle testBundle;

  @BeforeEach
  final void setUp() {
    contextEnricher = new ContextEnricher();

    final RepositoryIdentifier repo =
        RepositoryIdentifier.create(SourceProvider.GITLAB, "test-org/test-repo");
    final GitFileModification modification =
        new GitFileModification("src/Test.java", "src/Test.java");
    final GitDiffDocument gitDiff = new GitDiffDocument(List.of(modification));

    testBundle = new DiffAnalysisBundle(repo, gitDiff, "diff content", null, null);
  }

  @Nested
  @DisplayName("Empty Results Handling")
  final class EmptyResultsHandling {

    @Test
    @DisplayName("should_return_basic_bundle_when_no_strategy_results")
    final void should_return_basic_bundle_when_no_strategy_results() {
      final EnrichedDiffAnalysisBundle result = contextEnricher.mergeResults(testBundle, List.of());

      assertThat(result.hasContext()).isFalse();
      assertThat(result.repositoryIdentifier()).isEqualTo(testBundle.repositoryIdentifier());
      assertThat(result.structuredDiff()).isEqualTo(testBundle.structuredDiff());
    }
  }

  @Nested
  @DisplayName("Match Deduplication")
  final class MatchDeduplication {

    @Test
    @DisplayName("should_deduplicate_matches_keeping_highest_confidence")
    final void should_deduplicate_matches_keeping_highest_confidence() {
      final List<ContextMatch> metadataMatches =
          List.of(
              new ContextMatch("FileA.java", MatchReason.DIRECT_IMPORT, 0.7, "evidence1"),
              new ContextMatch("FileB.java", MatchReason.SIBLING_FILE, 0.6, "evidence2"));

      final List<ContextMatch> historyMatches =
          List.of(
              new ContextMatch("FileA.java", MatchReason.GIT_COCHANGE_HIGH, 0.9, "evidence3"),
              new ContextMatch("FileC.java", MatchReason.GIT_COCHANGE_HIGH, 0.8, "evidence4"));

      final ContextRetrievalMetadata metadata1 =
          new ContextRetrievalMetadata("metadata-based", Duration.ofMillis(100), 2, 1, Map.of());
      final ContextRetrievalMetadata metadata2 =
          new ContextRetrievalMetadata("git-history", Duration.ofMillis(200), 2, 2, Map.of());

      final List<ContextRetrievalResult> results =
          List.of(
              new ContextRetrievalResult(metadataMatches, metadata1),
              new ContextRetrievalResult(historyMatches, metadata2));

      final EnrichedDiffAnalysisBundle enriched = contextEnricher.mergeResults(testBundle, results);

      assertThat(enriched.hasContext()).isTrue();
      assertThat(enriched.getContextMatchCount()).isEqualTo(3);

      final List<ContextMatch> finalMatches = enriched.contextResult().matches();

      final ContextMatch fileA =
          finalMatches.stream().filter(m -> m.filePath().equals("FileA.java")).findFirst().get();

      assertThat(fileA.confidence()).isEqualTo(0.9);
      assertThat(fileA.reason()).isEqualTo(MatchReason.GIT_COCHANGE_HIGH);
    }

    @Test
    @DisplayName("should_keep_all_unique_files")
    final void should_keep_all_unique_files() {
      final List<ContextMatch> metadataMatches =
          List.of(
              new ContextMatch("FileA.java", MatchReason.DIRECT_IMPORT, 0.7, "evidence1"),
              new ContextMatch("FileB.java", MatchReason.SIBLING_FILE, 0.6, "evidence2"));

      final List<ContextMatch> historyMatches =
          List.of(new ContextMatch("FileC.java", MatchReason.GIT_COCHANGE_HIGH, 0.8, "evidence3"));

      final ContextRetrievalMetadata metadata1 =
          new ContextRetrievalMetadata("metadata-based", Duration.ofMillis(100), 2, 1, Map.of());
      final ContextRetrievalMetadata metadata2 =
          new ContextRetrievalMetadata("git-history", Duration.ofMillis(200), 1, 1, Map.of());

      final List<ContextRetrievalResult> results =
          List.of(
              new ContextRetrievalResult(metadataMatches, metadata1),
              new ContextRetrievalResult(historyMatches, metadata2));

      final EnrichedDiffAnalysisBundle enriched = contextEnricher.mergeResults(testBundle, results);

      assertThat(enriched.getContextMatchCount()).isEqualTo(3);

      final List<String> filePaths =
          enriched.contextResult().matches().stream().map(ContextMatch::filePath).toList();

      assertThat(filePaths).containsExactlyInAnyOrder("FileA.java", "FileB.java", "FileC.java");
    }

    @Test
    @DisplayName("should_sort_matches_by_confidence_descending")
    final void should_sort_matches_by_confidence_descending() {
      final List<ContextMatch> matches =
          List.of(
              new ContextMatch("FileA.java", MatchReason.DIRECT_IMPORT, 0.5, "evidence1"),
              new ContextMatch("FileB.java", MatchReason.GIT_COCHANGE_HIGH, 0.9, "evidence2"),
              new ContextMatch("FileC.java", MatchReason.SIBLING_FILE, 0.7, "evidence3"));

      final ContextRetrievalMetadata metadata =
          new ContextRetrievalMetadata("test", Duration.ofMillis(100), 3, 1, Map.of());

      final List<ContextRetrievalResult> results =
          List.of(new ContextRetrievalResult(matches, metadata));

      final EnrichedDiffAnalysisBundle enriched = contextEnricher.mergeResults(testBundle, results);

      final List<Double> confidences =
          enriched.contextResult().matches().stream().map(ContextMatch::confidence).toList();

      assertThat(confidences).containsExactly(0.9, 0.7, 0.5);
    }
  }

  @Nested
  @DisplayName("Metadata Merging")
  final class MetadataMerging {

    @Test
    @DisplayName("should_combine_strategy_names")
    final void should_combine_strategy_names() {
      final ContextRetrievalMetadata metadata1 =
          new ContextRetrievalMetadata("metadata-based", Duration.ofMillis(100), 2, 1, Map.of());
      final ContextRetrievalMetadata metadata2 =
          new ContextRetrievalMetadata("git-history", Duration.ofMillis(200), 3, 2, Map.of());

      final List<ContextRetrievalResult> results =
          List.of(
              new ContextRetrievalResult(List.of(), metadata1),
              new ContextRetrievalResult(List.of(), metadata2));

      final EnrichedDiffAnalysisBundle enriched = contextEnricher.mergeResults(testBundle, results);

      assertThat(enriched.contextResult().metadata().strategyName())
          .isEqualTo("metadata-based+git-history");
    }

    @Test
    @DisplayName("should_sum_execution_times")
    final void should_sum_execution_times() {
      final ContextRetrievalMetadata metadata1 =
          new ContextRetrievalMetadata("metadata-based", Duration.ofMillis(100), 2, 1, Map.of());
      final ContextRetrievalMetadata metadata2 =
          new ContextRetrievalMetadata("git-history", Duration.ofMillis(250), 3, 2, Map.of());

      final List<ContextRetrievalResult> results =
          List.of(
              new ContextRetrievalResult(List.of(), metadata1),
              new ContextRetrievalResult(List.of(), metadata2));

      final EnrichedDiffAnalysisBundle enriched = contextEnricher.mergeResults(testBundle, results);

      assertThat(enriched.contextResult().metadata().executionTime())
          .isEqualTo(Duration.ofMillis(350));
    }

    @Test
    @DisplayName("should_sum_total_candidates")
    final void should_sum_total_candidates() {
      final ContextRetrievalMetadata metadata1 =
          new ContextRetrievalMetadata("metadata-based", Duration.ofMillis(100), 8, 5, Map.of());
      final ContextRetrievalMetadata metadata2 =
          new ContextRetrievalMetadata("git-history", Duration.ofMillis(200), 12, 4, Map.of());

      final List<ContextRetrievalResult> results =
          List.of(
              new ContextRetrievalResult(List.of(), metadata1),
              new ContextRetrievalResult(List.of(), metadata2));

      final EnrichedDiffAnalysisBundle enriched = contextEnricher.mergeResults(testBundle, results);

      assertThat(enriched.contextResult().metadata().totalCandidates()).isEqualTo(20);
    }

    @Test
    @DisplayName("should_sum_high_confidence_counts")
    final void should_sum_high_confidence_counts() {
      final ContextRetrievalMetadata metadata1 =
          new ContextRetrievalMetadata("metadata-based", Duration.ofMillis(100), 8, 5, Map.of());
      final ContextRetrievalMetadata metadata2 =
          new ContextRetrievalMetadata("git-history", Duration.ofMillis(200), 12, 4, Map.of());

      final List<ContextRetrievalResult> results =
          List.of(
              new ContextRetrievalResult(List.of(), metadata1),
              new ContextRetrievalResult(List.of(), metadata2));

      final EnrichedDiffAnalysisBundle enriched = contextEnricher.mergeResults(testBundle, results);

      assertThat(enriched.contextResult().metadata().highConfidenceCount()).isEqualTo(9);
    }

    @Test
    @DisplayName("should_merge_reason_distributions")
    final void should_merge_reason_distributions() {
      final Map<MatchReason, Integer> reasons1 =
          Map.of(MatchReason.DIRECT_IMPORT, 3, MatchReason.SIBLING_FILE, 2);

      final Map<MatchReason, Integer> reasons2 =
          Map.of(MatchReason.GIT_COCHANGE_HIGH, 5, MatchReason.DIRECT_IMPORT, 2);

      final ContextRetrievalMetadata metadata1 =
          new ContextRetrievalMetadata("metadata-based", Duration.ofMillis(100), 5, 3, reasons1);
      final ContextRetrievalMetadata metadata2 =
          new ContextRetrievalMetadata("git-history", Duration.ofMillis(200), 7, 5, reasons2);

      final List<ContextRetrievalResult> results =
          List.of(
              new ContextRetrievalResult(List.of(), metadata1),
              new ContextRetrievalResult(List.of(), metadata2));

      final EnrichedDiffAnalysisBundle enriched = contextEnricher.mergeResults(testBundle, results);

      final Map<MatchReason, Integer> combinedReasons =
          enriched.contextResult().metadata().reasonDistribution();

      assertThat(combinedReasons).containsEntry(MatchReason.DIRECT_IMPORT, 5);
      assertThat(combinedReasons).containsEntry(MatchReason.SIBLING_FILE, 2);
      assertThat(combinedReasons).containsEntry(MatchReason.GIT_COCHANGE_HIGH, 5);
    }
  }

  @Nested
  @DisplayName("Single Strategy Result")
  final class SingleStrategyResult {

    @Test
    @DisplayName("should_handle_single_strategy_result")
    final void should_handle_single_strategy_result() {
      final List<ContextMatch> matches =
          List.of(new ContextMatch("FileA.java", MatchReason.DIRECT_IMPORT, 0.8, "evidence"));

      final ContextRetrievalMetadata metadata =
          new ContextRetrievalMetadata("metadata-based", Duration.ofMillis(100), 1, 1, Map.of());

      final List<ContextRetrievalResult> results =
          List.of(new ContextRetrievalResult(matches, metadata));

      final EnrichedDiffAnalysisBundle enriched = contextEnricher.mergeResults(testBundle, results);

      assertThat(enriched.hasContext()).isTrue();
      assertThat(enriched.getContextMatchCount()).isEqualTo(1);
      assertThat(enriched.contextResult().metadata().strategyName()).isEqualTo("metadata-based");
    }
  }
}
