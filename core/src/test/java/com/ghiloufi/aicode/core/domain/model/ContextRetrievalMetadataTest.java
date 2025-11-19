package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextRetrievalMetadataTest {

  @Test
  void should_create_valid_metadata() {
    final Map<MatchReason, Integer> distribution =
        Map.of(
            MatchReason.DIRECT_IMPORT, 5,
            MatchReason.GIT_COCHANGE_HIGH, 3);

    final ContextRetrievalMetadata metadata =
        new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 10, 8, distribution);

    assertThat(metadata.strategyName()).isEqualTo("metadata");
    assertThat(metadata.executionTime()).isEqualTo(Duration.ofMillis(200));
    assertThat(metadata.totalCandidates()).isEqualTo(10);
    assertThat(metadata.highConfidenceCount()).isEqualTo(8);
    assertThat(metadata.reasonDistribution()).hasSize(2);
  }

  @Test
  void should_calculate_high_confidence_percentage() {
    final ContextRetrievalMetadata metadata =
        new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 10, 8, Map.of());

    assertThat(metadata.getHighConfidencePercentage()).isEqualTo(80.0);
  }

  @Test
  void should_return_zero_percentage_when_no_candidates() {
    final ContextRetrievalMetadata metadata =
        new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 0, 0, Map.of());

    assertThat(metadata.getHighConfidencePercentage()).isEqualTo(0.0);
  }

  @Test
  void should_throw_when_strategy_name_is_null() {
    assertThatThrownBy(
            () -> new ContextRetrievalMetadata(null, Duration.ofMillis(200), 10, 8, Map.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Strategy name cannot be null");
  }

  @Test
  void should_throw_when_strategy_name_is_blank() {
    assertThatThrownBy(
            () -> new ContextRetrievalMetadata("", Duration.ofMillis(200), 10, 8, Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Strategy name cannot be blank");
  }

  @Test
  void should_throw_when_execution_time_is_null() {
    assertThatThrownBy(() -> new ContextRetrievalMetadata("metadata", null, 10, 8, Map.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Execution time cannot be null");
  }

  @Test
  void should_throw_when_reason_distribution_is_null() {
    assertThatThrownBy(
            () -> new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 10, 8, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Reason distribution cannot be null");
  }

  @Test
  void should_throw_when_total_candidates_is_negative() {
    assertThatThrownBy(
            () -> new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), -1, 0, Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Total candidates cannot be negative");
  }

  @Test
  void should_throw_when_high_confidence_count_is_negative() {
    assertThatThrownBy(
            () ->
                new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 10, -1, Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("High confidence count cannot be negative");
  }

  @Test
  void should_throw_when_high_confidence_exceeds_total() {
    assertThatThrownBy(
            () ->
                new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 10, 15, Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("High confidence count cannot exceed total candidates");
  }

  @Test
  void should_accept_equal_high_confidence_and_total() {
    final ContextRetrievalMetadata metadata =
        new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 10, 10, Map.of());

    assertThat(metadata.totalCandidates()).isEqualTo(10);
    assertThat(metadata.highConfidenceCount()).isEqualTo(10);
    assertThat(metadata.getHighConfidencePercentage()).isEqualTo(100.0);
  }
}
