package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ghiloufi.aicode.core.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SCMIdentifierValidator")
final class SCMIdentifierValidatorTest {

  private SCMIdentifierValidator validator;

  @BeforeEach
  final void setUp() {
    validator = new SCMIdentifierValidator();
  }

  @Nested
  @DisplayName("when validating GitLab repository identifier")
  final class GitLabRepositoryValidation {

    @Test
    @DisplayName("should_return_gitlab_repository_id_when_valid")
    final void should_return_gitlab_repository_id_when_valid() {
      final RepositoryIdentifier repo = new GitLabRepositoryId("my-org/my-project");

      final GitLabRepositoryId result = validator.validateGitLabRepository(repo);

      assertThat(result).isNotNull();
      assertThat(result.projectId()).isEqualTo("my-org/my-project");
    }

    @Test
    @DisplayName("should_throw_exception_when_not_gitlab_repository")
    final void should_throw_exception_when_not_gitlab_repository() {
      final RepositoryIdentifier repo = new GitHubRepositoryId("owner", "repo");

      assertThatThrownBy(() -> validator.validateGitLabRepository(repo))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected GitLab repository ID")
          .hasMessageContaining("GitHubRepositoryId");
    }
  }

  @Nested
  @DisplayName("when validating GitLab merge request identifier")
  final class GitLabMergeRequestValidation {

    @Test
    @DisplayName("should_return_merge_request_id_when_valid")
    final void should_return_merge_request_id_when_valid() {
      final ChangeRequestIdentifier changeRequest = new MergeRequestId(123);

      final MergeRequestId result = validator.validateGitLabChangeRequest(changeRequest);

      assertThat(result).isNotNull();
      assertThat(result.iid()).isEqualTo(123);
    }

    @Test
    @DisplayName("should_throw_exception_when_not_merge_request")
    final void should_throw_exception_when_not_merge_request() {
      final ChangeRequestIdentifier changeRequest = new PullRequestId(456);

      assertThatThrownBy(() -> validator.validateGitLabChangeRequest(changeRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected Merge Request ID")
          .hasMessageContaining("PullRequestId");
    }
  }

  @Nested
  @DisplayName("when validating GitHub repository identifier")
  final class GitHubRepositoryValidation {

    @Test
    @DisplayName("should_return_github_repository_id_when_valid")
    final void should_return_github_repository_id_when_valid() {
      final RepositoryIdentifier repo = new GitHubRepositoryId("owner", "repo");

      final GitHubRepositoryId result = validator.validateGitHubRepository(repo);

      assertThat(result).isNotNull();
      assertThat(result.owner()).isEqualTo("owner");
      assertThat(result.repo()).isEqualTo("repo");
    }

    @Test
    @DisplayName("should_throw_exception_when_not_github_repository")
    final void should_throw_exception_when_not_github_repository() {
      final RepositoryIdentifier repo = new GitLabRepositoryId("my-org/my-project");

      assertThatThrownBy(() -> validator.validateGitHubRepository(repo))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected GitHub repository ID")
          .hasMessageContaining("GitLabRepositoryId");
    }
  }

  @Nested
  @DisplayName("when validating GitHub pull request identifier")
  final class GitHubPullRequestValidation {

    @Test
    @DisplayName("should_return_pull_request_id_when_valid")
    final void should_return_pull_request_id_when_valid() {
      final ChangeRequestIdentifier changeRequest = new PullRequestId(789);

      final PullRequestId result = validator.validateGitHubChangeRequest(changeRequest);

      assertThat(result).isNotNull();
      assertThat(result.number()).isEqualTo(789);
    }

    @Test
    @DisplayName("should_throw_exception_when_not_pull_request")
    final void should_throw_exception_when_not_pull_request() {
      final ChangeRequestIdentifier changeRequest = new MergeRequestId(321);

      assertThatThrownBy(() -> validator.validateGitHubChangeRequest(changeRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected Pull Request ID")
          .hasMessageContaining("MergeRequestId");
    }
  }
}
