package com.ghiloufi.security.service;

import com.ghiloufi.security.model.SecurityFinding;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FindingConfidenceScorer {

  private static final Set<String> HIGH_CVSS_CWES =
      Set.of(
          "CWE-89", // SQL Injection
          "CWE-78", // OS Command Injection
          "CWE-79", // XSS
          "CWE-502", // Deserialization
          "CWE-798" // Hardcoded Credentials
          );

  public SecurityFinding enrichWithConfidenceScore(
      final SecurityFinding finding, final String code) {
    final double confidence = calculateConfidence(finding, code);

    return new SecurityFinding(
        finding.type(),
        finding.severity(),
        finding.line(),
        finding.message(),
        finding.recommendation(),
        finding.cweId(),
        finding.owaspCategory(),
        confidence);
  }

  private double calculateConfidence(final SecurityFinding finding, final String code) {
    double confidence = 0.5;

    if (finding.severity().equals("CRITICAL") || finding.severity().equals("HIGH")) {
      confidence += 0.1;
    }

    if (hasHighCvssScore(finding.cweId())) {
      confidence += 0.2;
    }

    if (code != null && hasSafePattern(finding, code)) {
      confidence -= 0.2;
    }

    return Math.max(0.0, Math.min(1.0, confidence));
  }

  private boolean hasHighCvssScore(final String cweId) {
    return HIGH_CVSS_CWES.contains(cweId);
  }

  private boolean hasSafePattern(final SecurityFinding finding, final String code) {
    if (finding.type().contains("SQL_INJECTION")) {
      return code.contains("PreparedStatement") && code.contains("?");
    }

    if (finding.type().contains("XSS")) {
      return code.contains("th:text") || code.contains("<c:out");
    }

    return false;
  }
}
