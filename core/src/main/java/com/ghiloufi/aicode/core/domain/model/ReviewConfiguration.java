package com.ghiloufi.aicode.core.domain.model;

public record ReviewConfiguration(
    ReviewFocus focus,
    SeverityThreshold minSeverity,
    boolean includePositiveFeedback,
    String customInstructions,
    String programmingLanguage,
    String llmProvider,
    String llmModel) {

  public enum ReviewFocus {
    COMPREHENSIVE
  }

  public enum SeverityThreshold {
    LOW
  }

  public static final ReviewConfiguration DEFAULT =
      new ReviewConfiguration(
          ReviewFocus.COMPREHENSIVE, SeverityThreshold.LOW, true, null, "Java", null, null);

  public static ReviewConfiguration defaults() {
    return DEFAULT;
  }

  public ReviewConfiguration withLlmMetadata(final String provider, final String model) {
    return new ReviewConfiguration(
        focus,
        minSeverity,
        includePositiveFeedback,
        customInstructions,
        programmingLanguage,
        provider,
        model);
  }
}
