package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextRetrievalResultTest {

  @Test
  void should_create_valid_result() {
    final List<ContextMatch> matches =
        List.of(
            new ContextMatch("File1.java", MatchReason.DIRECT_IMPORT, 0.95, "evidence1"),
            new ContextMatch("File2.java", MatchReason.GIT_COCHANGE_HIGH, 0.85, "evidence2"));

    final ContextRetrievalMetadata metadata =
        new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 2, 2, Map.of());

    final ContextRetrievalResult result = new ContextRetrievalResult(matches, metadata);

    assertThat(result.matches()).hasSize(2);
    assertThat(result.metadata()).isEqualTo(metadata);
  }

  @Test
  void should_return_total_matches_count() {
    final List<ContextMatch> matches =
        List.of(
            new ContextMatch("File1.java", MatchReason.DIRECT_IMPORT, 0.95, "evidence1"),
            new ContextMatch("File2.java", MatchReason.GIT_COCHANGE_HIGH, 0.85, "evidence2"),
            new ContextMatch("File3.java", MatchReason.SAME_PACKAGE, 0.75, "evidence3"));

    final ContextRetrievalResult result =
        new ContextRetrievalResult(
            matches,
            new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 3, 3, Map.of()));

    assertThat(result.getTotalMatches()).isEqualTo(3);
  }

  @Test
  void should_filter_high_confidence_matches() {
    final List<ContextMatch> matches =
        List.of(
            new ContextMatch("High1.java", MatchReason.DIRECT_IMPORT, 0.95, "evidence1"),
            new ContextMatch("Low1.java", MatchReason.PARENT_PACKAGE, 0.60, "evidence2"),
            new ContextMatch("High2.java", MatchReason.GIT_COCHANGE_HIGH, 0.85, "evidence3"),
            new ContextMatch("Low2.java", MatchReason.RELATED_LAYER, 0.70, "evidence4"));

    final ContextRetrievalResult result =
        new ContextRetrievalResult(
            matches,
            new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 4, 2, Map.of()));

    final List<ContextMatch> highConfidence = result.getHighConfidenceMatches();

    assertThat(highConfidence).hasSize(2);
    assertThat(highConfidence)
        .extracting(ContextMatch::filePath)
        .containsExactlyInAnyOrder("High1.java", "High2.java");
  }

  @Test
  void should_filter_matches_by_reason() {
    final List<ContextMatch> matches =
        List.of(
            new ContextMatch("File1.java", MatchReason.DIRECT_IMPORT, 0.95, "evidence1"),
            new ContextMatch("File2.java", MatchReason.GIT_COCHANGE_HIGH, 0.85, "evidence2"),
            new ContextMatch("File3.java", MatchReason.DIRECT_IMPORT, 0.95, "evidence3"));

    final ContextRetrievalResult result =
        new ContextRetrievalResult(
            matches,
            new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 3, 3, Map.of()));

    final List<ContextMatch> importMatches = result.getMatchesByReason(MatchReason.DIRECT_IMPORT);

    assertThat(importMatches).hasSize(2);
    assertThat(importMatches)
        .extracting(ContextMatch::reason)
        .containsOnly(MatchReason.DIRECT_IMPORT);
  }

  @Test
  void should_identify_empty_result() {
    final ContextRetrievalResult emptyResult =
        new ContextRetrievalResult(
            List.of(),
            new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 0, 0, Map.of()));

    assertThat(emptyResult.isEmpty()).isTrue();
    assertThat(emptyResult.getTotalMatches()).isZero();
  }

  @Test
  void should_identify_non_empty_result() {
    final ContextRetrievalResult result =
        new ContextRetrievalResult(
            List.of(new ContextMatch("File.java", MatchReason.DIRECT_IMPORT, 0.95, "evidence")),
            new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 1, 1, Map.of()));

    assertThat(result.isEmpty()).isFalse();
  }

  @Test
  void should_throw_when_matches_is_null() {
    final ContextRetrievalMetadata metadata =
        new ContextRetrievalMetadata("metadata", Duration.ofMillis(200), 0, 0, Map.of());

    assertThatThrownBy(() -> new ContextRetrievalResult(null, metadata))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Matches cannot be null");
  }

  @Test
  void should_throw_when_metadata_is_null() {
    assertThatThrownBy(() -> new ContextRetrievalResult(List.of(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Metadata cannot be null");
  }
}
