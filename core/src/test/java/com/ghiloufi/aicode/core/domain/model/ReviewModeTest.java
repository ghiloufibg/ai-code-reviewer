package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class ReviewModeTest {

  @Test
  void should_have_diff_and_agentic_modes() {
    assertThat(ReviewMode.values()).containsExactly(ReviewMode.DIFF, ReviewMode.AGENTIC);
  }

  @Test
  void should_return_description_for_diff_mode() {
    assertThat(ReviewMode.DIFF.getDescription()).isEqualTo("Diff-only analysis via SCM API");
  }

  @Test
  void should_return_description_for_agentic_mode() {
    assertThat(ReviewMode.AGENTIC.getDescription())
        .isEqualTo("Full repository checkout with static analysis and security scans");
  }

  @Test
  void should_not_require_container_for_diff_mode() {
    assertThat(ReviewMode.DIFF.requiresContainerExecution()).isFalse();
  }

  @Test
  void should_require_container_for_agentic_mode() {
    assertThat(ReviewMode.AGENTIC.requiresContainerExecution()).isTrue();
  }

  @Test
  void should_parse_diff_mode_from_string() {
    assertThat(ReviewMode.fromString("diff")).isEqualTo(ReviewMode.DIFF);
    assertThat(ReviewMode.fromString("DIFF")).isEqualTo(ReviewMode.DIFF);
    assertThat(ReviewMode.fromString("Diff")).isEqualTo(ReviewMode.DIFF);
  }

  @Test
  void should_parse_agentic_mode_from_string() {
    assertThat(ReviewMode.fromString("agentic")).isEqualTo(ReviewMode.AGENTIC);
    assertThat(ReviewMode.fromString("AGENTIC")).isEqualTo(ReviewMode.AGENTIC);
    assertThat(ReviewMode.fromString("Agentic")).isEqualTo(ReviewMode.AGENTIC);
  }

  @Test
  void should_default_to_diff_for_null_input() {
    assertThat(ReviewMode.fromString(null)).isEqualTo(ReviewMode.DIFF);
  }

  @Test
  void should_default_to_diff_for_blank_input() {
    assertThat(ReviewMode.fromString("")).isEqualTo(ReviewMode.DIFF);
    assertThat(ReviewMode.fromString("   ")).isEqualTo(ReviewMode.DIFF);
  }

  @Test
  void should_default_to_diff_for_unknown_input() {
    assertThat(ReviewMode.fromString("unknown")).isEqualTo(ReviewMode.DIFF);
    assertThat(ReviewMode.fromString("hybrid")).isEqualTo(ReviewMode.DIFF);
  }

  @Test
  void should_trim_whitespace_when_parsing() {
    assertThat(ReviewMode.fromString("  agentic  ")).isEqualTo(ReviewMode.AGENTIC);
    assertThat(ReviewMode.fromString("\tdiff\t")).isEqualTo(ReviewMode.DIFF);
  }
}
