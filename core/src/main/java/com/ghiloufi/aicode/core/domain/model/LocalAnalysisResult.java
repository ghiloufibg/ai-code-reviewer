package com.ghiloufi.aicode.core.domain.model;

import java.util.List;
import java.util.Objects;

public record LocalAnalysisResult(List<TestResult> testResults, AnalysisMetadata metadata) {

  public LocalAnalysisResult {
    Objects.requireNonNull(metadata, "metadata must not be null");
    testResults = testResults != null ? List.copyOf(testResults) : List.of();
  }

  public static LocalAnalysisResult empty(final AnalysisMetadata metadata) {
    return new LocalAnalysisResult(List.of(), metadata);
  }

  public static LocalAnalysisResult withTestResults(
      final List<TestResult> testResults, final AnalysisMetadata metadata) {
    return new LocalAnalysisResult(testResults, metadata);
  }

  public int failedTestCount() {
    return (int) testResults.stream().filter(TestResult::isFailure).count();
  }

  public int passedTestCount() {
    return (int) testResults.stream().filter(TestResult::isSuccess).count();
  }

  public int skippedTestCount() {
    return (int) testResults.stream().filter(TestResult::isSkipped).count();
  }

  public int totalTestCount() {
    return testResults.size();
  }

  public boolean hasTestFailures() {
    return testResults.stream().anyMatch(TestResult::isFailure);
  }

  public boolean allTestsPassed() {
    return !testResults.isEmpty() && testResults.stream().allMatch(TestResult::isSuccess);
  }

  public List<TestResult> failedTests() {
    return testResults.stream().filter(TestResult::isFailure).toList();
  }

  public LocalAnalysisResult merge(final LocalAnalysisResult other) {
    final List<TestResult> mergedTests =
        java.util.stream.Stream.concat(testResults.stream(), other.testResults.stream()).toList();

    return new LocalAnalysisResult(mergedTests, metadata);
  }
}
