package com.ghiloufi.aicode.core.domain.model;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record TestResult(
    String testClass,
    String testMethod,
    TestOutcome outcome,
    Duration duration,
    String failureMessage,
    String stackTrace,
    List<String> outputLines) {

  public TestResult {
    Objects.requireNonNull(testClass, "testClass must not be null");
    Objects.requireNonNull(testMethod, "testMethod must not be null");
    Objects.requireNonNull(outcome, "outcome must not be null");
    outputLines = outputLines != null ? List.copyOf(outputLines) : List.of();
  }

  public static TestResult passed(
      final String testClass, final String testMethod, final Duration duration) {
    return new TestResult(
        testClass, testMethod, TestOutcome.PASSED, duration, null, null, List.of());
  }

  public static TestResult failed(
      final String testClass,
      final String testMethod,
      final String failureMessage,
      final String stackTrace) {
    return new TestResult(
        testClass, testMethod, TestOutcome.FAILED, null, failureMessage, stackTrace, List.of());
  }

  public static TestResult skipped(
      final String testClass, final String testMethod, final String reason) {
    return new TestResult(
        testClass, testMethod, TestOutcome.SKIPPED, null, reason, null, List.of());
  }

  public static TestResult error(
      final String testClass,
      final String testMethod,
      final String errorMessage,
      final String stackTrace) {
    return new TestResult(
        testClass, testMethod, TestOutcome.ERROR, null, errorMessage, stackTrace, List.of());
  }

  public boolean isSuccess() {
    return outcome == TestOutcome.PASSED;
  }

  public boolean isFailure() {
    return outcome == TestOutcome.FAILED || outcome == TestOutcome.ERROR;
  }

  public boolean isSkipped() {
    return outcome == TestOutcome.SKIPPED;
  }

  public String fullyQualifiedName() {
    return testClass + "#" + testMethod;
  }

  public ReviewResult.Issue toReviewIssue() {
    if (!isFailure()) {
      return null;
    }

    final String file = testClass.replace('.', '/') + ".java";
    final String title = "Test " + outcome.name().toLowerCase() + ": " + testMethod;
    final String suggestion = failureMessage != null ? failureMessage : "Test did not pass";

    return ReviewResult.Issue.issueBuilder()
        .file("src/test/java/" + file)
        .startLine(1)
        .severity(outcome == TestOutcome.ERROR ? "error" : "warning")
        .title(title)
        .suggestion(suggestion)
        .confidenceScore(1.0)
        .confidenceExplanation("Test execution result")
        .build();
  }

  public enum TestOutcome {
    PASSED,
    FAILED,
    ERROR,
    SKIPPED
  }
}
