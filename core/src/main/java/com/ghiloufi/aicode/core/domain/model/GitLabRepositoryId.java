package com.ghiloufi.aicode.core.domain.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record GitLabRepositoryId(String projectId) implements RepositoryIdentifier {

  public GitLabRepositoryId {
    Objects.requireNonNull(projectId, "Project ID cannot be null");

    if (projectId.isBlank()) {
      throw new IllegalArgumentException("Project ID cannot be blank");
    }
  }

  @Override
  public SourceProvider getProvider() {
    return SourceProvider.GITLAB;
  }

  @Override
  public String getDisplayName() {
    return "Project " + projectId;
  }

  @Override
  public String toApiPath() {
    return "/projects/" + URLEncoder.encode(projectId, StandardCharsets.UTF_8);
  }

  public Long toNumericId() {
    try {
      return Long.parseLong(projectId);
    } catch (final NumberFormatException e) {
      return null;
    }
  }

  public boolean isNumericId() {
    return toNumericId() != null;
  }
}
