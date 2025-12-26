package com.ghiloufi.aicode.agentworker.aggregation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.agentworker.analysis.TestExecutionResult;
import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties;
import com.ghiloufi.aicode.agentworker.mapper.TestResultMapper;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.TestResult;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ResultAggregator")
final class ResultAggregatorTest {

  private ResultAggregator aggregator;
  private AgentWorkerProperties properties;

  @BeforeEach
  void setUp() {
    final var dedup =
        new AgentWorkerProperties.AggregationProperties.DeduplicationProperties(true, 0.85);
    final var filtering =
        new AgentWorkerProperties.AggregationProperties.FilteringProperties(0.7, 10);
    final var aggregation = new AgentWorkerProperties.AggregationProperties(dedup, filtering);

    final var decision = new AgentWorkerProperties.DecisionProperties("openai", "gpt-4o", 3);
    properties =
        new AgentWorkerProperties(
            new AgentWorkerProperties.ConsumerProperties(
                "test-stream", "test-group", "test-consumer", 1, Duration.ofSeconds(5)),
            new AgentWorkerProperties.CloneProperties(1, Duration.ofMinutes(2), "token"),
            new AgentWorkerProperties.DockerProperties(
                "unix:///var/run/docker.sock",
                "test-image",
                new AgentWorkerProperties.DockerProperties.ResourceLimitsProperties(
                    2147483648L, 2000000000L),
                Duration.ofMinutes(10),
                true),
            aggregation,
            decision);

    final var testResultMapper = new TestResultMapper();
    aggregator = new ResultAggregator(testResultMapper, properties);
  }

  @Nested
  @DisplayName("aggregate with AI review only")
  final class AiReviewOnlyTests {

    @Test
    void should_include_all_ai_issues_above_confidence_threshold() {
      final var aiReview =
          buildReviewWithIssues(
              List.of(
                  buildIssue("file1.java", 10, "warning", "Issue 1", 0.8),
                  buildIssue("file2.java", 20, "error", "Issue 2", 0.9)));

      final var result = aggregator.aggregate(aiReview, (TestExecutionResult) null);

      assertThat(result.issues()).hasSize(2);
      assertThat(result.findingCountsBySource()).containsEntry("ai", 2);
      assertThat(result.findingCountsBySource()).containsEntry("tests", 0);
    }

    @Test
    void should_filter_out_issues_below_confidence_threshold() {
      final var aiReview =
          buildReviewWithIssues(
              List.of(
                  buildIssue("file1.java", 10, "warning", "High Confidence", 0.9),
                  buildIssue("file2.java", 20, "warning", "Low Confidence", 0.5)));

      final var result = aggregator.aggregate(aiReview, (TestExecutionResult) null);

      assertThat(result.issues()).hasSize(1);
      assertThat(result.issues().getFirst().getTitle()).isEqualTo("High Confidence");
    }

    @Test
    void should_include_issues_with_null_confidence() {
      final var aiReview =
          buildReviewWithIssues(
              List.of(buildIssue("file1.java", 10, "warning", "No Confidence Score", null)));

      final var result = aggregator.aggregate(aiReview, (TestExecutionResult) null);

      assertThat(result.issues()).hasSize(1);
    }

