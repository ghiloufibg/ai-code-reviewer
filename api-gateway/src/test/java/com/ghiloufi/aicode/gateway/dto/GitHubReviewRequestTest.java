package com.ghiloufi.aicode.gateway.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.GitHubRepositoryId;
import com.ghiloufi.aicode.core.domain.model.PullRequestId;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GitHubReviewRequest Tests")
final class GitHubReviewRequestTest {

  private Validator validator;

  @BeforeEach
  final void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Nested
  @DisplayName("Valid Requests")
  final class ValidRequestTests {

    @Test
    @DisplayName("should_create_valid_request_with_all_fields")
    final void should_create_valid_request_with_all_fields() {
      final GitHubReviewRequest request = new GitHubReviewRequest("owner", "repo", 123);

      assertThat(request.owner()).isEqualTo("owner");
      assertThat(request.repo()).isEqualTo("repo");
      assertThat(request.pullRequestNumber()).isEqualTo(123);
    }

    @Test
    @DisplayName("should_pass_validation_for_valid_request")
    final void should_pass_validation_for_valid_request() {
      final GitHubReviewRequest request = new GitHubReviewRequest("anthropic", "claude", 42);

      final Set<ConstraintViolation<GitHubReviewRequest>> violations = validator.validate(request);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should_accept_owner_with_hyphen")
    final void should_accept_owner_with_hyphen() {
      final GitHubReviewRequest request = new GitHubReviewRequest("my-org", "my-repo", 1);

      assertThat(request.owner()).isEqualTo("my-org");
    }

    @Test
    @DisplayName("should_accept_large_pull_request_number")
    final void should_accept_large_pull_request_number() {
      final GitHubReviewRequest request =
          new GitHubReviewRequest("owner", "repo", Integer.MAX_VALUE);

      assertThat(request.pullRequestNumber()).isEqualTo(Integer.MAX_VALUE);
    }
  }

  @Nested
  @DisplayName("Constructor Validation")
  final class ConstructorValidationTests {

    @Test
    @DisplayName("should_reject_null_owner")
    final void should_reject_null_owner() {
      assertThatThrownBy(() -> new GitHubReviewRequest(null, "repo", 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Owner cannot be null or blank");
    }

    @Test
    @DisplayName("should_reject_blank_owner")
    final void should_reject_blank_owner() {
      assertThatThrownBy(() -> new GitHubReviewRequest("   ", "repo", 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Owner cannot be null or blank");
    }

    @Test
    @DisplayName("should_reject_empty_owner")
    final void should_reject_empty_owner() {
      assertThatThrownBy(() -> new GitHubReviewRequest("", "repo", 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Owner cannot be null or blank");
    }

    @Test
    @DisplayName("should_reject_null_repo")
    final void should_reject_null_repo() {
      assertThatThrownBy(() -> new GitHubReviewRequest("owner", null, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Repository name cannot be null or blank");
    }

    @Test
    @DisplayName("should_reject_blank_repo")
    final void should_reject_blank_repo() {
      assertThatThrownBy(() -> new GitHubReviewRequest("owner", "  ", 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Repository name cannot be null or blank");
    }
  }

  @Nested
  @DisplayName("Bean Validation")
  final class BeanValidationTests {

    @Test
    @DisplayName("should_reject_zero_pull_request_number")
    final void should_reject_zero_pull_request_number() {
      final GitHubReviewRequest request = new GitHubReviewRequest("owner", "repo", 0);

      final Set<ConstraintViolation<GitHubReviewRequest>> violations = validator.validate(request);

      assertThat(violations).hasSize(1);
      assertThat(violations.iterator().next().getPropertyPath().toString())
          .isEqualTo("pullRequestNumber");
    }

    @Test
    @DisplayName("should_reject_negative_pull_request_number")
    final void should_reject_negative_pull_request_number() {
      final GitHubReviewRequest request = new GitHubReviewRequest("owner", "repo", -5);

      final Set<ConstraintViolation<GitHubReviewRequest>> violations = validator.validate(request);

      assertThat(violations).hasSize(1);
    }
  }

  @Nested
  @DisplayName("CodeReviewRequest Interface Implementation")
  final class InterfaceImplementationTests {

    @Test
    @DisplayName("should_return_github_provider")
    final void should_return_github_provider() {
      final GitHubReviewRequest request = new GitHubReviewRequest("owner", "repo", 1);

      assertThat(request.getProvider()).isEqualTo(SourceProvider.GITHUB);
    }

    @Test
    @DisplayName("should_return_github_repository_identifier")
    final void should_return_github_repository_identifier() {
      final GitHubReviewRequest request = new GitHubReviewRequest("anthropic", "claude", 42);

      assertThat(request.getRepositoryIdentifier()).isInstanceOf(GitHubRepositoryId.class);
      final GitHubRepositoryId repoId = (GitHubRepositoryId) request.getRepositoryIdentifier();
      assertThat(repoId.owner()).isEqualTo("anthropic");
      assertThat(repoId.repo()).isEqualTo("claude");
    }

    @Test
    @DisplayName("should_return_pull_request_identifier")
    final void should_return_pull_request_identifier() {
      final GitHubReviewRequest request = new GitHubReviewRequest("owner", "repo", 999);

      assertThat(request.getChangeRequestIdentifier()).isInstanceOf(PullRequestId.class);
      final PullRequestId prId = (PullRequestId) request.getChangeRequestIdentifier();
      assertThat(prId.number()).isEqualTo(999);
    }
  }

  @Nested
  @DisplayName("JSON Deserialization")
  final class JsonDeserializationTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should_deserialize_from_json")
    final void should_deserialize_from_json() throws Exception {
      final String json =
          """
          {
            "provider": "GITHUB",
            "owner": "github-org",
            "repo": "awesome-project",
            "pullRequestNumber": 456
          }
          """;

      final GitHubReviewRequest request = objectMapper.readValue(json, GitHubReviewRequest.class);

      assertThat(request.owner()).isEqualTo("github-org");
      assertThat(request.repo()).isEqualTo("awesome-project");
      assertThat(request.pullRequestNumber()).isEqualTo(456);
    }

    @Test
    @DisplayName("should_serialize_to_json")
    final void should_serialize_to_json() throws Exception {
      final GitHubReviewRequest request = new GitHubReviewRequest("my-owner", "my-repo", 789);

      final String json = objectMapper.writeValueAsString(request);

      assertThat(json).contains("\"owner\":\"my-owner\"");
      assertThat(json).contains("\"repo\":\"my-repo\"");
      assertThat(json).contains("\"pullRequestNumber\":789");
    }
  }

  @Nested
  @DisplayName("Record Equality")
  final class RecordEqualityTests {

    @Test
    @DisplayName("should_be_equal_for_same_values")
    final void should_be_equal_for_same_values() {
      final GitHubReviewRequest request1 = new GitHubReviewRequest("owner", "repo", 1);
      final GitHubReviewRequest request2 = new GitHubReviewRequest("owner", "repo", 1);

      assertThat(request1).isEqualTo(request2);
      assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("should_not_be_equal_for_different_pr_numbers")
    final void should_not_be_equal_for_different_pr_numbers() {
      final GitHubReviewRequest request1 = new GitHubReviewRequest("owner", "repo", 1);
      final GitHubReviewRequest request2 = new GitHubReviewRequest("owner", "repo", 2);

      assertThat(request1).isNotEqualTo(request2);
    }
  }
}
