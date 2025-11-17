package com.ghiloufi.aicode.core.security.model;

import java.util.List;
import lombok.Builder;

@Builder
public record ValidationResult(
    boolean approved,
    List<SecurityIssue> issues,
    double riskScore,
    boolean requiresManualApproval,
    String rejectionReason) {

  public ValidationResult(
      final boolean approved,
      final List<SecurityIssue> issues,
      final double riskScore,
      final boolean requiresManualApproval,
      final String rejectionReason) {
    this.approved = approved;
    this.issues = issues != null ? issues : List.of();
    this.riskScore = riskScore;
    this.requiresManualApproval = requiresManualApproval;
    this.rejectionReason = rejectionReason;
  }

  public static ValidationResult createApproved() {
    return ValidationResult.builder()
        .approved(true)
        .issues(List.of())
        .riskScore(0.0)
        .requiresManualApproval(false)
        .build();
  }

  public static ValidationResult createRejected(
      final String reason, final List<SecurityIssue> issues) {
    return ValidationResult.builder()
        .approved(false)
        .issues(issues)
        .riskScore(calculateRiskScore(issues))
        .requiresManualApproval(false)
        .rejectionReason(reason)
        .build();
  }

  public static ValidationResult createManualApprovalRequired(
      final String reason, final List<SecurityIssue> issues) {
    return ValidationResult.builder()
        .approved(false)
        .issues(issues)
        .riskScore(calculateRiskScore(issues))
        .requiresManualApproval(true)
        .rejectionReason(reason)
        .build();
  }

  private static double calculateRiskScore(final List<SecurityIssue> issues) {
    if (issues == null || issues.isEmpty()) {
      return 0.0;
    }
    return issues.stream().mapToDouble(issue -> issue.severity().getWeight()).sum();
  }

  public boolean hasCriticalIssues() {
    return issues.stream().anyMatch(issue -> issue.severity() == Severity.CRITICAL);
  }

  public boolean hasHighOrCriticalIssues() {
    return issues.stream()
        .anyMatch(
            issue -> issue.severity() == Severity.CRITICAL || issue.severity() == Severity.HIGH);
  }
}
