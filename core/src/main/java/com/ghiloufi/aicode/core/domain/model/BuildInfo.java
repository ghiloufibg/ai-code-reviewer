package com.ghiloufi.aicode.core.domain.model;

import java.util.Objects;

public record BuildInfo(
    String language, String languageVersion, String buildSystem, String testFramework) {

  public BuildInfo {
    Objects.requireNonNull(language, "language must not be null");
  }

  public static BuildInfo unknown() {
    return new BuildInfo("unknown", null, null, null);
  }

  public static BuildInfo of(final String language) {
    return new BuildInfo(language, null, null, null);
  }

  public static BuildInfo of(final String language, final String languageVersion) {
    return new BuildInfo(language, languageVersion, null, null);
  }

  public boolean hasLanguageVersion() {
    return languageVersion != null && !languageVersion.isBlank();
  }

  public boolean hasBuildSystem() {
    return buildSystem != null && !buildSystem.isBlank();
  }

  public boolean hasTestFramework() {
    return testFramework != null && !testFramework.isBlank();
  }
}
