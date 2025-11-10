package com.ghiloufi.aicode.core.domain.model;

import java.util.Optional;

public sealed interface ChangeRequestIdentifier permits PullRequestId, MergeRequestId {

  SourceProvider getProvider();

  int getNumber();

  String getDisplayName();

  static ChangeRequestIdentifier create(final SourceProvider provider, final int id) {
    final SourceProvider validProvider =
        Optional.ofNullable(provider)
            .orElseThrow(() -> new IllegalArgumentException("Provider cannot be null"));

    if (id <= 0) {
      throw new IllegalArgumentException("ID must be positive");
    }

    return switch (validProvider) {
      case GITHUB -> new PullRequestId(id);
      case GITLAB -> new MergeRequestId(id);
    };
  }
}
