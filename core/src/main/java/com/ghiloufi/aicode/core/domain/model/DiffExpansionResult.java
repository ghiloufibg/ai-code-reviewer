package com.ghiloufi.aicode.core.domain.model;

import java.util.List;

public record DiffExpansionResult(
    List<ExpandedFileContext> expandedFiles,
    int totalFilesRequested,
    int filesExpanded,
    int filesSkipped,
    String skipReason) {

  public DiffExpansionResult {
    expandedFiles = expandedFiles != null ? List.copyOf(expandedFiles) : List.of();

    if (totalFilesRequested < 0) {
      throw new IllegalArgumentException("Total files requested cannot be negative");
    }

    if (filesExpanded < 0) {
      throw new IllegalArgumentException("Files expanded cannot be negative");
    }

    if (filesSkipped < 0) {
      throw new IllegalArgumentException("Files skipped cannot be negative");
    }
  }

  public boolean hasExpandedFiles() {
    return !expandedFiles.isEmpty();
  }

  public int totalContentLength() {
    return expandedFiles.stream().mapToInt(ExpandedFileContext::contentLength).sum();
  }

  public int totalLineCount() {
    return expandedFiles.stream().mapToInt(ExpandedFileContext::lineCount).sum();
  }

  public long truncatedFileCount() {
    return expandedFiles.stream().filter(ExpandedFileContext::truncated).count();
  }

  public static DiffExpansionResult empty() {
    return new DiffExpansionResult(List.of(), 0, 0, 0, null);
  }

  public static DiffExpansionResult disabled() {
    return new DiffExpansionResult(List.of(), 0, 0, 0, "Feature disabled");
  }
}
