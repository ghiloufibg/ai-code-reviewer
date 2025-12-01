package com.ghiloufi.aicode.core.service.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConfidenceFilter Unit Tests")
class ConfidenceFilterTest {

  private ConfidenceFilter filter;

  @BeforeEach
  void setUp() {
    filter = new ConfidenceFilter();
  }

  @Test
  @DisplayName("should_throw_exception_when_review_result_is_null")
  void should_throw_exception_when_review_result_is_null() {
    assertThatThrownBy(() -> filter.filterByConfidence(null, 0.5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ReviewResult cannot be null");
  }

  @Test
  @DisplayName("should_use_default_threshold_when_threshold_is_below_zero")
  void should_use_default_threshold_when_threshold_is_below_zero() {
    final ReviewResult result = createResultWithIssues(0.4, 0.6);

    final ReviewResult filtered = filter.filterByConfidence(result, -0.1);

    assertThat(filtered.getIssues()).hasSize(1);
    assertThat(filtered.getIssues().get(0).getConfidenceScore()).isEqualTo(0.6);
  }

  @Test
  @DisplayName("should_use_default_threshold_when_threshold_is_above_one")
  void should_use_default_threshold_when_threshold_is_above_one() {
    final ReviewResult result = createResultWithIssues(0.4, 0.6);

    final ReviewResult filtered = filter.filterByConfidence(result, 1.5);

    assertThat(filtered.getIssues()).hasSize(1);
    assertThat(filtered.getIssues().get(0).getConfidenceScore()).isEqualTo(0.6);
  }

  @Test
  @DisplayName("should_return_unchanged_result_when_no_issues_present")
  void should_return_unchanged_result_when_no_issues_present() {
    final ReviewResult result = ReviewResult.builder().build();

    final ReviewResult filtered = filter.filterByConfidence(result, 0.5);

    assertThat(filtered.getIssues()).isEmpty();
  }

  @Test
  @DisplayName("should_filter_out_issues_below_confidence_threshold")
  void should_filter_out_issues_below_confidence_threshold() {
    final ReviewResult result = createResultWithIssues(0.3, 0.5, 0.7, 0.9);

    final ReviewResult filtered = filter.filterByConfidence(result, 0.6);

    assertThat(filtered.getIssues()).hasSize(2);
    assertThat(filtered.getIssues().get(0).getConfidenceScore()).isEqualTo(0.7);
    assertThat(filtered.getIssues().get(1).getConfidenceScore()).isEqualTo(0.9);
  }

  @Test
  @DisplayName("should_keep_issues_exactly_at_confidence_threshold")
  void should_keep_issues_exactly_at_confidence_threshold() {
    final ReviewResult result = createResultWithIssues(0.4, 0.5, 0.6);

    final ReviewResult filtered = filter.filterByConfidence(result, 0.5);

    assertThat(filtered.getIssues()).hasSize(2);
    assertThat(filtered.getIssues().get(0).getConfidenceScore()).isEqualTo(0.5);
    assertThat(filtered.getIssues().get(1).getConfidenceScore()).isEqualTo(0.6);
  }

  @Test
  @DisplayName("should_treat_null_confidence_as_default_0_5")
  void should_treat_null_confidence_as_default_0_5() {
    final ReviewResult.Issue issueWithNullConfidence =
        ReviewResult.Issue.issueBuilder()
            .title("Issue with null confidence")
            .confidenceScore(null)
            .build();

    final ReviewResult result =
        ReviewResult.builder().issues(List.of(issueWithNullConfidence)).build();

    final ReviewResult filtered = filter.filterByConfidence(result, 0.4);

    assertThat(filtered.getIssues()).hasSize(1);
  }

  @Test
  @DisplayName("should_filter_out_null_confidence_when_threshold_is_above_0_5")
  void should_filter_out_null_confidence_when_threshold_is_above_0_5() {
    final ReviewResult.Issue issueWithNullConfidence =
        ReviewResult.Issue.issueBuilder()
            .title("Issue with null confidence")
            .confidenceScore(null)
            .build();

    final ReviewResult result =
        ReviewResult.builder().issues(List.of(issueWithNullConfidence)).build();

    final ReviewResult filtered = filter.filterByConfidence(result, 0.6);

    assertThat(filtered.getIssues()).isEmpty();
  }

  @Test
  @DisplayName("should_keep_all_issues_when_threshold_is_zero")
  void should_keep_all_issues_when_threshold_is_zero() {
    final ReviewResult result = createResultWithIssues(0.1, 0.3, 0.5, 0.7, 0.9);

    final ReviewResult filtered = filter.filterByConfidence(result, 0.0);

    assertThat(filtered.getIssues()).hasSize(5);
  }

  @Test
  @DisplayName("should_filter_all_issues_when_threshold_is_one")
  void should_filter_all_issues_when_threshold_is_one() {
    final ReviewResult result = createResultWithIssues(0.3, 0.5, 0.7, 0.9);

    final ReviewResult filtered = filter.filterByConfidence(result, 1.0);

    assertThat(filtered.getIssues()).isEmpty();
  }

  @Test
  @DisplayName("should_keep_issues_with_confidence_one_when_threshold_is_one")
  void should_keep_issues_with_confidence_one_when_threshold_is_one() {
    final ReviewResult result = createResultWithIssues(0.5, 1.0, 0.9);

    final ReviewResult filtered = filter.filterByConfidence(result, 1.0);

    assertThat(filtered.getIssues()).hasSize(1);
    assertThat(filtered.getIssues().get(0).getConfidenceScore()).isEqualTo(1.0);
  }

  private ReviewResult createResultWithIssues(final Double... confidenceScores) {
    final List<ReviewResult.Issue> issues = new ArrayList<>();

    for (int i = 0; i < confidenceScores.length; i++) {
      final ReviewResult.Issue issue =
          ReviewResult.Issue.issueBuilder()
              .title("Issue " + (i + 1))
              .confidenceScore(confidenceScores[i])
              .confidenceExplanation("Confidence explanation for issue " + (i + 1))
              .build();
      issues.add(issue);
    }

    return ReviewResult.builder().issues(issues).build();
  }
}
