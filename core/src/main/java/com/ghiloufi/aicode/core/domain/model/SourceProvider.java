package com.ghiloufi.aicode.core.domain.model;

import lombok.Getter;

@Getter
public enum SourceProvider {
  GITHUB("GitHub"),
  GITLAB("GitLab");

  private final String displayName;

  SourceProvider(final String displayName) {
    this.displayName = displayName;
  }

  public static SourceProvider fromString(final String value) {
    if (value == null) {
      throw new IllegalArgumentException("Provider name cannot be null");
    }

    for (final SourceProvider provider : values()) {
      if (provider.name().equalsIgnoreCase(value)) {
        return provider;
      }
    }

    throw new IllegalArgumentException("Unknown provider: " + value);
  }
}
