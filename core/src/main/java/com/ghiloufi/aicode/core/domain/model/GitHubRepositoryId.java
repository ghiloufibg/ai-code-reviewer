package com.ghiloufi.aicode.core.domain.model;

import java.util.Objects;

public record GitHubRepositoryId(String owner, String repo) implements RepositoryIdentifier {

  public GitHubRepositoryId {
    Objects.requireNonNull(owner, "Owner cannot be null");
    Objects.requireNonNull(repo, "Repository cannot be null");

    if (owner.isBlank()) {
      throw new IllegalArgumentException("Owner cannot be blank");
    }
    if (repo.isBlank()) {
      throw new IllegalArgumentException("Repository cannot be blank");
    }
  }

  @Override
  public SourceProvider getProvider() {
    return SourceProvider.GITHUB;
  }

  @Override
  public String getDisplayName() {
    return owner + "/" + repo;
  }

  @Override
  public String toApiPath() {
    return "/repos/" + owner + "/" + repo;
  }

  public static GitHubRepositoryId fromString(final String fullName) {
    Objects.requireNonNull(fullName, "Full name cannot be null");

    final String[] parts = fullName.split("/");
    if (parts.length != 2) {
      throw new IllegalArgumentException(
          "Invalid GitHub repository format. Expected: owner/repo, got: " + fullName);
    }

    return new GitHubRepositoryId(parts[0], parts[1]);
  }
}
