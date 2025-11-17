package com.ghiloufi.aicode.core.security.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Severity Tests")
final class SeverityTest {

  @Test
  @DisplayName("should_return_correct_weight_for_critical_severity")
  final void should_return_critical_weight() {
    assertThat(Severity.CRITICAL.getWeight()).isEqualTo(10.0);
  }

  @Test
  @DisplayName("should_return_correct_weight_for_high_severity")
  final void should_return_high_weight() {
    assertThat(Severity.HIGH.getWeight()).isEqualTo(7.0);
  }

  @Test
  @DisplayName("should_return_correct_weight_for_medium_severity")
  final void should_return_medium_weight() {
    assertThat(Severity.MEDIUM.getWeight()).isEqualTo(4.0);
  }

  @Test
  @DisplayName("should_return_correct_weight_for_low_severity")
  final void should_return_low_weight() {
    assertThat(Severity.LOW.getWeight()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("should_return_correct_weight_for_info_severity")
  final void should_return_info_weight() {
    assertThat(Severity.INFO.getWeight()).isEqualTo(0.1);
  }

  @Test
  @DisplayName("should_have_descending_weight_order")
  final void should_have_descending_weights() {
    assertThat(Severity.CRITICAL.getWeight())
        .isGreaterThan(Severity.HIGH.getWeight())
        .isGreaterThan(Severity.MEDIUM.getWeight())
        .isGreaterThan(Severity.LOW.getWeight())
        .isGreaterThan(Severity.INFO.getWeight());
  }
}
