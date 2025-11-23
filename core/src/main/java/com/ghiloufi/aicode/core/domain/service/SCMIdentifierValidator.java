package com.ghiloufi.aicode.core.domain.service;

import com.ghiloufi.aicode.core.domain.model.*;
import org.springframework.stereotype.Service;

@Service
public final class SCMIdentifierValidator {

  public GitLabRepositoryId validateGitLabRepository(final RepositoryIdentifier repo) {
    if (!(repo instanceof GitLabRepositoryId gitLabRepo)) {
      throw new IllegalArgumentException(
          "Expected GitLab repository ID, got: " + repo.getClass().getSimpleName());
    }
    return gitLabRepo;
  }

  public MergeRequestId validateGitLabChangeRequest(final ChangeRequestIdentifier changeRequest) {
    if (!(changeRequest instanceof MergeRequestId mrId)) {
      throw new IllegalArgumentException(
          "Expected Merge Request ID, got: " + changeRequest.getClass().getSimpleName());
    }
    return mrId;
  }

  public GitHubRepositoryId validateGitHubRepository(final RepositoryIdentifier repo) {
    if (!(repo instanceof GitHubRepositoryId gitHubRepo)) {
      throw new IllegalArgumentException(
          "Expected GitHub repository ID, got: " + repo.getClass().getSimpleName());
    }
    return gitHubRepo;
  }

  public PullRequestId validateGitHubChangeRequest(final ChangeRequestIdentifier changeRequest) {
    if (!(changeRequest instanceof PullRequestId prId)) {
      throw new IllegalArgumentException(
          "Expected Pull Request ID, got: " + changeRequest.getClass().getSimpleName());
    }
    return prId;
  }
}
