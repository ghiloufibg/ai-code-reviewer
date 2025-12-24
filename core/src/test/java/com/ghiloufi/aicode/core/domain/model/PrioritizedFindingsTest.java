package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class PrioritizedFindingsTest {

  @Nested
  final class Creation {

    @Test
    void should_create_empty_prioritized_findings() {
      final var findings = PrioritizedFindings.empty();

      assertThat(findings.criticalIssues()).isEmpty();
      assertThat(findings.highPriorityIssues()).isEmpty();
      assertThat(findings.mediumPriorityIssues()).isEmpty();
      assertThat(findings.lowPriorityIssues()).isEmpty();
      assertThat(findings.filteredOut()).isEmpty();
      assertThat(findings.isEmpty()).isTrue();
    }

    @Test
    void should_create_from_issues_list() {
      final var critical = createIssue("critical", 0.9);
      final var high = createIssue("error", 0.85);
      final var medium = createIssue("warning", 0.8);
      final var low = createIssue("info", 0.75);

      final var findings =
          PrioritizedFindings.fromIssues(List.of(critical, high, medium, low), 0.7, 10);

      assertThat(findings.criticalIssues()).hasSize(1);
      assertThat(findings.highPriorityIssues()).hasSize(1);
      assertThat(findings.mediumPriorityIssues()).hasSize(1);
      assertThat(findings.lowPriorityIssues()).hasSize(1);
      assertThat(findings.totalIncludedCount()).isEqualTo(4);
      assertThat(findings.isEmpty()).isFalse();
    }

    @Test
    void should_filter_by_confidence_threshold() {
      final var highConfidence = createIssue("warning", 0.9);
      final var lowConfidence = createIssue("warning", 0.5);

      final var findings =
          PrioritizedFindings.fromIssues(List.of(highConfidence, lowConfidence), 0.7, 10);

      assertThat(findings.totalIncludedCount()).isEqualTo(1);
      assertThat(findings.totalFilteredCount()).isEqualTo(1);
    }

    @Test
    void should_limit_issues_per_file() {
      final var issues =
          List.of(
              createIssueForFile("Test.java", "critical", 0.95),
              createIssueForFile("Test.java", "error", 0.9),
              createIssueForFile("Test.java", "warning", 0.85),
              createIssueForFile("Other.java", "warning", 0.8));

      final var findings = PrioritizedFindings.fromIssues(issues, 0.7, 2);

      assertThat(findings.totalIncludedCount()).isEqualTo(3);
      assertThat(findings.totalFilteredCount()).isEqualTo(1);
    }

    @Test
    void should_require_non_null_metrics() {
      assertThatThrownBy(
              () ->
                  new PrioritizedFindings(
                      List.of(), List.of(), List.of(), List.of(), List.of(), null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("metrics");
    }
  }

  @Nested
  final class Builder {

    @Test
    void should_build_with_all_fields() {
      final var critical = createIssue("critical", 0.95);
      final var high = createIssue("error", 0.9);
      final var filtered = createIssue("info", 0.5);
      final var metrics =
          new PrioritizedFindings.PrioritizationMetrics(
              3, 2, 1, 0, 0.92, Map.of("CRITICAL", 1, "HIGH", 1));

      final var findings =
          PrioritizedFindings.builder()
              .criticalIssues(List.of(critical))
              .highPriorityIssues(List.of(high))
              .filteredOut(List.of(filtered))
              .metrics(metrics)
              .build();

      assertThat(findings.criticalIssues()).hasSize(1);
      assertThat(findings.highPriorityIssues()).hasSize(1);
      assertThat(findings.filteredOut()).hasSize(1);
      assertThat(findings.metrics().totalInputIssues()).isEqualTo(3);
    }
  }

  @Nested
  final class QueryMethods {

    @Test
    void should_return_all_prioritized_issues() {
      final var critical = createIssue("critical", 0.95);
      final var high = createIssue("error", 0.9);
      final var medium = createIssue("warning", 0.85);
      final var low = createIssue("info", 0.8);

      final var findings =
          PrioritizedFindings.fromIssues(List.of(critical, high, medium, low), 0.7, 10);

      final var all = findings.allPrioritizedIssues();

      assertThat(all).hasSize(4);
    }

    @Test
    void should_detect_critical_issues() {
      final var critical = createIssue("critical", 0.95);
      final var findings = PrioritizedFindings.fromIssues(List.of(critical), 0.7, 10);

      assertThat(findings.hasCriticalIssues()).isTrue();
    }

    @Test
    void should_detect_high_priority_issues() {
      final var high = createIssue("error", 0.9);
      final var findings = PrioritizedFindings.fromIssues(List.of(high), 0.7, 10);

      assertThat(findings.hasHighPriorityIssues()).isTrue();
    }

    @Test
    void should_report_no_critical_when_only_low_priority() {
      final var low = createIssue("info", 0.8);
      final var findings = PrioritizedFindings.fromIssues(List.of(low), 0.7, 10);

      assertThat(findings.hasCriticalIssues()).isFalse();
      assertThat(findings.hasHighPriorityIssues()).isFalse();
    }
  }

  @Nested
  final class PrioritizationMetricsTest {

    @Test
    void should_create_empty_metrics() {
      final var metrics = PrioritizedFindings.PrioritizationMetrics.empty();

      assertThat(metrics.totalInputIssues()).isZero();
      assertThat(metrics.totalOutputIssues()).isZero();
      assertThat(metrics.averageConfidence()).isZero();
    }

    @Test
    void should_calculate_filter_rate() {
      final var metrics =
          new PrioritizedFindings.PrioritizationMetrics(10, 7, 3, 0, 0.85, Map.of());

      assertThat(metrics.filterRate()).isEqualTo(0.3);
    }

    @Test
    void should_handle_zero_input_for_filter_rate() {
      final var metrics = new PrioritizedFindings.PrioritizationMetrics(0, 0, 0, 0, 0.0, Map.of());

      assertThat(metrics.filterRate()).isZero();
    }
  }

  @Nested
  final class SeverityMapping {

    @Test
    void should_map_critical_severity_to_critical_priority() {
      final var issue = createIssue("critical", 0.9);
      final var findings = PrioritizedFindings.fromIssues(List.of(issue), 0.7, 10);

      assertThat(findings.criticalIssues()).hasSize(1);
    }

    @Test
    void should_map_blocker_severity_to_critical_priority() {
      final var issue = createIssue("blocker", 0.9);
      final var findings = PrioritizedFindings.fromIssues(List.of(issue), 0.7, 10);

      assertThat(findings.criticalIssues()).hasSize(1);
    }

    @Test
    void should_map_error_severity_to_high_priority() {
      final var issue = createIssue("error", 0.9);
      final var findings = PrioritizedFindings.fromIssues(List.of(issue), 0.7, 10);

      assertThat(findings.highPriorityIssues()).hasSize(1);
    }

    @Test
    void should_map_warning_severity_to_medium_priority() {
      final var issue = createIssue("warning", 0.9);
      final var findings = PrioritizedFindings.fromIssues(List.of(issue), 0.7, 10);

      assertThat(findings.mediumPriorityIssues()).hasSize(1);
    }

    @Test
    void should_map_info_severity_to_low_priority() {
      final var issue = createIssue("info", 0.9);
      final var findings = PrioritizedFindings.fromIssues(List.of(issue), 0.7, 10);

      assertThat(findings.lowPriorityIssues()).hasSize(1);
    }

    @Test
    void should_map_null_severity_to_medium_priority() {
      final var issue = createIssue(null, 0.9);
      final var findings = PrioritizedFindings.fromIssues(List.of(issue), 0.7, 10);

      assertThat(findings.mediumPriorityIssues()).hasSize(1);
    }
  }

  private ReviewResult.Issue createIssue(String severity, double confidence) {
    return ReviewResult.Issue.issueBuilder()
        .file("Test.java")
        .startLine(1)
        .severity(severity)
        .title("Test issue")
        .suggestion("Fix this")
        .confidenceScore(confidence)
        .build();
  }

  private ReviewResult.Issue createIssueForFile(String file, String severity, double confidence) {
    return ReviewResult.Issue.issueBuilder()
        .file(file)
        .startLine(1)
        .severity(severity)
        .title("Test issue")
        .suggestion("Fix this")
        .confidenceScore(confidence)
        .build();
  }
}
