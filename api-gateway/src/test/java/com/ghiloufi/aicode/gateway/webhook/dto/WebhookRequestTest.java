package com.ghiloufi.aicode.gateway.webhook.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class WebhookRequestTest {

  private Validator validator;

  @BeforeEach
  final void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Nested
  @DisplayName("Valid Requests")
  final class ValidRequestTests {

    @Test
    @DisplayName("should_accept_valid_github_request")
    final void should_accept_valid_github_request() {
      final WebhookRequest request =
          new WebhookRequest("github", "owner/repo", 123, "github-actions");

      final Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should_accept_valid_gitlab_request")
    final void should_accept_valid_gitlab_request() {
      final WebhookRequest request =
          new WebhookRequest("gitlab", "group/project", 456, "gitlab-ci");

      final Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

      assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("should_accept_request_without_trigger_source")
    final void should_accept_request_without_trigger_source() {
      final WebhookRequest request = new WebhookRequest("github", "owner/repo", 1, null);

      final Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

      assertThat(violations).isEmpty();
    }
  }

  @Nested
  @DisplayName("Provider Validation")
  final class ProviderValidationTests {

    @Test
    @DisplayName("should_reject_null_provider")
    final void should_reject_null_provider() {
      final WebhookRequest request = new WebhookRequest(null, "owner/repo", 1, null);

      final Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

      assertThat(violations).hasSize(1);
      assertThat(violations.iterator().next().getMessage()).isEqualTo("Provider is required");
    }

    @Test
    @DisplayName("should_reject_blank_provider")
    final void should_reject_blank_provider() {
      final WebhookRequest request = new WebhookRequest("", "owner/repo", 1, null);

      final Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

      assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("should_reject_invalid_provider")
    final void should_reject_invalid_provider() {
      final WebhookRequest request = new WebhookRequest("bitbucket", "owner/repo", 1, null);

      final Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

      assertThat(violations).hasSize(1);
      assertThat(violations.iterator().next().getMessage())
          .isEqualTo("Provider must be 'github' or 'gitlab'");
    }
  }

  @Nested
  @DisplayName("Repository ID Validation")
  final class RepositoryIdValidationTests {

    @Test
    @DisplayName("should_reject_null_repository_id")
    final void should_reject_null_repository_id() {
      final WebhookRequest request = new WebhookRequest("github", null, 1, null);

      final Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

      assertThat(violations).hasSize(1);
      assertThat(violations.iterator().next().getMessage()).isEqualTo("Repository ID is required");
    }

    @Test
    @DisplayName("should_reject_blank_repository_id")
    final void should_reject_blank_repository_id() {
      final WebhookRequest request = new WebhookRequest("github", "   ", 1, null);

      final Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

      assertThat(violations).hasSize(1);
      assertThat(violations.iterator().next().getMessage()).isEqualTo("Repository ID is required");
    }
  }

  @Nested
  @DisplayName("Change Request ID Validation")
  final class ChangeRequestIdValidationTests {

    @Test
    @DisplayName("should_reject_null_change_request_id")
    final void should_reject_null_change_request_id() {
      final WebhookRequest request = new WebhookRequest("github", "owner/repo", null, null);

      final Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

      assertThat(violations).hasSize(1);
      assertThat(violations.iterator().next().getMessage())
          .isEqualTo("Change request ID is required");
    }

    @Test
    @DisplayName("should_reject_zero_change_request_id")
    final void should_reject_zero_change_request_id() {
      final WebhookRequest request = new WebhookRequest("github", "owner/repo", 0, null);

      final Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

      assertThat(violations).hasSize(1);
      assertThat(violations.iterator().next().getMessage())
          .isEqualTo("Change request ID must be positive");
    }

    @Test
    @DisplayName("should_reject_negative_change_request_id")
    final void should_reject_negative_change_request_id() {
      final WebhookRequest request = new WebhookRequest("github", "owner/repo", -1, null);

      final Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

      assertThat(violations).hasSize(1);
      assertThat(violations.iterator().next().getMessage())
          .isEqualTo("Change request ID must be positive");
    }
  }
}
