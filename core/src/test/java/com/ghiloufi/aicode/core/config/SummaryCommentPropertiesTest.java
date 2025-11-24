package com.ghiloufi.aicode.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class SummaryCommentPropertiesTest {

  @Test
  void should_have_enabled_false_by_default() {
    final SummaryCommentProperties properties = new SummaryCommentProperties();

    assertThat(properties.isEnabled()).isFalse();
  }

  @Test
  void should_have_include_statistics_true_by_default() {
    final SummaryCommentProperties properties = new SummaryCommentProperties();

    assertThat(properties.isIncludeStatistics()).isTrue();
  }

  @Test
  void should_have_include_severity_breakdown_true_by_default() {
    final SummaryCommentProperties properties = new SummaryCommentProperties();

    assertThat(properties.isIncludeSeverityBreakdown()).isTrue();
  }

  @Test
  void should_allow_enabling_feature() {
    final SummaryCommentProperties properties = new SummaryCommentProperties();

    properties.setEnabled(true);

    assertThat(properties.isEnabled()).isTrue();
  }

  @Test
  void should_allow_disabling_statistics() {
    final SummaryCommentProperties properties = new SummaryCommentProperties();

    properties.setIncludeStatistics(false);

    assertThat(properties.isIncludeStatistics()).isFalse();
  }

  @Test
  void should_allow_disabling_severity_breakdown() {
    final SummaryCommentProperties properties = new SummaryCommentProperties();

    properties.setIncludeSeverityBreakdown(false);

    assertThat(properties.isIncludeSeverityBreakdown()).isFalse();
  }
}
