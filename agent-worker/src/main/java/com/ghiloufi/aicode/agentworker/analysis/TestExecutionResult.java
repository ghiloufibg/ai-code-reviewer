package com.ghiloufi.aicode.agentworker.analysis;

import com.ghiloufi.aicode.core.domain.model.TestResult;
import java.time.Duration;
import java.util.List;

public record TestExecutionResult(
    boolean executed,
    boolean success,
    String frameworkName,
    List<TestResult> testResults,
    int totalTests,
    int passedTests,
    int failedTests,
    int skippedTests,
    Duration duration,
    String rawOutput,
    String errorMessage) {

  public static TestExecutionResult notExecuted(String reason) {
    return new TestExecutionResult(
        false, true, null, List.of(), 0, 0, 0, 0, Duration.ZERO, null, reason);
  }

  public static TestExecutionResult success(
      String frameworkName,
      List<TestResult> testResults,
      int total,
      int passed,
      int failed,
      int skipped,
      Duration duration,
      String rawOutput) {
    return new TestExecutionResult(
        true,
        failed == 0,
        frameworkName,
        testResults,
        total,
        passed,
        failed,
        skipped,
        duration,
        rawOutput,
        null);
  }

  public static TestExecutionResult failure(
      String frameworkName,
      List<TestResult> testResults,
      Duration duration,
      String rawOutput,
      String errorMessage) {
    final int passed = (int) testResults.stream().filter(TestResult::isSuccess).count();
    final int failed = testResults.size() - passed;
    return new TestExecutionResult(
        true,
        false,
        frameworkName,
        testResults,
        testResults.size(),
        passed,
        failed,
        0,
        duration,
        rawOutput,
        errorMessage);
  }
}
