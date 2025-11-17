package com.ghiloufi.aicode.core.security.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ValidationResult Tests")
final class ValidationResultTest {

  @Test
  @DisplayName("should_create_approved_result_with_factory_method")
  final void should_create_approved_result() {
    final ValidationResult result = ValidationResult.createApproved();

    assertThat(result.approved()).isTrue();
    assertThat(result.issues()).isEmpty();
    assertThat(result.riskScore()).isEqualTo(0.0);
    assertThat(result.requiresManualApproval()).isFalse();
    assertThat(result.rejectionReason()).isNull();
  }

  @Test
  @DisplayName("should_create_rejected_result_with_issues")
  final void should_create_rejected_result() {
    final List<SecurityIssue> issues =
        List.of(
            SecurityIssue.critical("COMMAND_INJECTION", "Runtime.exec detected"),
            SecurityIssue.high("REFLECTION_ABUSE", "Class.forName detected"));

    final ValidationResult result =
        ValidationResult.createRejected("Critical issues found", issues);

    assertThat(result.approved()).isFalse();
    assertThat(result.issues()).hasSize(2);
    assertThat(result.riskScore()).isEqualTo(17.0); // 10.0 + 7.0
    assertThat(result.requiresManualApproval()).isFalse();
    assertThat(result.rejectionReason()).isEqualTo("Critical issues found");
  }

  @Test
  @DisplayName("should_create_manual_approval_required_result")
  final void should_create_manual_approval_result() {
    final List<SecurityIssue> issues =
        List.of(SecurityIssue.medium("PATH_TRAVERSAL", "File path concatenation"));

    final ValidationResult result =
        ValidationResult.createManualApprovalRequired("Sensitive file modification", issues);

    assertThat(result.approved()).isFalse();
    assertThat(result.issues()).hasSize(1);
    assertThat(result.riskScore()).isEqualTo(4.0); // MEDIUM severity
    assertThat(result.requiresManualApproval()).isTrue();
    assertThat(result.rejectionReason()).isEqualTo("Sensitive file modification");
  }

  @Test
  @DisplayName("should_calculate_risk_score_correctly")
  final void should_calculate_risk_score() {
    final List<SecurityIssue> issues =
        List.of(
            SecurityIssue.critical("TEST1", "Critical issue"), // 10.0
            SecurityIssue.high("TEST2", "High issue"), // 7.0
            SecurityIssue.medium("TEST3", "Medium issue"), // 4.0
            SecurityIssue.low("TEST4", "Low issue")); // 1.0

    final ValidationResult result = ValidationResult.createRejected("Multiple issues", issues);

    assertThat(result.riskScore()).isEqualTo(22.0);
  }

  @Test
  @DisplayName("should_detect_critical_issues")
  final void should_detect_critical_issues() {
    final List<SecurityIssue> issues =
        List.of(
            SecurityIssue.medium("TEST1", "Medium issue"),
            SecurityIssue.critical("TEST2", "Critical issue"));

    final ValidationResult result = ValidationResult.createRejected("Issues found", issues);

    assertThat(result.hasCriticalIssues()).isTrue();
  }

  @Test
  @DisplayName("should_detect_no_critical_issues_when_only_lower_severities")
  final void should_not_detect_critical_when_absent() {
    final List<SecurityIssue> issues =
        List.of(
            SecurityIssue.medium("TEST1", "Medium issue"), SecurityIssue.low("TEST2", "Low issue"));

    final ValidationResult result = ValidationResult.createRejected("Issues found", issues);

    assertThat(result.hasCriticalIssues()).isFalse();
  }

  @Test
  @DisplayName("should_detect_high_or_critical_issues")
  final void should_detect_high_or_critical() {
    final List<SecurityIssue> issues =
        List.of(
            SecurityIssue.medium("TEST1", "Medium issue"),
            SecurityIssue.high("TEST2", "High issue"));

    final ValidationResult result = ValidationResult.createRejected("Issues found", issues);

    assertThat(result.hasHighOrCriticalIssues()).isTrue();
  }

  @Test
  @DisplayName("should_handle_null_issues_list_gracefully")
  final void should_handle_null_issues() {
    final ValidationResult result =
        ValidationResult.builder()
            .approved(true)
            .issues(null)
            .riskScore(0.0)
            .requiresManualApproval(false)
            .build();

    assertThat(result.issues()).isNotNull();
    assertThat(result.issues()).isEmpty();
  }

  @Test
  @DisplayName("should_calculate_zero_risk_score_for_empty_issues")
  final void should_calculate_zero_for_empty() {
    final ValidationResult result = ValidationResult.createRejected("No issues", List.of());

    assertThat(result.riskScore()).isEqualTo(0.0);
  }
}
