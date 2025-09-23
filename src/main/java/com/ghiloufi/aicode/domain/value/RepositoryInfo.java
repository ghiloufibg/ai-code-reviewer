package com.ghiloufi.aicode.domain.value;

import com.ghiloufi.aicode.shared.exception.DomainException;

/**
 * Value object representing repository information.
 *
 * <p>Contains the repository identifier and the specific commits or PR being reviewed.
 */
public record RepositoryInfo(
    String repository,
    String mode,
    Integer pullRequestNumber,
    String fromCommit,
    String toCommit,
    String baseBranch) {

  public RepositoryInfo {
    if (repository == null || repository.trim().isEmpty()) {
      throw new DomainException("Repository cannot be null or empty");
    }
    if (mode == null || mode.trim().isEmpty()) {
      throw new DomainException("Mode cannot be null or empty");
    }
    if (!isValidMode(mode)) {
      throw new DomainException("Invalid mode: " + mode + ". Must be 'github' or 'local'");
    }
    if ("github".equals(mode) && pullRequestNumber == null) {
      throw new DomainException("Pull request number is required for github mode");
    }
    if ("local".equals(mode) && (fromCommit == null || toCommit == null)) {
      throw new DomainException("From and to commits are required for local mode");
    }

    repository = repository.trim();
    mode = mode.trim();
  }

  private static boolean isValidMode(String mode) {
    return "github".equals(mode) || "local".equals(mode);
  }

  /** Checks if this is a GitHub PR review. */
  public boolean isGitHubMode() {
    return "github".equals(mode);
  }

  /** Checks if this is a local commit review. */
  public boolean isLocalMode() {
    return "local".equals(mode);
  }

  /** Gets a display name for this repository info. */
  public String getDisplayName() {
    if (isGitHubMode()) {
      return repository + " PR #" + pullRequestNumber;
    } else {
      return repository + " (" + fromCommit + ".." + toCommit + ")";
    }
  }

  /** Creates a RepositoryInfo for GitHub PR review. */
  public static RepositoryInfo forGitHubPR(String repository, int pullRequestNumber) {
    return new RepositoryInfo(repository, "github", pullRequestNumber, null, null, null);
  }

  /** Creates a RepositoryInfo for local commit review. */
  public static RepositoryInfo forLocalCommits(
      String repository, String fromCommit, String toCommit, String baseBranch) {
    return new RepositoryInfo(repository, "local", null, fromCommit, toCommit, baseBranch);
  }
}
