package com.ghiloufi.aicode.core.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CommitInfo(
    String commitId, String message, String author, Instant timestamp, List<String> changedFiles) {

  public CommitInfo {
    Objects.requireNonNull(commitId, "Commit ID cannot be null");
    Objects.requireNonNull(message, "Message cannot be null");
    Objects.requireNonNull(author, "Author cannot be null");
    Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    Objects.requireNonNull(changedFiles, "Changed files cannot be null");

    if (commitId.isBlank()) {
      throw new IllegalArgumentException("Commit ID cannot be blank");
    }

    if (author.isBlank()) {
      throw new IllegalArgumentException("Author cannot be blank");
    }
  }

  public int getChangedFileCount() {
    return changedFiles.size();
  }

  public boolean touchedFile(final String filePath) {
    return changedFiles.contains(filePath);
  }
}
