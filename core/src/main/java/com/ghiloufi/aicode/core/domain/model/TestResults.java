package com.ghiloufi.aicode.core.domain.model;

import java.util.List;
import java.util.Objects;

public record TestResults(boolean passed, TestSummary summary, List<TestFailure> failures) {

  public TestResults {
    Objects.requireNonNull(summary, "summary must not be null");
    failures = failures != null ? List.copyOf(failures) : List.of();
  }

  public static TestResults none() {
    return new TestResults(true, TestSummary.empty(), List.of());
  }

  public static TestResults allPassed(final int totalTests) {
    return new TestResults(true, TestSummary.allPassed(totalTests), List.of());
  }

  public static TestResults withFailures(
      final TestSummary summary, final List<TestFailure> failures) {
    final boolean passed = failures == null || failures.isEmpty();
    return new TestResults(passed, summary, failures);
  }

  public boolean hasFailures() {
    return !failures.isEmpty();
  }

  public boolean wasExecuted() {
    return summary.hasTests();
  }

  public int failureCount() {
    return failures.size();
  }

  public String formatSummary() {
    if (!wasExecuted()) {
      return "No tests executed";
    }
    if (passed) {
      return String.format("All %d tests passed", summary.total());
    }
    return String.format(
        "%d of %d tests failed (%.1f%% pass rate)",
        summary.failed(), summary.total(), summary.passRate() * 100);
  }
}
