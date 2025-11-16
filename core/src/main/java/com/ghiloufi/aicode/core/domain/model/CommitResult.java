package com.ghiloufi.aicode.core.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CommitResult(
    String commitSha,
    String commitUrl,
    String branchName,
    List<String> filesModified,
    Instant createdAt) {

  public CommitResult {
    Objects.requireNonNull(commitSha, "Commit SHA cannot be null");
    Objects.requireNonNull(commitUrl, "Commit URL cannot be null");
    Objects.requireNonNull(branchName, "Branch name cannot be null");
    Objects.requireNonNull(filesModified, "Files modified list cannot be null");
    Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
  }
}
