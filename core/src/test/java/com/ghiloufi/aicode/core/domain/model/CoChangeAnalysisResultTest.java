package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CoChangeAnalysisResult Tests")
final class CoChangeAnalysisResultTest {

  @Nested
  @DisplayName("Construction")
  final class Construction {

    @Test
    @DisplayName("should_create_result_with_valid_parameters")
    final void should_create_result_with_valid_parameters() {
      final List<CoChangeMetrics> metrics =
          List.of(
              new CoChangeMetrics("src/UserRepository.java", 15, 0.75),
              new CoChangeMetrics("src/UserController.java", 10, 0.50));

      final Map<String, Integer> fileFrequency =
          Map.of("src/UserRepository.java", 15, "src/UserController.java", 10);

      final CoChangeAnalysisResult result =
          new CoChangeAnalysisResult("src/UserService.java", metrics, fileFrequency, 20);

      assertThat(result.targetFile()).isEqualTo("src/UserService.java");
      assertThat(result.coChangeMetrics()).hasSize(2);
      assertThat(result.rawFrequencyMap()).hasSize(2);
      assertThat(result.maxFrequency()).isEqualTo(20);
    }

    @Test
    @DisplayName("should_reject_null_target_file")
    final void should_reject_null_target_file() {
      assertThatThrownBy(() -> new CoChangeAnalysisResult(null, List.of(), Map.of(), 10))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Target file cannot be null");
    }

    @Test
    @DisplayName("should_reject_blank_target_file")
    final void should_reject_blank_target_file() {
      assertThatThrownBy(() -> new CoChangeAnalysisResult("  ", List.of(), Map.of(), 10))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Target file cannot be blank");
    }

    @Test
    @DisplayName("should_reject_null_cochange_metrics")
    final void should_reject_null_cochange_metrics() {
      assertThatThrownBy(() -> new CoChangeAnalysisResult("src/User.java", null, Map.of(), 10))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Co-change metrics cannot be null");
    }

    @Test
    @DisplayName("should_reject_null_frequency_map")
    final void should_reject_null_frequency_map() {
      assertThatThrownBy(() -> new CoChangeAnalysisResult("src/User.java", List.of(), null, 10))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Raw frequency map cannot be null");
    }

