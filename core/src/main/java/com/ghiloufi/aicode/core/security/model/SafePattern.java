package com.ghiloufi.aicode.core.security.model;

import java.util.regex.Pattern;
import lombok.Builder;

@Builder
public record SafePattern(String name, Pattern pattern, String description) {

  public SafePattern {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Pattern name cannot be null or blank");
    }
    if (pattern == null) {
      throw new IllegalArgumentException("Pattern cannot be null");
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

  public static SafePattern create(
      final String name, final String regex, final String description) {
    return SafePattern.builder()
        .name(name)
        .pattern(Pattern.compile(regex))
        .description(description)
        .build();
  }
}
