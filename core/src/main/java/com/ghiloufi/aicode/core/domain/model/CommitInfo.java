package com.ghiloufi.aicode.core.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CommitInfo(
    String commitId, String message, String author, Instant timestamp, List<String> changedFiles) {

  public CommitInfo {
    Objects.requireNonNull(commitId, "Commit ID cannot be null");
    changedFiles = changedFiles != null ? List.copyOf(changedFiles) : List.of();

    if (commitId.isBlank()) {
      throw new IllegalArgumentException("Commit ID cannot be blank");
    }
  }

  public String shortId() {
    return commitId.length() > 7 ? commitId.substring(0, 7) : commitId;
  }

  public String firstLineOfMessage() {
    if (message == null || message.isBlank()) {
      return "";
    }
    final int newlineIndex = message.indexOf('\n');
    return newlineIndex > 0 ? message.substring(0, newlineIndex) : message;
  }

  public boolean hasMessage() {
    return message != null && !message.isBlank();
  }

  public boolean hasAuthor() {
    return author != null && !author.isBlank();
  }

  public int getChangedFileCount() {
    return changedFiles.size();
  }

  public boolean touchedFile(final String filePath) {
    return changedFiles.contains(filePath);
  }
}
