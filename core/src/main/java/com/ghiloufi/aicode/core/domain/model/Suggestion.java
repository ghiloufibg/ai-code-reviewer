package com.ghiloufi.aicode.core.domain.model;

import java.util.List;
import java.util.Objects;

public record Suggestion(
    String id,
    String file,
    int startLine,
    int endLine,
    SuggestionType type,
    String title,
    String description,
    String currentCode,
    String suggestedCode,
    String rationale,
    double confidence,
    List<String> tags,
    SuggestionSource source) {

  public Suggestion {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(file, "file must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(title, "title must not be null");
    Objects.requireNonNull(source, "source must not be null");
    tags = tags != null ? List.copyOf(tags) : List.of();

    if (confidence < 0 || confidence > 1) {
      throw new IllegalArgumentException("confidence must be between 0 and 1");
    }
    if (startLine < 0) {
      throw new IllegalArgumentException("startLine must be non-negative");
    }
    if (endLine < startLine) {
      throw new IllegalArgumentException("endLine must be >= startLine");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Suggestion fromIssue(final ReviewResult.Issue issue, final String suggestedFix) {
    return builder()
        .id(java.util.UUID.randomUUID().toString())
        .file(issue.getFile())
        .startLine(issue.getStartLine())
        .endLine(issue.getStartLine())
        .type(mapSeverityToType(issue.getSeverity()))
        .title(issue.getTitle())
        .description(issue.getSuggestion())
        .suggestedCode(suggestedFix)
        .rationale(issue.getConfidenceExplanation())
        .confidence(issue.getConfidenceScore() != null ? issue.getConfidenceScore() : 0.8)
        .source(SuggestionSource.LLM_REVIEW)
        .build();
  }

  public boolean hasCodeChange() {
    return suggestedCode != null && !suggestedCode.isBlank();
  }

  public boolean isHighConfidence() {
    return confidence >= 0.8;
  }

  public boolean affectsLine(final int lineNumber) {
    return lineNumber >= startLine && lineNumber <= endLine;
  }

  public int lineCount() {
    return endLine - startLine + 1;
  }

  private static SuggestionType mapSeverityToType(final String severity) {
    if (severity == null) {
      return SuggestionType.IMPROVEMENT;
    }
    return switch (severity.toLowerCase()) {
      case "critical", "blocker", "error" -> SuggestionType.FIX;
      case "warning", "high" -> SuggestionType.REFACTOR;
      case "info", "medium" -> SuggestionType.IMPROVEMENT;
      case "low", "suggestion" -> SuggestionType.OPTIMIZATION;
      default -> SuggestionType.IMPROVEMENT;
    };
  }

  public enum SuggestionType {
    FIX,
    REFACTOR,
    IMPROVEMENT,
    OPTIMIZATION,
    SECURITY,
    PERFORMANCE,
    STYLE
  }

  public enum SuggestionSource {
    LLM_REVIEW,
    TEST_FAILURE,
    STATIC_ANALYSIS,
    SECURITY_SCAN,
    MANUAL
  }

  public static final class Builder {
    private String id;
    private String file;
    private int startLine = 0;
    private int endLine = 0;
    private SuggestionType type = SuggestionType.IMPROVEMENT;
    private String title;
    private String description;
    private String currentCode;
    private String suggestedCode;
    private String rationale;
    private double confidence = 0.8;
    private List<String> tags = List.of();
    private SuggestionSource source = SuggestionSource.LLM_REVIEW;

    private Builder() {}

    public Builder id(final String id) {
      this.id = id;
      return this;
    }

    public Builder file(final String file) {
      this.file = file;
      return this;
    }

    public Builder startLine(final int startLine) {
      this.startLine = startLine;
      return this;
    }

    public Builder endLine(final int endLine) {
      this.endLine = endLine;
      return this;
    }

    public Builder type(final SuggestionType type) {
      this.type = type;
      return this;
    }

    public Builder title(final String title) {
      this.title = title;
      return this;
    }

    public Builder description(final String description) {
      this.description = description;
      return this;
    }

    public Builder currentCode(final String currentCode) {
      this.currentCode = currentCode;
      return this;
    }

    public Builder suggestedCode(final String suggestedCode) {
      this.suggestedCode = suggestedCode;
      return this;
    }

    public Builder rationale(final String rationale) {
      this.rationale = rationale;
      return this;
    }

    public Builder confidence(final double confidence) {
      this.confidence = confidence;
      return this;
    }

    public Builder tags(final List<String> tags) {
      this.tags = tags;
      return this;
    }

    public Builder source(final SuggestionSource source) {
      this.source = source;
      return this;
    }

    public Suggestion build() {
      if (id == null) {
        id = java.util.UUID.randomUUID().toString();
      }
      if (endLine < startLine) {
        endLine = startLine;
      }
      return new Suggestion(
          id,
          file,
          startLine,
          endLine,
          type,
          title,
          description,
          currentCode,
          suggestedCode,
          rationale,
          confidence,
          tags,
          source);
    }
  }
}