    @Test
    void should_include_notes_from_ai_review() {
      final var aiReview =
          ReviewResult.builder()
              .summary("Test summary")
              .issues(List.of())
              .nonBlockingNotes(
                  List.of(
                      ReviewResult.Note.noteBuilder()
                          .file("file.java")
                          .line(1)
                          .note("Consider this improvement")
                          .build()))
              .build();

      final var result = aggregator.aggregate(aiReview, (TestExecutionResult) null);

      assertThat(result.notes()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("aggregate with test results")
  final class TestResultsTests {

    @Test
    void should_add_issues_from_failed_tests() {
      final var testResult =
          TestExecutionResult.failure(
              "maven",
              List.of(
                  TestResult.passed("Test", "passing", Duration.ZERO),
                  TestResult.failed("Test", "failing", "assertion failed", null)),
              Duration.ofSeconds(5),
              "output",
              "Build failed");

      final var result = aggregator.aggregate(null, testResult);

      assertThat(result.issues()).hasSize(1);
      assertThat(result.findingCountsBySource()).containsEntry("tests", 1);
    }

    @Test
    void should_not_add_issues_when_tests_pass() {
      final var testResult =
          TestExecutionResult.success(
              "maven",
              List.of(TestResult.passed("Test", "passing", Duration.ZERO)),
              1,
              1,
              0,
              0,
              Duration.ofSeconds(5),
              "output");

      final var result = aggregator.aggregate(null, testResult);

      assertThat(result.issues()).isEmpty();
      assertThat(result.findingCountsBySource()).containsEntry("tests", 0);
    }

    @Test
    void should_not_add_issues_when_tests_not_executed() {
      final var testResult = TestExecutionResult.notExecuted("disabled");

      final var result = aggregator.aggregate(null, testResult);

      assertThat(result.issues()).isEmpty();
    }
  }

  @Nested
  @DisplayName("aggregate combined")
  final class CombinedTests {

    @Test
    void should_combine_ai_and_test_issues() {
      final var aiReview =
          buildReviewWithIssues(List.of(buildIssue("file1.java", 10, "warning", "AI Issue", 0.8)));
      final var testResult =
          TestExecutionResult.failure(
              "maven",
              List.of(TestResult.failed("Test", "failing", "error", null)),
              Duration.ofSeconds(5),
              "output",
              "Build failed");

      final var result = aggregator.aggregate(aiReview, testResult);

      assertThat(result.issues()).hasSize(2);
      assertThat(result.findingCountsBySource()).containsEntry("ai", 1);
      assertThat(result.findingCountsBySource()).containsEntry("tests", 1);
    }
  }

  @Nested
  @DisplayName("deduplication")
  final class DeduplicationTests {

    @Test
    void should_remove_duplicate_issues_at_same_location() {
      final var aiReview =
          buildReviewWithIssues(
              List.of(
                  buildIssue("file.java", 10, "warning", "Duplicate Issue", 0.8),
                  buildIssue("file.java", 10, "warning", "Duplicate Issue", 0.9)));

      final var result = aggregator.aggregate(aiReview, (TestExecutionResult) null);

      assertThat(result.issues()).hasSize(1);
      assertThat(result.totalFindingsBeforeDedup()).isEqualTo(2);
      assertThat(result.totalFindingsAfterDedup()).isEqualTo(1);
    }

    @Test
    void should_keep_issues_at_different_locations() {
      final var aiReview =
          buildReviewWithIssues(
              List.of(
                  buildIssue("file.java", 10, "warning", "Issue 1", 0.8),
                  buildIssue("file.java", 20, "warning", "Issue 2", 0.8)));

      final var result = aggregator.aggregate(aiReview, (TestExecutionResult) null);

      assertThat(result.issues()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("issues per file limit")
  final class IssuesPerFileLimitTests {

    @Test
    void should_limit_issues_per_file_to_configured_max() {
      final var issues = new ArrayList<ReviewResult.Issue>();
      for (int i = 0; i < 15; i++) {
        issues.add(buildIssue("file.java", i + 1, "warning", "Issue " + i, 0.8));
      }
      final var aiReview = buildReviewWithIssues(issues);

      final var result = aggregator.aggregate(aiReview, (TestExecutionResult) null);

      assertThat(result.issues()).hasSize(10);
      assertThat(result.totalFindingsFiltered()).isEqualTo(5);
    }

    @Test
    void should_limit_issues_independently_per_file() {
      final var issues = new ArrayList<ReviewResult.Issue>();
      for (int i = 0; i < 5; i++) {
        issues.add(buildIssue("file1.java", i + 1, "warning", "File1 Issue " + i, 0.8));
        issues.add(buildIssue("file2.java", i + 1, "warning", "File2 Issue " + i, 0.8));
      }
      final var aiReview = buildReviewWithIssues(issues);

      final var result = aggregator.aggregate(aiReview, (TestExecutionResult) null);

      assertThat(result.issues()).hasSize(10);
    }
  }

  @Nested
  @DisplayName("summary generation")
  final class SummaryTests {

    @Test
    void should_include_ai_summary_when_present() {
      final var aiReview =
          ReviewResult.builder()
              .summary("AI found issues in the code")
              .issues(List.of())
              .nonBlockingNotes(List.of())
              .build();

      final var result = aggregator.aggregate(aiReview, (TestExecutionResult) null);

      assertThat(result.summary()).contains("AI found issues in the code");
    }

    @Test
    void should_append_test_execution_status() {
      final var testResult =
          TestExecutionResult.success(
              "maven",
              List.of(
                  TestResult.passed("Test", "test1", Duration.ZERO),
                  TestResult.passed("Test", "test2", Duration.ZERO)),
              2,
              2,
              0,
              0,
              Duration.ofSeconds(5),
              "output");

      final var result = aggregator.aggregate(null, testResult);

      assertThat(result.summary()).contains("Test Execution");
      assertThat(result.summary()).contains("All 2 tests passed");
    }

    @Test
    void should_show_failed_test_count_in_summary() {
      final var testResult =
          TestExecutionResult.failure(
              "maven",
              List.of(
                  TestResult.passed("Test", "passing", Duration.ZERO),
                  TestResult.failed("Test", "failing1", "error", null),
                  TestResult.failed("Test", "failing2", "error", null)),
              Duration.ofSeconds(5),
              "output",
              "Build failed");

      final var result = aggregator.aggregate(null, testResult);

      assertThat(result.summary()).contains("2 of 3 tests failed");
    }
  }

  @Nested
  @DisplayName("confidence calculation")
  final class ConfidenceTests {

    @Test
    void should_calculate_average_confidence() {
      final var aiReview =
          buildReviewWithIssues(
              List.of(
                  buildIssue("file1.java", 10, "warning", "Issue 1", 0.8),
                  buildIssue("file2.java", 20, "warning", "Issue 2", 0.9)));

      final var result = aggregator.aggregate(aiReview, (TestExecutionResult) null);

      assertThat(result.overallConfidence())
          .isCloseTo(0.85, org.assertj.core.api.Assertions.within(0.0001));
    }

    @Test
    void should_return_1_0_when_no_issues() {
      final var result = aggregator.aggregate(null, (TestExecutionResult) null);

      assertThat(result.overallConfidence()).isEqualTo(1.0);
    }

    @Test
    void should_use_default_when_all_issues_lack_confidence() {
      final var aiReview =
          buildReviewWithIssues(List.of(buildIssue("file1.java", 10, "warning", "Issue 1", null)));

      final var result = aggregator.aggregate(aiReview, (TestExecutionResult) null);

      assertThat(result.overallConfidence()).isEqualTo(0.7);
    }
  }

  @Nested
  @DisplayName("severity counts")
  final class SeverityCountTests {

    @Test
    void should_count_issues_by_severity() {
      final var aiReview =
          buildReviewWithIssues(
              List.of(
                  buildIssue("file1.java", 10, "error", "Error Issue", 0.8),
                  buildIssue("file2.java", 20, "warning", "Warning Issue", 0.8),
                  buildIssue("file3.java", 30, "warning", "Another Warning", 0.8)));

      final var result = aggregator.aggregate(aiReview, (TestExecutionResult) null);

      assertThat(result.findingCountsBySeverity()).containsEntry("error", 1);
      assertThat(result.findingCountsBySeverity()).containsEntry("warning", 2);
    }
  }

  private ReviewResult buildReviewWithIssues(List<ReviewResult.Issue> issues) {
    return ReviewResult.builder()
        .summary("Test summary")
        .issues(issues)
        .nonBlockingNotes(List.of())
        .build();
  }

  private ReviewResult.Issue buildIssue(
      String file, int line, String severity, String title, Double confidence) {
    return ReviewResult.Issue.issueBuilder()
        .file(file)
        .startLine(line)
        .severity(severity)
        .title(title)
        .suggestion("Fix this issue")
        .confidenceScore(confidence)
        .confidenceExplanation("Test confidence")
        .build();
  }
}
