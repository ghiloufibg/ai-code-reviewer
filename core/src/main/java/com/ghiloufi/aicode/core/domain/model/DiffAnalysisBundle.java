package com.ghiloufi.aicode.core.domain.model;

import java.util.Objects;

public record DiffAnalysisBundle(
    RepositoryIdentifier repositoryIdentifier,
    GitDiffDocument structuredDiff,
    String rawDiffText,
    PrMetadata prMetadata) {

  public DiffAnalysisBundle {
    Objects.requireNonNull(repositoryIdentifier, "Le repository identifier ne peut pas être null");
    Objects.requireNonNull(structuredDiff, "Le diff structuré ne peut pas être null");
    Objects.requireNonNull(rawDiffText, "Le texte brut du diff ne peut pas être null");

    if (rawDiffText.trim().isEmpty()) {
      throw new IllegalArgumentException("Le texte brut du diff ne peut pas être vide");
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
      return "Diff vide - aucune modification";
    }

    return String.format(
        "Diff: %d fichier(s) modifié(s), %d ligne(s) total",
        getModifiedFileCount(), getTotalLineCount());
  }

  @Override
  public String toString() {
    return String.format(
        "DiffAnalysisBundle[%s, rawText=%d caractères]", getSummary(), rawDiffText.length());
  }
}
