package com.ghiloufi.security.service;

import com.ghiloufi.security.model.SecurityFinding;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContextAwareSecurityAnalyzer {

  public List<SecurityFinding> filterFalsePositives(
      final List<SecurityFinding> findings, final String code) {
    if (findings == null || findings.isEmpty()) {
      return List.of();
    }

    if (code == null) {
      return findings;
    }

    return findings.stream().filter(finding -> !isFalsePositive(finding, code)).toList();
  }

  private boolean isFalsePositive(final SecurityFinding finding, final String code) {
    if (finding.type().contains("SQL_INJECTION")) {
      return isPreparedStatementUsage(finding, code);
    }

    if (finding.type().contains("XSS")) {
      return isFrameworkProtected(finding, code);
    }

    if (finding.type().contains("HARDCODED")) {
      return isTestOrExampleCode(code);
    }

    return false;
  }

  private boolean isPreparedStatementUsage(final SecurityFinding finding, final String code) {
    final String[] lines = code.split("\n");
    if (finding.line() < 1 || finding.line() > lines.length) {
      return false;
    }

    final String targetLine = lines[finding.line() - 1];
    final boolean hasPreparedStatement = code.contains("PreparedStatement");
    final boolean hasPlaceholder = code.contains("?");
    final boolean hasStringConcatenation = targetLine.contains("+");

    return hasPreparedStatement && hasPlaceholder && !hasStringConcatenation;
  }

  private boolean isFrameworkProtected(final SecurityFinding finding, final String code) {
    return code.contains("th:text") || code.contains("<c:out");
  }

  private boolean isTestOrExampleCode(final String code) {
    return code.contains("@Test") || code.contains("class Test") || code.contains("class Example");
  }
}