    @Test
    @DisplayName("should_reject_negative_max_frequency")
    final void should_reject_negative_max_frequency() {
      assertThatThrownBy(() -> new CoChangeAnalysisResult("src/User.java", List.of(), Map.of(), -1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max frequency cannot be negative");
    }

    @Test
    @DisplayName("should_accept_empty_metrics_and_frequency_map")
    final void should_accept_empty_metrics_and_frequency_map() {
      final CoChangeAnalysisResult result =
          new CoChangeAnalysisResult("src/User.java", List.of(), Map.of(), 0);

      assertThat(result.coChangeMetrics()).isEmpty();
      assertThat(result.rawFrequencyMap()).isEmpty();
      assertThat(result.maxFrequency()).isZero();
    }
  }

  @Nested
  @DisplayName("Query Operations")
  final class QueryOperations {

    @Test
    @DisplayName("should_get_high_confidence_matches")
    final void should_get_high_confidence_matches() {
      final List<CoChangeMetrics> metrics =
          List.of(
              new CoChangeMetrics("src/HighFreq.java", 18, 0.90),
              new CoChangeMetrics("src/MediumFreq.java", 10, 0.50),
              new CoChangeMetrics("src/LowFreq.java", 2, 0.10));

      final CoChangeAnalysisResult result =
          new CoChangeAnalysisResult("src/Target.java", metrics, Map.of(), 20);

      final List<CoChangeMetrics> highConfidence = result.getHighConfidenceMatches();

      assertThat(highConfidence).hasSize(1);
      assertThat(highConfidence.get(0).filePath()).isEqualTo("src/HighFreq.java");
    }

    @Test
    @DisplayName("should_get_medium_confidence_matches")
    final void should_get_medium_confidence_matches() {
      final List<CoChangeMetrics> metrics =
          List.of(
              new CoChangeMetrics("src/HighFreq.java", 18, 0.90),
              new CoChangeMetrics("src/MediumFreq.java", 10, 0.50),
              new CoChangeMetrics("src/LowFreq.java", 2, 0.10));

      final CoChangeAnalysisResult result =
          new CoChangeAnalysisResult("src/Target.java", metrics, Map.of(), 20);

      final List<CoChangeMetrics> mediumConfidence = result.getMediumConfidenceMatches();

      assertThat(mediumConfidence).hasSize(1);
      assertThat(mediumConfidence.get(0).filePath()).isEqualTo("src/MediumFreq.java");
    }

    @Test
    @DisplayName("should_convert_to_context_matches")
    final void should_convert_to_context_matches() {
      final List<CoChangeMetrics> metrics =
          List.of(
              new CoChangeMetrics("src/UserRepository.java", 15, 0.80),
              new CoChangeMetrics("src/UserController.java", 8, 0.45));

      final CoChangeAnalysisResult result =
          new CoChangeAnalysisResult("src/UserService.java", metrics, Map.of(), 20);

      final List<ContextMatch> contextMatches = result.toContextMatches();

      assertThat(contextMatches).hasSize(2);
      assertThat(contextMatches.get(0).filePath()).isEqualTo("src/UserRepository.java");
      assertThat(contextMatches.get(0).reason()).isEqualTo(MatchReason.GIT_COCHANGE_HIGH);
      assertThat(contextMatches.get(1).filePath()).isEqualTo("src/UserController.java");
      assertThat(contextMatches.get(1).reason()).isEqualTo(MatchReason.GIT_COCHANGE_MEDIUM);
    }

    @Test
    @DisplayName("should_get_total_analyzed_files")
    final void should_get_total_analyzed_files() {
      final List<CoChangeMetrics> metrics =
          List.of(
              new CoChangeMetrics("src/A.java", 10, 0.50),
              new CoChangeMetrics("src/B.java", 8, 0.40),
              new CoChangeMetrics("src/C.java", 5, 0.25));

      final CoChangeAnalysisResult result =
          new CoChangeAnalysisResult("src/Target.java", metrics, Map.of(), 20);

      assertThat(result.getTotalAnalyzedFiles()).isEqualTo(3);
    }

    @Test
    @DisplayName("should_check_if_file_was_analyzed")
    final void should_check_if_file_was_analyzed() {
      final Map<String, Integer> frequencyMap =
          Map.of("src/UserRepository.java", 15, "src/UserController.java", 10);

      final CoChangeAnalysisResult result =
          new CoChangeAnalysisResult("src/UserService.java", List.of(), frequencyMap, 20);

      assertThat(result.wasFileAnalyzed("src/UserRepository.java")).isTrue();
      assertThat(result.wasFileAnalyzed("src/NotAnalyzed.java")).isFalse();
    }

    @Test
    @DisplayName("should_get_frequency_for_file")
    final void should_get_frequency_for_file() {
      final Map<String, Integer> frequencyMap =
          Map.of("src/UserRepository.java", 15, "src/UserController.java", 10);

      final CoChangeAnalysisResult result =
          new CoChangeAnalysisResult("src/UserService.java", List.of(), frequencyMap, 20);

      assertThat(result.getFrequencyFor("src/UserRepository.java")).isEqualTo(15);
      assertThat(result.getFrequencyFor("src/NotFound.java")).isZero();
    }
  }

  @Nested
  @DisplayName("Statistics")
  final class Statistics {

    @Test
    @DisplayName("should_calculate_average_frequency")
    final void should_calculate_average_frequency() {
      final List<CoChangeMetrics> metrics =
          List.of(
              new CoChangeMetrics("src/A.java", 20, 1.0),
              new CoChangeMetrics("src/B.java", 10, 0.5),
              new CoChangeMetrics("src/C.java", 0, 0.0));

      final CoChangeAnalysisResult result =
          new CoChangeAnalysisResult("src/Target.java", metrics, Map.of(), 20);

      assertThat(result.getAverageFrequency()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("should_return_zero_average_for_empty_metrics")
    final void should_return_zero_average_for_empty_metrics() {
      final CoChangeAnalysisResult result =
          new CoChangeAnalysisResult("src/Target.java", List.of(), Map.of(), 0);

      assertThat(result.getAverageFrequency()).isZero();
    }

    @Test
    @DisplayName("should_check_if_result_has_matches")
    final void should_check_if_result_has_matches() {
      final CoChangeAnalysisResult emptyResult =
          new CoChangeAnalysisResult("src/Target.java", List.of(), Map.of(), 0);

      final CoChangeAnalysisResult nonEmptyResult =
          new CoChangeAnalysisResult(
              "src/Target.java",
              List.of(new CoChangeMetrics("src/Other.java", 5, 0.5)),
              Map.of(),
              10);

      assertThat(emptyResult.hasMatches()).isFalse();
      assertThat(nonEmptyResult.hasMatches()).isTrue();
    }
  }
}
