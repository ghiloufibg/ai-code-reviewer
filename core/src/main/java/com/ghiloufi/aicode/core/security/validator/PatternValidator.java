package com.ghiloufi.aicode.core.security.validator;

import com.ghiloufi.aicode.core.security.model.DangerousPattern;
import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class PatternValidator {

  private final List<DangerousPattern> dangerousPatterns;

  public PatternValidator(final List<DangerousPattern> dangerousPatterns) {
    this.dangerousPatterns = dangerousPatterns;
    log.info("PatternValidator initialized with {} dangerous patterns", dangerousPatterns.size());
  }

  public List<SecurityIssue> validateCode(final String code) {
    if (code == null || code.isBlank()) {
      return List.of();
    }

    final List<SecurityIssue> issues = new ArrayList<>();

    for (final DangerousPattern pattern : dangerousPatterns) {
      if (pattern.matches(code)) {
        log.debug("Dangerous pattern detected: {}", pattern.name());
        issues.add(
            SecurityIssue.builder()
                .severity(pattern.severity())
                .category(pattern.name())
                .description(pattern.description())
                .recommendation(pattern.recommendation())
                .build());
      }
    }

    return issues;
  }

  public List<SecurityIssue> validateCodeWithLineNumbers(final String code) {
    if (code == null || code.isBlank()) {
      return List.of();
    }

    final List<SecurityIssue> issues = new ArrayList<>();
    final String[] lines = code.split("\\r?\\n");

    for (int lineNum = 0; lineNum < lines.length; lineNum++) {
      final String line = lines[lineNum];
      final int lineNumber = lineNum + 1;

      for (final DangerousPattern pattern : dangerousPatterns) {
        if (pattern.matches(line)) {
          log.debug("Dangerous pattern detected at line {}: {}", lineNumber, pattern.name());
          issues.add(
              SecurityIssue.builder()
                  .severity(pattern.severity())
                  .category(pattern.name())
                  .description(
                      String.format("%s (detected at line %d)", pattern.description(), lineNumber))
                  .recommendation(pattern.recommendation())
                  .build());
        }
      }
    }

    return issues;
  }
}
