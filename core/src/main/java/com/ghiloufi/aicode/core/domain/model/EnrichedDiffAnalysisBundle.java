package com.ghiloufi.aicode.core.domain.model;

import java.util.Objects;
import java.util.Optional;

public record EnrichedDiffAnalysisBundle(
    GitDiffDocument structuredDiff,
    String rawDiffText,
    Optional<ContextRetrievalResult> contextResult) {

  public EnrichedDiffAnalysisBundle {
    Objects.requireNonNull(structuredDiff, "Structured diff cannot be null");
    Objects.requireNonNull(rawDiffText, "Raw diff text cannot be null");
    Objects.requireNonNull(contextResult, "Context result optional cannot be null");

    if (rawDiffText.trim().isEmpty()) {
      throw new IllegalArgumentException("Raw diff text cannot be empty");
    }
  }

  public EnrichedDiffAnalysisBundle(final DiffAnalysisBundle originalBundle) {
    this(originalBundle.structuredDiff(), originalBundle.rawDiffText(), Optional.empty());
  }

  public EnrichedDiffAnalysisBundle withContext(final ContextRetrievalResult context) {
    Objects.requireNonNull(context, "Context cannot be null");
    return new EnrichedDiffAnalysisBundle(
        this.structuredDiff, this.rawDiffText, Optional.of(context));
  }

  public DiffAnalysisBundle toBasicBundle() {
    return new DiffAnalysisBundle(structuredDiff, rawDiffText);
  }

  public boolean hasContext() {
    return contextResult.isPresent();
  }

  public int getTotalLineCount() {
    return structuredDiff.getTotalLineCount();
  }

  public int getModifiedFileCount() {
    return structuredDiff.getFileCount();
  }

  public int getContextMatchCount() {
    return contextResult.map(ContextRetrievalResult::getTotalMatches).orElse(0);
  }

  public String getSummary() {
    final String baseSummary =
        String.format(
            "Diff: %d file(s) modifié(s), %d ligne(s) total",
            getModifiedFileCount(), getTotalLineCount());

    if (hasContext()) {
      return baseSummary + String.format(", %d fichier(s) de contexte", getContextMatchCount());
    }

    return baseSummary;
  }
}
