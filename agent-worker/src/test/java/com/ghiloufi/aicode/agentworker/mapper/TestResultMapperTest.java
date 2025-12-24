package com.ghiloufi.aicode.agentworker.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.TestResult;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TestResultMapper")
final class TestResultMapperTest {

  private TestResultMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new TestResultMapper();
  }

  @Nested
  @DisplayName("mapFailedTest")
  final class MapFailedTestTests {

    @Test
    void should_map_failed_test_to_issue() {
      final var failedTest =
          TestResult.failed(
              "com.example.MyTest", "shouldDoSomething", "Expected 1 but was 2", null);

      final var issue = mapper.mapFailedTest(failedTest);

      assertThat(issue.getFile()).isEqualTo("com/example/MyTest.java");
      assertThat(issue.getStartLine()).isEqualTo(1);
      assertThat(issue.getSeverity()).isEqualTo("error");
      assertThat(issue.getTitle()).isEqualTo("Test Failed: shouldDoSomething");
      assertThat(issue.getConfidenceScore()).isEqualTo(1.0);
    }

    @Test
    void should_include_failure_message_in_suggestion() {
      final var failedTest =
          TestResult.failed("Test", "myTest", "assertion error: values differ", null);

      final var issue = mapper.mapFailedTest(failedTest);

      assertThat(issue.getSuggestion()).contains("assertion error: values differ");
    }

    @Test
    void should_handle_unknown_test_class() {
      final var failedTest = TestResult.failed("unknown", "testMethod", "failed", null);

      final var issue = mapper.mapFailedTest(failedTest);

      assertThat(issue.getFile()).isEqualTo("tests/unknown");
    }

    @Test
    void should_handle_null_failure_message() {
      final var failedTest = TestResult.failed("Test", "testMethod", null, null);

      final var issue = mapper.mapFailedTest(failedTest);

      assertThat(issue.getSuggestion()).contains("Test `testMethod` failed");
    }
  }

  @Nested
  @DisplayName("mapFailedTests")
  final class MapFailedTestsTests {

    @Test
    void should_only_map_failed_tests() {
      final var tests =
          List.of(
              TestResult.passed("Test", "passing1", Duration.ZERO),
              TestResult.failed("Test", "failing", "error", null),
              TestResult.passed("Test", "passing2", Duration.ZERO));

      final var issues = mapper.mapFailedTests(tests);

      assertThat(issues).hasSize(1);
      assertThat(issues.getFirst().getTitle()).isEqualTo("Test Failed: failing");
    }

    @Test
    void should_return_empty_list_when_all_tests_pass() {
      final var tests =
          List.of(
              TestResult.passed("Test", "test1", Duration.ZERO),
              TestResult.passed("Test", "test2", Duration.ZERO));

      final var issues = mapper.mapFailedTests(tests);

      assertThat(issues).isEmpty();
    }

    @Test
    void should_map_all_failed_tests() {
      final var tests =
          List.of(
              TestResult.failed("Test", "fail1", "error1", null),
              TestResult.failed("Test", "fail2", "error2", null),
              TestResult.failed("Test", "fail3", "error3", null));

      final var issues = mapper.mapFailedTests(tests);

      assertThat(issues).hasSize(3);
    }

    @Test
    void should_include_error_type_tests_as_failures() {
      final var tests =
          List.of(
              TestResult.passed("Test", "passing", Duration.ZERO),
              TestResult.error("Test", "errorTest", "NullPointerException", "stack trace"));

      final var issues = mapper.mapFailedTests(tests);

      assertThat(issues).hasSize(1);
      assertThat(issues.getFirst().getTitle()).isEqualTo("Test Failed: errorTest");
    }

    @Test
    void should_exclude_skipped_tests() {
      final var tests =
          List.of(
              TestResult.skipped("Test", "skipped", "disabled"),
              TestResult.passed("Test", "passing", Duration.ZERO));

      final var issues = mapper.mapFailedTests(tests);

      assertThat(issues).isEmpty();
    }
  }
}
