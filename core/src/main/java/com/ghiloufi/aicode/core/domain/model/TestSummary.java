package com.ghiloufi.aicode.core.domain.model;

public record TestSummary(int total, int passed, int failed, int skipped) {

  public TestSummary {
    if (total < 0) {
      throw new IllegalArgumentException("total must not be negative");
    }
    if (passed < 0) {
      throw new IllegalArgumentException("passed must not be negative");
    }
    if (failed < 0) {
      throw new IllegalArgumentException("failed must not be negative");
    }
    if (skipped < 0) {
      throw new IllegalArgumentException("skipped must not be negative");
    }
  }

  public static TestSummary empty() {
    return new TestSummary(0, 0, 0, 0);
  }

  public static TestSummary allPassed(final int total) {
    return new TestSummary(total, total, 0, 0);
  }

  public boolean hasTests() {
    return total > 0;
  }

  public boolean allPassed() {
    return failed == 0 && hasTests();
  }

  public boolean hasFailures() {
    return failed > 0;
  }

  public double passRate() {
    if (total == 0) {
      return 1.0;
    }
    return (double) passed / total;
  }
}
