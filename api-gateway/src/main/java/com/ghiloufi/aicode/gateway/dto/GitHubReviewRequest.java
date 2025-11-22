package com.ghiloufi.aicode.gateway.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.GitHubRepositoryId;
import com.ghiloufi.aicode.core.domain.model.PullRequestId;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.Optional;

public record GitHubReviewRequest(
    @NotBlank @JsonProperty("owner") String owner,
    @NotBlank @JsonProperty("repo") String repo,
    @Positive @JsonProperty("pullRequestNumber") int pullRequestNumber)
    implements CodeReviewRequest {

  @JsonCreator
  public GitHubReviewRequest {
    Optional.ofNullable(owner)
        .filter(o -> !o.isBlank())
        .orElseThrow(() -> new IllegalArgumentException("Owner cannot be null or blank"));

    Optional.ofNullable(repo)
        .filter(r -> !r.isBlank())
        .orElseThrow(() -> new IllegalArgumentException("Repository name cannot be null or blank"));
  }

  @Override
  public SourceProvider getProvider() {
    return SourceProvider.GITHUB;
  }

  @Override
  public RepositoryIdentifier getRepositoryIdentifier() {
    return new GitHubRepositoryId(owner, repo);
  }

  @Override
  public ChangeRequestIdentifier getChangeRequestIdentifier() {
    return new PullRequestId(pullRequestNumber);
  }
}
