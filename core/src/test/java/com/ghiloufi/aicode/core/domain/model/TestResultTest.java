package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TestResultTest {

  @Test
  void should_create_passed_test_result() {
    final var result =
        TestResult.passed("com.example.ServiceTest", "should_work", Duration.ofMillis(150));

    assertThat(result.testClass()).isEqualTo("com.example.ServiceTest");
    assertThat(result.testMethod()).isEqualTo("should_work");
    assertThat(result.outcome()).isEqualTo(TestResult.TestOutcome.PASSED);
    assertThat(result.duration()).isEqualTo(Duration.ofMillis(150));
    assertThat(result.failureMessage()).isNull();
    assertThat(result.stackTrace()).isNull();
  }

  @Test
  void should_create_failed_test_result() {
    final var result =
        TestResult.failed(
            "com.example.ServiceTest",
            "should_fail",
            "Expected 1 but was 2",
            "at ServiceTest.java:45");

    assertThat(result.outcome()).isEqualTo(TestResult.TestOutcome.FAILED);
    assertThat(result.failureMessage()).isEqualTo("Expected 1 but was 2");
    assertThat(result.stackTrace()).isEqualTo("at ServiceTest.java:45");
    assertThat(result.duration()).isNull();
  }

  @Test
  void should_create_skipped_test_result() {
    final var result =
        TestResult.skipped("com.example.ServiceTest", "should_skip", "Not implemented yet");

    assertThat(result.outcome()).isEqualTo(TestResult.TestOutcome.SKIPPED);
    assertThat(result.failureMessage()).isEqualTo("Not implemented yet");
  }

  @Test
  void should_create_error_test_result() {
    final var result =
        TestResult.error(
            "com.example.ServiceTest", "should_error", "NullPointerException", "at line 10");

    assertThat(result.outcome()).isEqualTo(TestResult.TestOutcome.ERROR);
    assertThat(result.failureMessage()).isEqualTo("NullPointerException");
    assertThat(result.stackTrace()).isEqualTo("at line 10");
  }

  @Test
  void should_throw_when_test_class_is_null() {
    assertThatThrownBy(
            () ->
                new TestResult(
                    null, "method", TestResult.TestOutcome.PASSED, null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("testClass");
  }

  @Test
  void should_throw_when_test_method_is_null() {
    assertThatThrownBy(
            () ->
                new TestResult(
                    "class", null, TestResult.TestOutcome.PASSED, null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("testMethod");
  }

  @Test
  void should_throw_when_outcome_is_null() {
    assertThatThrownBy(() -> new TestResult("class", "method", null, null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("outcome");
  }

  @Test
  void should_identify_success() {
    final var passed = TestResult.passed("c", "m", null);
    final var failed = TestResult.failed("c", "m", "msg", null);
    final var skipped = TestResult.skipped("c", "m", null);
    final var error = TestResult.error("c", "m", "msg", null);

    assertThat(passed.isSuccess()).isTrue();
    assertThat(failed.isSuccess()).isFalse();
    assertThat(skipped.isSuccess()).isFalse();
    assertThat(error.isSuccess()).isFalse();
  }

  @Test
  void should_identify_failure() {
    final var passed = TestResult.passed("c", "m", null);
    final var failed = TestResult.failed("c", "m", "msg", null);
    final var skipped = TestResult.skipped("c", "m", null);
    final var error = TestResult.error("c", "m", "msg", null);

    assertThat(passed.isFailure()).isFalse();
    assertThat(failed.isFailure()).isTrue();
    assertThat(skipped.isFailure()).isFalse();
    assertThat(error.isFailure()).isTrue();
  }

  @Test
  void should_return_fully_qualified_name() {
    final var result = TestResult.passed("com.example.ServiceTest", "should_work", null);

    assertThat(result.fullyQualifiedName()).isEqualTo("com.example.ServiceTest#should_work");
  }

  @Test
  void should_convert_failed_test_to_review_issue() {
    final var result =
        TestResult.failed("com.example.ServiceTest", "should_validate", "Assertion failed", null);

    final var issue = result.toReviewIssue();

    assertThat(issue).isNotNull();
    assertThat(issue.getFile()).isEqualTo("src/test/java/com/example/ServiceTest.java");
    assertThat(issue.getStartLine()).isEqualTo(1);
    assertThat(issue.getSeverity()).isEqualTo("warning");
    assertThat(issue.getTitle()).isEqualTo("Test failed: should_validate");
    assertThat(issue.getSuggestion()).isEqualTo("Assertion failed");
    assertThat(issue.getConfidenceScore()).isEqualTo(1.0);
  }

  @Test
  void should_convert_error_test_to_review_issue() {
    final var result = TestResult.error("com.example.ServiceTest", "should_work", "NPE", null);

    final var issue = result.toReviewIssue();

    assertThat(issue).isNotNull();
    assertThat(issue.getSeverity()).isEqualTo("error");
    assertThat(issue.getTitle()).isEqualTo("Test error: should_work");
  }

  @Test
  void should_not_convert_passed_test_to_issue() {
    final var result = TestResult.passed("c", "m", null);

    assertThat(result.toReviewIssue()).isNull();
  }

  @Test
  void should_not_convert_skipped_test_to_issue() {
    final var result = TestResult.skipped("c", "m", null);

    assertThat(result.toReviewIssue()).isNull();
  }

  @Test
  void should_handle_null_output_lines() {
    final var result =
        new TestResult("c", "m", TestResult.TestOutcome.PASSED, null, null, null, null);

    assertThat(result.outputLines()).isEmpty();
  }

  @Test
  void should_make_defensive_copy_of_output_lines() {
    final var mutableList = new ArrayList<>(List.of("line1", "line2"));
    final var result =
        new TestResult("c", "m", TestResult.TestOutcome.PASSED, null, null, null, mutableList);

    mutableList.clear();

    assertThat(result.outputLines()).hasSize(2);
  }
}
