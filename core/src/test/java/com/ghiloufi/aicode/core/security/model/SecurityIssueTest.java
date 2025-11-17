package com.ghiloufi.aicode.core.security.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityIssue Tests")
final class SecurityIssueTest {

  @Test
  @DisplayName("should_create_security_issue_with_all_fields")
  final void should_create_with_all_fields() {
    final SecurityIssue issue =
        SecurityIssue.builder()
            .severity(Severity.CRITICAL)
            .category("COMMAND_INJECTION")
            .description("Runtime.exec with user input")
            .cwe("CWE-78")
            .recommendation("Use parameterized command array")
            .build();

    assertThat(issue.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issue.category()).isEqualTo("COMMAND_INJECTION");
    assertThat(issue.description()).isEqualTo("Runtime.exec with user input");
    assertThat(issue.cwe()).isEqualTo("CWE-78");
    assertThat(issue.recommendation()).isEqualTo("Use parameterized command array");
  }

  @Test
  @DisplayName("should_create_critical_severity_issue_with_factory_method")
  final void should_create_critical_issue() {
    final SecurityIssue issue =
        SecurityIssue.critical("COMMAND_INJECTION", "Dangerous command execution");

    assertThat(issue.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issue.category()).isEqualTo("COMMAND_INJECTION");
    assertThat(issue.description()).isEqualTo("Dangerous command execution");
  }

  @Test
  @DisplayName("should_create_high_severity_issue_with_factory_method")
  final void should_create_high_issue() {
    final SecurityIssue issue = SecurityIssue.high("REFLECTION_ABUSE", "Class.forName detected");

    assertThat(issue.severity()).isEqualTo(Severity.HIGH);
    assertThat(issue.category()).isEqualTo("REFLECTION_ABUSE");
  }

  @Test
  @DisplayName("should_create_medium_severity_issue_with_factory_method")
  final void should_create_medium_issue() {
    final SecurityIssue issue = SecurityIssue.medium("PATH_TRAVERSAL", "File path concatenation");

    assertThat(issue.severity()).isEqualTo(Severity.MEDIUM);
    assertThat(issue.category()).isEqualTo("PATH_TRAVERSAL");
  }

  @Test
  @DisplayName("should_throw_exception_when_severity_is_null")
  final void should_reject_null_severity() {
    assertThatThrownBy(
            () ->
                SecurityIssue.builder()
                    .severity(null)
                    .category("TEST")
                    .description("Test description")
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Severity cannot be null");
  }

  @Test
  @DisplayName("should_throw_exception_when_category_is_null")
  final void should_reject_null_category() {
    assertThatThrownBy(
            () ->
                SecurityIssue.builder()
                    .severity(Severity.CRITICAL)
                    .category(null)
                    .description("Test description")
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Category cannot be null or blank");
  }

  @Test
  @DisplayName("should_throw_exception_when_category_is_blank")
  final void should_reject_blank_category() {
    assertThatThrownBy(
            () ->
                SecurityIssue.builder()
                    .severity(Severity.CRITICAL)
                    .category("   ")
                    .description("Test description")
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Category cannot be null or blank");
  }

  @Test
  @DisplayName("should_throw_exception_when_description_is_null")
  final void should_reject_null_description() {
    assertThatThrownBy(
            () ->
                SecurityIssue.builder()
                    .severity(Severity.CRITICAL)
                    .category("TEST")
                    .description(null)
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Description cannot be null or blank");
  }
}
