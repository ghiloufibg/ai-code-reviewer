package com.ghiloufi.aicode.core.domain.model;

import java.util.List;

public record PrMetadata(
    String title,
    String description,
    String author,
    String baseBranch,
    String headBranch,
    List<String> labels,
    List<CommitInfo> commits,
    int changedFilesCount) {

  public PrMetadata {
    labels = labels != null ? List.copyOf(labels) : List.of();
    commits = commits != null ? List.copyOf(commits) : List.of();

    if (changedFilesCount < 0) {
      throw new IllegalArgumentException("Changed files count cannot be negative");
    }
  }

  public boolean hasTitle() {
    return title != null && !title.isBlank();
  }

  public boolean hasDescription() {
    return description != null && !description.isBlank();
  }

  public boolean hasLabels() {
    return !labels.isEmpty();
  }

  public boolean hasCommits() {
    return !commits.isEmpty();
  }

  public boolean hasAuthor() {
    return author != null && !author.isBlank();
  }

  public String branchInfo() {
    if (headBranch == null || baseBranch == null) {
      return "";
    }
    return headBranch + " → " + baseBranch;
  }

  public static PrMetadata empty() {
    return new PrMetadata(null, null, null, null, null, List.of(), List.of(), 0);
  }
}
