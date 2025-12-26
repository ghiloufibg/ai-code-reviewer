package com.ghiloufi.aicode.agentworker.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.TestResult;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TestExecutionResult")
final class TestExecutionResultTest {

  @Nested
  @DisplayName("notExecuted factory method")
  final class NotExecutedTests {

    @Test
    void should_create_not_executed_result_with_reason() {
      final var reason = "Test execution is disabled";

      final var result = TestExecutionResult.notExecuted(reason);

      assertThat(result.executed()).isFalse();
      assertThat(result.success()).isTrue();
      assertThat(result.frameworkName()).isNull();
      assertThat(result.testResults()).isEmpty();
      assertThat(result.totalTests()).isZero();
      assertThat(result.passedTests()).isZero();
      assertThat(result.failedTests()).isZero();
      assertThat(result.skippedTests()).isZero();
      assertThat(result.duration()).isEqualTo(Duration.ZERO);
      assertThat(result.rawOutput()).isNull();
      assertThat(result.errorMessage()).isEqualTo(reason);
    }
  }

  @Nested
  @DisplayName("success factory method")
  final class SuccessTests {

    @Test
    void should_create_success_result_with_all_tests_passed() {
      final var frameworkName = "maven";
      final var testResults =
          List.of(
              TestResult.passed("com.example.Test", "testMethod1", Duration.ofMillis(100)),
              TestResult.passed("com.example.Test", "testMethod2", Duration.ofMillis(200)));
      final var duration = Duration.ofSeconds(5);
      final var rawOutput = "Build successful";

      final var result =
          TestExecutionResult.success(frameworkName, testResults, 2, 2, 0, 0, duration, rawOutput);

      assertThat(result.executed()).isTrue();
      assertThat(result.success()).isTrue();
      assertThat(result.frameworkName()).isEqualTo("maven");
      assertThat(result.testResults()).hasSize(2);
      assertThat(result.totalTests()).isEqualTo(2);
      assertThat(result.passedTests()).isEqualTo(2);
      assertThat(result.failedTests()).isZero();
      assertThat(result.skippedTests()).isZero();
      assertThat(result.duration()).isEqualTo(duration);
      assertThat(result.rawOutput()).isEqualTo(rawOutput);
      assertThat(result.errorMessage()).isNull();
    }

    @Test
    void should_mark_as_failure_when_failed_tests_exist() {
      final var frameworkName = "npm";
      final var testResults =
          List.of(
              TestResult.passed("Test", "passing", Duration.ZERO),
              TestResult.failed("Test", "failing", "assertion failed", null));

      final var result =
          TestExecutionResult.success(
              frameworkName, testResults, 2, 1, 1, 0, Duration.ofSeconds(1), "output");

      assertThat(result.executed()).isTrue();
      assertThat(result.success()).isFalse();
      assertThat(result.failedTests()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("failure factory method")
  final class FailureTests {

    @Test
    void should_create_failure_result_with_error_message() {
      final var frameworkName = "gradle";
      final var testResults =
          List.of(
              TestResult.passed("Test", "passing", Duration.ZERO),
              TestResult.failed("Test", "failing", "error", null));
      final var duration = Duration.ofSeconds(3);
      final var errorMessage = "Build failed with errors";

      final var result =
          TestExecutionResult.failure(frameworkName, testResults, duration, "output", errorMessage);

      assertThat(result.executed()).isTrue();
      assertThat(result.success()).isFalse();
      assertThat(result.frameworkName()).isEqualTo("gradle");
      assertThat(result.testResults()).hasSize(2);
      assertThat(result.totalTests()).isEqualTo(2);
      assertThat(result.passedTests()).isEqualTo(1);
      assertThat(result.failedTests()).isEqualTo(1);
      assertThat(result.duration()).isEqualTo(duration);
      assertThat(result.errorMessage()).isEqualTo(errorMessage);
    }

    @Test
    void should_calculate_passed_and_failed_counts_from_test_results() {
      final var testResults =
          List.of(
              TestResult.passed("A", "test1", Duration.ZERO),
              TestResult.passed("A", "test2", Duration.ZERO),
              TestResult.failed("A", "test3", "failed", null));

      final var result =
          TestExecutionResult.failure("pytest", testResults, Duration.ZERO, null, "error");

      assertThat(result.passedTests()).isEqualTo(2);
      assertThat(result.failedTests()).isEqualTo(1);
      assertThat(result.totalTests()).isEqualTo(3);
    }
  }
}
