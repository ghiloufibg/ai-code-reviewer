package com.ghiloufi.aicode.core.domain.model;

import java.util.Objects;

public record DiffAnalysisBundle(
    RepositoryIdentifier repositoryIdentifier,
    GitDiffDocument structuredDiff,
    String rawDiffText,
    PrMetadata prMetadata) {

  public DiffAnalysisBundle {
    Objects.requireNonNull(repositoryIdentifier, "Repository identifier cannot be null");
    Objects.requireNonNull(structuredDiff, "Structured diff cannot be null");
    Objects.requireNonNull(rawDiffText, "Raw diff text cannot be null");

    if (rawDiffText.trim().isEmpty()) {
      throw new IllegalArgumentException("Raw diff text cannot be empty");
    }

    if (prMetadata == null) {
      prMetadata = PrMetadata.empty();
    }
  }

  public int getTotalLineCount() {
    return structuredDiff.files.stream()
        .flatMap(file -> file.diffHunkBlocks.stream())
        .mapToInt(hunk -> hunk.lines.size())
        .sum();
  }

  public int getModifiedFileCount() {
    return structuredDiff.files.size();
  }

  public String getSummary() {
    if (structuredDiff.files.isEmpty()
        || structuredDiff.files.stream().allMatch(file -> file.diffHunkBlocks.isEmpty())) {
      return "Empty diff - no modifications";
    }

    return String.format(
        "Diff: %d file(s) modified, %d line(s) total", getModifiedFileCount(), getTotalLineCount());
  }

  @Override
  public String toString() {
    return String.format(
        "DiffAnalysisBundle[%s, rawText=%d chars]", getSummary(), rawDiffText.length());
  }
}
