package com.ghiloufi.aicode.core.domain.model;

import java.util.Optional;

public sealed interface RepositoryIdentifier permits GitHubRepositoryId, GitLabRepositoryId {

  SourceProvider getProvider();

  String getDisplayName();

  String toApiPath();

  static RepositoryIdentifier create(final SourceProvider provider, final String identifier) {
    final SourceProvider validProvider =
        Optional.ofNullable(provider)
            .orElseThrow(() -> new IllegalArgumentException("Provider cannot be null"));

    final String validIdentifier =
        Optional.ofNullable(identifier)
            .filter(id -> !id.isBlank())
            .orElseThrow(() -> new IllegalArgumentException("Identifier cannot be null or blank"));

    return switch (validProvider) {
      case GITHUB -> GitHubRepositoryId.fromString(validIdentifier);
      case GITLAB -> new GitLabRepositoryId(validIdentifier);
    };
  }
}
