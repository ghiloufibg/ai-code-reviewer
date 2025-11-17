package com.ghiloufi.aicode.core.security.model;

import com.github.javaparser.Range;
import lombok.Builder;

@Builder
public record SecurityIssue(
    Severity severity,
    String category,
    String description,
    String cwe,
    Range location,
    String recommendation) {

  public SecurityIssue {
    if (severity == null) {
      throw new IllegalArgumentException("Severity cannot be null");
    }
    if (category == null || category.isBlank()) {
      throw new IllegalArgumentException("Category cannot be null or blank");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Description cannot be null or blank");
    }
  }

  public static SecurityIssue create(
      final Severity severity, final String category, final String description) {
    return SecurityIssue.builder()
        .severity(severity)
        .category(category)
        .description(description)
        .build();
  }

  public static SecurityIssue critical(final String category, final String description) {
    return create(Severity.CRITICAL, category, description);
  }

  public static SecurityIssue high(final String category, final String description) {
    return create(Severity.HIGH, category, description);
  }

  public static SecurityIssue medium(final String category, final String description) {
    return create(Severity.MEDIUM, category, description);
  }

  public static SecurityIssue low(final String category, final String description) {
    return create(Severity.LOW, category, description);
  }
}
