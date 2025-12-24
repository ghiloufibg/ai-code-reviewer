package com.ghiloufi.aicode.agentworker.mapper;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.TestResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TestResultMapper {

  private static final String SOURCE_TEST_EXECUTION = "test-execution";

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
}
