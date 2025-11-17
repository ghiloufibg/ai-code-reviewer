package com.ghiloufi.aicode.core.security.model;

import java.util.regex.Pattern;
import lombok.Builder;

@Builder
public record DangerousPattern(
    String name, Pattern pattern, Severity severity, String description, String recommendation) {

  public DangerousPattern {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Pattern name cannot be null or blank");
    }
    if (pattern == null) {
      throw new IllegalArgumentException("Pattern cannot be null");
    }
    if (severity == null) {
      throw new IllegalArgumentException("Severity cannot be null");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Description cannot be null or blank");
    }
  }

  public boolean matches(final String code) {
    if (code == null || code.isBlank()) {
      return false;
    }
    return pattern.matcher(code).find();
  }

  public static DangerousPattern create(
      final String name,
      final String regex,
      final Severity severity,
      final String description,
      final String recommendation) {
    return DangerousPattern.builder()
        .name(name)
        .pattern(Pattern.compile(regex))
        .severity(severity)
        .description(description)
        .recommendation(recommendation)
        .build();
  }
}
