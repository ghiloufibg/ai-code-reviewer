package com.ghiloufi.aicode.gateway.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "provider")
@JsonSubTypes({
  @JsonSubTypes.Type(value = GitHubReviewRequest.class, name = "GITHUB"),
  @JsonSubTypes.Type(value = GitLabReviewRequest.class, name = "GITLAB")
})
public sealed interface CodeReviewRequest permits GitHubReviewRequest, GitLabReviewRequest {

  SourceProvider getProvider();

  RepositoryIdentifier getRepositoryIdentifier();

  ChangeRequestIdentifier getChangeRequestIdentifier();
}
