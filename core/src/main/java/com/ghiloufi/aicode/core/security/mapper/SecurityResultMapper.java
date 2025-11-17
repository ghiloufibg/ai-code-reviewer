package com.ghiloufi.aicode.core.security.mapper;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.security.detector.SensitiveFileDetector;
import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.ghiloufi.aicode.core.security.model.Severity;
import org.springframework.stereotype.Component;

@Component
public final class SecurityResultMapper {

  private final SensitiveFileDetector fileDetector;

  public SecurityResultMapper(final SensitiveFileDetector fileDetector) {
    this.fileDetector = fileDetector;
  }

  public ReviewResult.Issue toReviewIssue(
      final SecurityIssue securityIssue, final String filePath) {
    return toReviewIssue(securityIssue, filePath, 0);
  }

  public ReviewResult.Issue toReviewIssue(
      final SecurityIssue securityIssue, final String filePath, final int lineNumber) {
    final ReviewResult.Issue issue = new ReviewResult.Issue();

    issue.file = filePath;
    issue.start_line =
        lineNumber == 0 ? extractLineNumberFromDescription(securityIssue) : lineNumber;
    issue.severity = mapSeverity(securityIssue.severity());
    issue.title = formatTitle(securityIssue);
    issue.suggestion = formatSuggestion(securityIssue);
    issue.confidenceScore = calculateConfidence(securityIssue.severity(), filePath);
    issue.confidenceExplanation = buildConfidenceExplanation(securityIssue.severity(), filePath);
    issue.suggestedFix = securityIssue.recommendation();

    return issue;
  }

  private int extractLineNumberFromDescription(final SecurityIssue securityIssue) {
    if (securityIssue.description() == null) {
      return 0;
    }

    final String linePattern = "line ";
    final int lineIndex = securityIssue.description().indexOf(linePattern);
    if (lineIndex == -1) {
      return 0;
    }

    try {
      final int startIndex = lineIndex + linePattern.length();
      int endIndex = securityIssue.description().indexOf(' ', startIndex);
      if (endIndex == -1) {
        endIndex = securityIssue.description().indexOf('.', startIndex);
      }
      if (endIndex == -1) {
        endIndex = securityIssue.description().indexOf(')', startIndex);
      }
      if (endIndex == -1) {
        endIndex = securityIssue.description().length();
      }

      if (endIndex > startIndex) {
        return Integer.parseInt(securityIssue.description().substring(startIndex, endIndex));
      }
    } catch (final NumberFormatException e) {
      return 0;
    }

    return 0;
  }

  private String mapSeverity(final Severity severity) {
    return switch (severity) {
      case CRITICAL -> "critical";
      case HIGH -> "high";
      case MEDIUM -> "medium";
      case LOW -> "low";
      case INFO -> "info";
    };
  }

  private String formatTitle(final SecurityIssue issue) {
    return String.format("[Security] %s", issue.category());
  }

  private String formatSuggestion(final SecurityIssue issue) {
    final StringBuilder suggestion = new StringBuilder();
    suggestion.append("**Security Issue Detected**\n\n");
    suggestion.append(issue.description()).append("\n\n");

    if (issue.recommendation() != null && !issue.recommendation().isBlank()) {
      suggestion.append("**Recommendation:**\n");
      suggestion.append(issue.recommendation());
    }

    return suggestion.toString();
  }

  private double calculateConfidence(final Severity severity, final String filePath) {
    final double baseConfidence = getBaseConfidence(severity);
    final double requiredThreshold = fileDetector.getRequiredConfidenceThreshold(filePath);

    if (requiredThreshold > 0.9) {
      return Math.max(baseConfidence, requiredThreshold);
    }

    return baseConfidence;
  }

  private double getBaseConfidence(final Severity severity) {
    return switch (severity) {
      case CRITICAL -> 0.95;
      case HIGH -> 0.85;
      case MEDIUM -> 0.75;
      case LOW -> 0.65;
      case INFO -> 0.50;
    };
  }

  private String buildConfidenceExplanation(final Severity severity, final String filePath) {
    final boolean isSensitive = fileDetector.isSensitiveFile(filePath);
    final String sensitivityReason = fileDetector.getSensitivityReason(filePath);

    if (isSensitive && sensitivityReason != null) {
      return String.format(
          "High confidence security issue (%s severity). %s - requires careful review.",
          severity.name().toLowerCase(), sensitivityReason);
    }

    return String.format(
        "Security issue detected with %s severity.", severity.name().toLowerCase());
  }
}
