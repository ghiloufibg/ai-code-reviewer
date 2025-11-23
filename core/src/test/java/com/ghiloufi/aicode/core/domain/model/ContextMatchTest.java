package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ContextMatchTest {

  @Test
  void should_create_valid_context_match() {
    final ContextMatch match =
        new ContextMatch(
            "UserService.java", MatchReason.DIRECT_IMPORT, 0.95, "Import: com.example.UserService");

    assertThat(match.filePath()).isEqualTo("UserService.java");
    assertThat(match.reason()).isEqualTo(MatchReason.DIRECT_IMPORT);
    assertThat(match.confidence()).isEqualTo(0.95);
    assertThat(match.evidence()).isEqualTo("Import: com.example.UserService");
  }

  @Test
  void should_identify_high_confidence_match() {
    final ContextMatch highConfidence =
        new ContextMatch("file.java", MatchReason.DIRECT_IMPORT, 0.75, "evidence");

    assertThat(highConfidence.isHighConfidence()).isTrue();
  }

  @Test
  void should_identify_low_confidence_match() {
    final ContextMatch lowConfidence =
        new ContextMatch("file.java", MatchReason.PARENT_PACKAGE, 0.74, "evidence");

    assertThat(lowConfidence.isHighConfidence()).isFalse();
  }

  @Test
  void should_throw_when_file_path_is_null() {
    assertThatThrownBy(() -> new ContextMatch(null, MatchReason.DIRECT_IMPORT, 0.95, "evidence"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("File path cannot be null");
  }

  @Test
  void should_throw_when_file_path_is_blank() {
    assertThatThrownBy(() -> new ContextMatch("", MatchReason.DIRECT_IMPORT, 0.95, "evidence"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("File path cannot be blank");
  }

  @Test
  void should_throw_when_reason_is_null() {
    assertThatThrownBy(() -> new ContextMatch("file.java", null, 0.95, "evidence"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Reason cannot be null");
  }

  @Test
  void should_throw_when_evidence_is_null() {
    assertThatThrownBy(() -> new ContextMatch("file.java", MatchReason.DIRECT_IMPORT, 0.95, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Evidence cannot be null");
  }

  @Test
  void should_throw_when_confidence_is_negative() {
    assertThatThrownBy(
            () -> new ContextMatch("file.java", MatchReason.DIRECT_IMPORT, -0.1, "evidence"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Confidence must be between 0.0 and 1.0");
  }

  @Test
  void should_throw_when_confidence_exceeds_one() {
    assertThatThrownBy(
            () -> new ContextMatch("file.java", MatchReason.DIRECT_IMPORT, 1.1, "evidence"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Confidence must be between 0.0 and 1.0");
  }

  @Test
  void should_accept_boundary_confidence_values() {
    final ContextMatch minConfidence =
        new ContextMatch("file.java", MatchReason.DIRECT_IMPORT, 0.0, "evidence");
    final ContextMatch maxConfidence =
        new ContextMatch("file.java", MatchReason.DIRECT_IMPORT, 1.0, "evidence");

    assertThat(minConfidence.confidence()).isEqualTo(0.0);
    assertThat(maxConfidence.confidence()).isEqualTo(1.0);
  }
}
