package com.ghiloufi.aicode.core.security;

import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.ghiloufi.aicode.core.security.model.Severity;
import com.ghiloufi.aicode.core.security.model.ValidationResult;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class SecurityValidator {

  private static final double HIGH_CONFIDENCE_THRESHOLD = 0.9;
  private static final double SENSITIVE_FILE_CONFIDENCE_THRESHOLD = 0.95;

  public ValidationResult validateFixDiff(
      final String fixDiff, final String filePath, final double confidenceScore) {

    log.debug("Validating fix diff for file: {}, confidence: {}", filePath, confidenceScore);

    final List<SecurityIssue> issues = new ArrayList<>();

    if (fixDiff == null || fixDiff.isBlank()) {
      return ValidationResult.createRejected("Fix diff is empty", List.of());
    }

    final boolean isSensitiveFile = isSensitiveFile(filePath);

    if (hasCriticalIssues(issues)) {
      return ValidationResult.createRejected(
          "Critical security vulnerabilities detected in fix suggestion", issues);
    }

    final boolean requiresManualApproval =
        isSensitiveFile || confidenceScore < getRequiredConfidenceThreshold(isSensitiveFile);

    if (requiresManualApproval) {
      final String reason =
          isSensitiveFile
              ? String.format(
                  "Sensitive file modification requires manual approval (confidence: %.2f, required: %.2f)",
                  confidenceScore, SENSITIVE_FILE_CONFIDENCE_THRESHOLD)
              : String.format(
                  "Low confidence score requires manual approval (confidence: %.2f, required: %.2f)",
                  confidenceScore, HIGH_CONFIDENCE_THRESHOLD);

      return ValidationResult.createManualApprovalRequired(reason, issues);
    }

    if (issues.isEmpty()) {
      log.info("Fix diff validation passed for file: {}", filePath);
      return ValidationResult.createApproved();
    }

    return ValidationResult.builder()
        .approved(true)
        .issues(issues)
        .riskScore(calculateRiskScore(issues))
        .requiresManualApproval(false)
        .build();
  }

  private boolean hasCriticalIssues(final List<SecurityIssue> issues) {
    return issues.stream().anyMatch(issue -> issue.severity() == Severity.CRITICAL);
  }

  private double calculateRiskScore(final List<SecurityIssue> issues) {
    return issues.stream().mapToDouble(issue -> issue.severity().getWeight()).sum();
  }

  private boolean isSensitiveFile(final String filePath) {
    if (filePath == null) {
      return false;
    }

    final String lowerPath = filePath.toLowerCase();

    return lowerPath.endsWith(".config")
        || lowerPath.endsWith(".properties")
        || lowerPath.endsWith(".yml")
        || lowerPath.endsWith(".yaml")
        || lowerPath.endsWith(".env")
        || lowerPath.endsWith(".key")
        || lowerPath.endsWith(".pem")
        || lowerPath.endsWith(".crt")
        || lowerPath.contains("/config/")
        || lowerPath.contains("/security/")
        || lowerPath.contains("/auth/")
        || lowerPath.contains("application.properties")
        || lowerPath.contains("application.yml")
        || lowerPath.contains("application-prod")
        || lowerPath.contains("application-production")
        || lowerPath.contains("credentials")
        || lowerPath.contains("secrets");
  }

  private double getRequiredConfidenceThreshold(final boolean isSensitiveFile) {
    return isSensitiveFile ? SENSITIVE_FILE_CONFIDENCE_THRESHOLD : HIGH_CONFIDENCE_THRESHOLD;
  }
}
