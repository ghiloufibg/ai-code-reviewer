package com.ghiloufi.aicode.core.domain.model;

import java.util.regex.Pattern;

public record TestFailure(String testClass, String testMethod, String message, String stackTrace) {

  private static final Pattern LINE_NUMBER_PATTERN = Pattern.compile("\\((\\w+\\.java):(\\d+)\\)");

  public TestFailure {
    if (testClass == null || testClass.isBlank()) {
      testClass = "unknown";
    }
    if (testMethod == null || testMethod.isBlank()) {
      testMethod = "unknown";
    }
  }

  public static TestFailure of(final String testClass, final String testMethod) {
    return new TestFailure(testClass, testMethod, null, null);
  }

  public static TestFailure of(
      final String testClass, final String testMethod, final String message) {
    return new TestFailure(testClass, testMethod, message, null);
  }

  public String fullyQualifiedName() {
    return testClass + "#" + testMethod;
  }

  public String classToPath() {
    if ("unknown".equals(testClass)) {
      return "tests/unknown";
    }
    return "src/test/java/" + testClass.replace('.', '/') + ".java";
  }

  public int extractLineNumber() {
    if (stackTrace == null || stackTrace.isBlank()) {
      return 1;
    }
    final var matcher = LINE_NUMBER_PATTERN.matcher(stackTrace);
    if (matcher.find()) {
      try {
        return Integer.parseInt(matcher.group(2));
      } catch (NumberFormatException e) {
        return 1;
      }
    }
    return 1;
  }

  public boolean hasMessage() {
    return message != null && !message.isBlank();
  }

  public boolean hasStackTrace() {
    return stackTrace != null && !stackTrace.isBlank();
  }
}
