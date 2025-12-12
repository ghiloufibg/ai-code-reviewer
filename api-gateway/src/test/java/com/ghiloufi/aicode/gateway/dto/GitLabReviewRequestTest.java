package com.ghiloufi.aicode.gateway.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.GitLabRepositoryId;
import com.ghiloufi.aicode.core.domain.model.MergeRequestId;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GitLabReviewRequest Tests")
final class GitLabReviewRequestTest {

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
      final GitLabReviewRequest request = new GitLabReviewRequest("group/project", 123);

      assertThat(request.projectId()).isEqualTo("group/project");
      assertThat(request.mergeRequestIid()).isEqualTo(123);
    }

    @Test
    @DisplayName("should_pass_validation_for_valid_request")
    final void should_pass_validation_for_valid_request() {
      final GitLabReviewRequest request = new GitLabReviewRequest("my-group/my-project", 42);

      final Set<ConstraintViolation<GitLabReviewRequest>> violations = validator.validate(request);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should_accept_numeric_project_id")
    final void should_accept_numeric_project_id() {
      final GitLabReviewRequest request = new GitLabReviewRequest("12345", 1);

      assertThat(request.projectId()).isEqualTo("12345");
    }

    @Test
    @DisplayName("should_accept_nested_group_project_id")
    final void should_accept_nested_group_project_id() {
      final GitLabReviewRequest request = new GitLabReviewRequest("org/team/subteam/project", 1);

      assertThat(request.projectId()).isEqualTo("org/team/subteam/project");
    }

    @Test
    @DisplayName("should_accept_large_merge_request_iid")
    final void should_accept_large_merge_request_iid() {
      final GitLabReviewRequest request = new GitLabReviewRequest("project", Integer.MAX_VALUE);

      assertThat(request.mergeRequestIid()).isEqualTo(Integer.MAX_VALUE);
    }
  }

  @Nested
  @DisplayName("Constructor Validation")
  final class ConstructorValidationTests {

    @Test
    @DisplayName("should_reject_null_project_id")
    final void should_reject_null_project_id() {
      assertThatThrownBy(() -> new GitLabReviewRequest(null, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Project ID cannot be null or blank");
    }

    @Test
    @DisplayName("should_reject_blank_project_id")
    final void should_reject_blank_project_id() {
      assertThatThrownBy(() -> new GitLabReviewRequest("   ", 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Project ID cannot be null or blank");
    }

    @Test
    @DisplayName("should_reject_empty_project_id")
    final void should_reject_empty_project_id() {
      assertThatThrownBy(() -> new GitLabReviewRequest("", 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Project ID cannot be null or blank");
    }
  }

  @Nested
  @DisplayName("Bean Validation")
  final class BeanValidationTests {

    @Test
    @DisplayName("should_reject_zero_merge_request_iid")
    final void should_reject_zero_merge_request_iid() {
      final GitLabReviewRequest request = new GitLabReviewRequest("project", 0);

      final Set<ConstraintViolation<GitLabReviewRequest>> violations = validator.validate(request);

      assertThat(violations).hasSize(1);
      assertThat(violations.iterator().next().getPropertyPath().toString())
          .isEqualTo("mergeRequestIid");
    }

    @Test
    @DisplayName("should_reject_negative_merge_request_iid")
    final void should_reject_negative_merge_request_iid() {
      final GitLabReviewRequest request = new GitLabReviewRequest("project", -10);

      final Set<ConstraintViolation<GitLabReviewRequest>> violations = validator.validate(request);

      assertThat(violations).hasSize(1);
    }
  }

  @Nested
  @DisplayName("CodeReviewRequest Interface Implementation")
  final class InterfaceImplementationTests {

    @Test
    @DisplayName("should_return_gitlab_provider")
    final void should_return_gitlab_provider() {
      final GitLabReviewRequest request = new GitLabReviewRequest("project", 1);

      assertThat(request.getProvider()).isEqualTo(SourceProvider.GITLAB);
    }

    @Test
    @DisplayName("should_return_gitlab_repository_identifier")
    final void should_return_gitlab_repository_identifier() {
      final GitLabReviewRequest request = new GitLabReviewRequest("group/project", 42);

      assertThat(request.getRepositoryIdentifier()).isInstanceOf(GitLabRepositoryId.class);
      final GitLabRepositoryId repoId = (GitLabRepositoryId) request.getRepositoryIdentifier();
      assertThat(repoId.projectId()).isEqualTo("group/project");
    }

    @Test
    @DisplayName("should_return_merge_request_identifier")
    final void should_return_merge_request_identifier() {
      final GitLabReviewRequest request = new GitLabReviewRequest("project", 999);

      assertThat(request.getChangeRequestIdentifier()).isInstanceOf(MergeRequestId.class);
      final MergeRequestId mrId = (MergeRequestId) request.getChangeRequestIdentifier();
      assertThat(mrId.iid()).isEqualTo(999);
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
            "provider": "GITLAB",
            "projectId": "gitlab-org/gitlab",
            "mergeRequestIid": 456
          }
          """;

      final GitLabReviewRequest request = objectMapper.readValue(json, GitLabReviewRequest.class);

      assertThat(request.projectId()).isEqualTo("gitlab-org/gitlab");
      assertThat(request.mergeRequestIid()).isEqualTo(456);
    }

    @Test
    @DisplayName("should_serialize_to_json")
    final void should_serialize_to_json() throws Exception {
      final GitLabReviewRequest request = new GitLabReviewRequest("my-project", 789);

      final String json = objectMapper.writeValueAsString(request);

      assertThat(json).contains("\"projectId\":\"my-project\"");
      assertThat(json).contains("\"mergeRequestIid\":789");
    }
  }

  @Nested
  @DisplayName("Record Equality")
  final class RecordEqualityTests {

    @Test
    @DisplayName("should_be_equal_for_same_values")
    final void should_be_equal_for_same_values() {
      final GitLabReviewRequest request1 = new GitLabReviewRequest("project", 1);
      final GitLabReviewRequest request2 = new GitLabReviewRequest("project", 1);

      assertThat(request1).isEqualTo(request2);
      assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("should_not_be_equal_for_different_project_ids")
    final void should_not_be_equal_for_different_project_ids() {
      final GitLabReviewRequest request1 = new GitLabReviewRequest("project-a", 1);
      final GitLabReviewRequest request2 = new GitLabReviewRequest("project-b", 1);

      assertThat(request1).isNotEqualTo(request2);
    }
  }
}
