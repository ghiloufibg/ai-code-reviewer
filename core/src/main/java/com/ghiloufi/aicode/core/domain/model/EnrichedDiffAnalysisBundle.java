package com.ghiloufi.aicode.core.domain.model;

import java.util.Objects;

public record EnrichedDiffAnalysisBundle(
    RepositoryIdentifier repositoryIdentifier,
    GitDiffDocument structuredDiff,
    String rawDiffText,
    ContextRetrievalResult contextResult,
    PrMetadata prMetadata) {

  public EnrichedDiffAnalysisBundle {
    Objects.requireNonNull(repositoryIdentifier, "Repository identifier cannot be null");
    Objects.requireNonNull(structuredDiff, "Structured diff cannot be null");
    Objects.requireNonNull(rawDiffText, "Raw diff text cannot be null");
    Objects.requireNonNull(contextResult, "Context result cannot be null");

    if (rawDiffText.trim().isEmpty()) {
      throw new IllegalArgumentException("Raw diff text cannot be empty");
    }

    if (prMetadata == null) {
      prMetadata = PrMetadata.empty();
    }
  }

  public EnrichedDiffAnalysisBundle(final DiffAnalysisBundle originalBundle) {
    this(
        originalBundle.repositoryIdentifier(),
        originalBundle.structuredDiff(),
        originalBundle.rawDiffText(),
        ContextRetrievalResult.empty(),
        originalBundle.prMetadata());
  }

  public EnrichedDiffAnalysisBundle withContext(final ContextRetrievalResult context) {
    Objects.requireNonNull(context, "Context cannot be null");
    return new EnrichedDiffAnalysisBundle(
        this.repositoryIdentifier, this.structuredDiff, this.rawDiffText, context, this.prMetadata);
  }

  public DiffAnalysisBundle toBasicBundle() {
    return new DiffAnalysisBundle(repositoryIdentifier, structuredDiff, rawDiffText, prMetadata);
  }

  public boolean hasContext() {
    return !contextResult.isEmpty();
  }

  public int getTotalLineCount() {
    return structuredDiff.getTotalLineCount();
  }

  public int getModifiedFileCount() {
    return structuredDiff.getFileCount();
  }

  public int getContextMatchCount() {
    return contextResult.getTotalMatches();
  }

  public String getSummary() {
    final String baseSummary =
        String.format(
            "Diff: %d file(s) modified, %d line(s) total",
            getModifiedFileCount(), getTotalLineCount());

    if (hasContext()) {
      return baseSummary + String.format(", %d context file(s)", getContextMatchCount());
    }

    return baseSummary;
  }
}
