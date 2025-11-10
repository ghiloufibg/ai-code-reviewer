package com.ghiloufi.aicode.gateway.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.GitLabRepositoryId;
import com.ghiloufi.aicode.core.domain.model.MergeRequestId;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.Optional;

public record GitLabReviewRequest(
    @NotBlank @JsonProperty("projectId") String projectId,
    @Positive @JsonProperty("mergeRequestIid") int mergeRequestIid)
    implements CodeReviewRequest {

  @JsonCreator
  public GitLabReviewRequest {
    Optional.ofNullable(projectId)
        .filter(id -> !id.isBlank())
        .orElseThrow(() -> new IllegalArgumentException("Project ID cannot be null or blank"));

    if (mergeRequestIid <= 0) {
      throw new IllegalArgumentException("Merge request IID must be positive");
    }
  }

  @Override
  public SourceProvider getProvider() {
    return SourceProvider.GITLAB;
  }

  @Override
  public RepositoryIdentifier getRepositoryIdentifier() {
    return new GitLabRepositoryId(projectId);
  }

  @Override
  public ChangeRequestIdentifier getChangeRequestIdentifier() {
    return new MergeRequestId(mergeRequestIid);
  }
}
