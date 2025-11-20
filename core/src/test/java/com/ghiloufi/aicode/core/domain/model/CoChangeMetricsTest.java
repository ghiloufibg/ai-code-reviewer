package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CoChangeMetrics Tests")
final class CoChangeMetricsTest {

  @Nested
  @DisplayName("Construction")
  final class Construction {

    @Test
    @DisplayName("should_create_metrics_with_valid_parameters")
    final void should_create_metrics_with_valid_parameters() {
      final CoChangeMetrics metrics = new CoChangeMetrics("src/User.java", 15, 0.75);

      assertThat(metrics.filePath()).isEqualTo("src/User.java");
      assertThat(metrics.coChangeCount()).isEqualTo(15);
      assertThat(metrics.normalizedFrequency()).isEqualTo(0.75);
    }

    @Test
    @DisplayName("should_reject_null_file_path")
    final void should_reject_null_file_path() {
      assertThatThrownBy(() -> new CoChangeMetrics(null, 10, 0.5))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("File path cannot be null");
    }

    @Test
    @DisplayName("should_reject_blank_file_path")
    final void should_reject_blank_file_path() {
      assertThatThrownBy(() -> new CoChangeMetrics("  ", 10, 0.5))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("File path cannot be blank");
    }

    @Test
    @DisplayName("should_reject_negative_cochange_count")
    final void should_reject_negative_cochange_count() {
      assertThatThrownBy(() -> new CoChangeMetrics("src/User.java", -1, 0.5))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Co-change count cannot be negative");
    }

    @Test
    @DisplayName("should_reject_normalized_frequency_below_zero")
    final void should_reject_normalized_frequency_below_zero() {
      assertThatThrownBy(() -> new CoChangeMetrics("src/User.java", 10, -0.1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Normalized frequency must be between 0.0 and 1.0");
    }

    @Test
    @DisplayName("should_reject_normalized_frequency_above_one")
    final void should_reject_normalized_frequency_above_one() {
      assertThatThrownBy(() -> new CoChangeMetrics("src/User.java", 10, 1.5))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Normalized frequency must be between 0.0 and 1.0");
    }

    @Test
    @DisplayName("should_accept_zero_cochange_count")
    final void should_accept_zero_cochange_count() {
      final CoChangeMetrics metrics = new CoChangeMetrics("src/User.java", 0, 0.0);

      assertThat(metrics.coChangeCount()).isZero();
      assertThat(metrics.normalizedFrequency()).isZero();
    }

    @Test
    @DisplayName("should_accept_boundary_normalized_frequency_values")
    final void should_accept_boundary_normalized_frequency_values() {
      final CoChangeMetrics metricsZero = new CoChangeMetrics("src/User.java", 5, 0.0);
      final CoChangeMetrics metricsOne = new CoChangeMetrics("src/Other.java", 10, 1.0);

      assertThat(metricsZero.normalizedFrequency()).isZero();
      assertThat(metricsOne.normalizedFrequency()).isEqualTo(1.0);
    }
  }

  @Nested
  @DisplayName("Business Logic")
  final class BusinessLogic {

    @Test
    @DisplayName("should_identify_high_frequency_cochange")
    final void should_identify_high_frequency_cochange() {
      final CoChangeMetrics metrics = new CoChangeMetrics("src/User.java", 15, 0.85);

      assertThat(metrics.isHighFrequency()).isTrue();
    }

    @Test
    @DisplayName("should_identify_medium_frequency_cochange")
    final void should_identify_medium_frequency_cochange() {
      final CoChangeMetrics metrics = new CoChangeMetrics("src/User.java", 8, 0.55);

      assertThat(metrics.isMediumFrequency()).isTrue();
      assertThat(metrics.isHighFrequency()).isFalse();
    }

    @Test
    @DisplayName("should_identify_low_frequency_cochange")
    final void should_identify_low_frequency_cochange() {
      final CoChangeMetrics metrics = new CoChangeMetrics("src/User.java", 2, 0.25);

      assertThat(metrics.isHighFrequency()).isFalse();
      assertThat(metrics.isMediumFrequency()).isFalse();
    }

    @Test
    @DisplayName("should_determine_match_reason_for_high_frequency")
    final void should_determine_match_reason_for_high_frequency() {
      final CoChangeMetrics metrics = new CoChangeMetrics("src/User.java", 18, 0.80);

      assertThat(metrics.getMatchReason()).isEqualTo(MatchReason.GIT_COCHANGE_HIGH);
    }

    @Test
    @DisplayName("should_determine_match_reason_for_medium_frequency")
    final void should_determine_match_reason_for_medium_frequency() {
      final CoChangeMetrics metrics = new CoChangeMetrics("src/User.java", 10, 0.60);

      assertThat(metrics.getMatchReason()).isEqualTo(MatchReason.GIT_COCHANGE_MEDIUM);
    }

    @Test
    @DisplayName("should_calculate_confidence_score_for_high_frequency")
    final void should_calculate_confidence_score_for_high_frequency() {
      final CoChangeMetrics metrics = new CoChangeMetrics("src/User.java", 20, 0.90);

      final double confidence = metrics.calculateConfidence();

      assertThat(confidence).isGreaterThanOrEqualTo(0.75);
      assertThat(confidence).isLessThanOrEqualTo(0.85);
    }

    @Test
    @DisplayName("should_calculate_confidence_score_for_medium_frequency")
    final void should_calculate_confidence_score_for_medium_frequency() {
      final CoChangeMetrics metrics = new CoChangeMetrics("src/User.java", 10, 0.50);

      final double confidence = metrics.calculateConfidence();

      assertThat(confidence).isGreaterThanOrEqualTo(0.35);
      assertThat(confidence).isLessThanOrEqualTo(0.70);
    }

    @Test
    @DisplayName("should_format_evidence_string")
    final void should_format_evidence_string() {
      final CoChangeMetrics metrics = new CoChangeMetrics("src/User.java", 12, 0.65);

      final String evidence = metrics.formatEvidence();

      assertThat(evidence).contains("12");
      assertThat(evidence).containsIgnoringCase("co-changed");
    }
  }
}
