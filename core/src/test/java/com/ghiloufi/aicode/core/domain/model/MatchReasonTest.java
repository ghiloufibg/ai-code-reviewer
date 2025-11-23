package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MatchReasonTest {

  @Test
  void should_have_valid_confidence_scores() {
    for (final MatchReason reason : MatchReason.values()) {
      assertThat(reason.getBaseConfidence())
          .as("Confidence for %s should be between 0.0 and 1.0", reason)
          .isBetween(0.0, 1.0);
    }
  }

  @Test
  void should_have_non_blank_descriptions() {
    for (final MatchReason reason : MatchReason.values()) {
      assertThat(reason.getDescription())
          .as("Description for %s should not be blank", reason)
          .isNotBlank();
    }
  }

  @Test
  void should_have_direct_import_with_highest_confidence() {
    assertThat(MatchReason.DIRECT_IMPORT.getBaseConfidence())
        .isEqualTo(0.95)
        .as("DIRECT_IMPORT should have highest confidence");
  }

  @Test
  void should_have_parent_package_with_lowest_confidence() {
    final double minConfidence = 0.60;
    assertThat(MatchReason.PARENT_PACKAGE.getBaseConfidence())
        .isEqualTo(minConfidence)
        .as("PARENT_PACKAGE should have minimum confidence threshold");
  }

  @Test
  void should_have_git_cochange_high_greater_than_medium() {
    assertThat(MatchReason.GIT_COCHANGE_HIGH.getBaseConfidence())
        .isGreaterThan(MatchReason.GIT_COCHANGE_MEDIUM.getBaseConfidence())
        .as("GIT_COCHANGE_HIGH should have higher confidence than GIT_COCHANGE_MEDIUM");
  }

  @Test
  void should_have_test_counterpart_with_high_confidence() {
    assertThat(MatchReason.TEST_COUNTERPART.getBaseConfidence())
        .isEqualTo(0.85)
        .as("TEST_COUNTERPART should have high confidence for strong relationship");
  }
}
