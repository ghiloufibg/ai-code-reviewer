package com.ghiloufi.aicode.agentworker.mapper;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.TestFailure;
import com.ghiloufi.aicode.core.domain.model.TestResult;
import com.ghiloufi.aicode.core.domain.model.TestResults;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TestResultMapper {

  public ReviewResult.Issue mapFailedTest(TestResult test) {
    final var testFile = convertClassToPath(test.testClass());

    return ReviewResult.Issue.issueBuilder()
        .file(testFile)
        .startLine(1)
        .severity("error")
        .title("Test Failed: " + test.testMethod())
        .suggestion(formatSuggestion(test))
        .confidenceScore(1.0)
        .confidenceExplanation("Test execution result - deterministic outcome")
        .build();
  }

  public List<ReviewResult.Issue> mapFailedTests(List<TestResult> tests) {
    return tests.stream().filter(TestResult::isFailure).map(this::mapFailedTest).toList();
  }

  public ReviewResult.Issue mapTestFailure(TestFailure failure) {
    return ReviewResult.Issue.issueBuilder()
        .file(failure.classToPath())
        .startLine(failure.extractLineNumber())
        .severity("error")
        .title("Test Failed: " + failure.testMethod())
        .suggestion(formatFailureSuggestion(failure))
        .confidenceScore(1.0)
        .confidenceExplanation("CI/CD test execution result - deterministic outcome")
        .build();
  }

  public List<ReviewResult.Issue> mapTestFailures(TestResults testResults) {
    if (testResults == null || !testResults.hasFailures()) {
      return List.of();
    }
    return testResults.failures().stream().map(this::mapTestFailure).toList();
  }

  private String convertClassToPath(String testClass) {
    if (testClass == null || testClass.equals("unknown")) {
      return "tests/unknown";
    }
    return testClass.replace('.', '/') + ".java";
  }

  private String formatSuggestion(TestResult test) {
    final var sb = new StringBuilder();
    sb.append("Test `").append(test.testMethod()).append("` failed.");

    if (test.failureMessage() != null && !test.failureMessage().isBlank()) {
      sb.append("\n\n**Failure Message:**\n```\n");
      sb.append(test.failureMessage());
      sb.append("\n```");
    }

    sb.append("\n\nPlease review the test implementation or the code under test.");

    return sb.toString();
  }

  private String formatFailureSuggestion(TestFailure failure) {
    final var sb = new StringBuilder();
    sb.append("Test `").append(failure.testMethod()).append("` failed in CI/CD.");

    if (failure.hasMessage()) {
      sb.append("\n\n**Failure Message:**\n```\n");
      sb.append(failure.message());
      sb.append("\n```");
    }

    if (failure.hasStackTrace()) {
      sb.append("\n\n**Stack Trace:**\n```\n");
      sb.append(truncateStackTrace(failure.stackTrace()));
      sb.append("\n```");
    }

    sb.append("\n\nPlease review the test implementation or the code under test.");

    return sb.toString();
  }

  private String truncateStackTrace(String stackTrace) {
    final int maxLength = 500;
    if (stackTrace.length() <= maxLength) {
      return stackTrace;
    }
    return stackTrace.substring(0, maxLength) + "\n... (truncated)";
  }
}
