package com.ghiloufi.aicode.gateway.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.exception.SCMAuthenticationException;
import com.ghiloufi.aicode.core.exception.SCMException;
import com.ghiloufi.aicode.core.exception.SCMRateLimitException;
import com.ghiloufi.aicode.core.exception.SCMResourceNotFoundException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebInputException;
import reactor.test.StepVerifier;

@DisplayName("GlobalExceptionHandler Tests")
final class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  final void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Nested
  @DisplayName("SCMAuthenticationException Handling")
  final class AuthenticationExceptionTests {

    @Test
    @DisplayName("should_return_unauthorized_for_authentication_exception")
    final void should_return_unauthorized_for_authentication_exception() {
      final SCMAuthenticationException exception =
          new SCMAuthenticationException(
              "Invalid API token", SourceProvider.GITLAB, "getDiff operation");

      StepVerifier.create(handler.handleAuthenticationException(exception))
          .assertNext(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                final Map<String, Object> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.get("error")).isEqualTo("authentication_failed");
                assertThat(body.get("message"))
                    .isEqualTo("Failed to authenticate with SCM provider");
                assertThat(body.get("provider")).isEqualTo("GITLAB");
                assertThat(body.get("context")).isEqualTo("getDiff operation");
                assertThat(body.get("timestamp")).isNotNull();
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_handle_github_authentication_exception")
    final void should_handle_github_authentication_exception() {
      final SCMAuthenticationException exception =
          new SCMAuthenticationException(
              "Token expired", SourceProvider.GITHUB, "getPullRequest", new RuntimeException());

      StepVerifier.create(handler.handleAuthenticationException(exception))
          .assertNext(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                final Map<String, Object> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.get("provider")).isEqualTo("GITHUB");
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("SCMResourceNotFoundException Handling")
  final class ResourceNotFoundExceptionTests {

    @Test
    @DisplayName("should_return_not_found_for_resource_not_found_exception")
    final void should_return_not_found_for_resource_not_found_exception() {
      final SCMResourceNotFoundException exception =
          new SCMResourceNotFoundException(
              "Merge request not found",
              SourceProvider.GITLAB,
              "getMergeRequest",
              "merge_request",
              "123",
              null);

      StepVerifier.create(handler.handleResourceNotFoundException(exception))
          .assertNext(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                final Map<String, Object> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.get("error")).isEqualTo("resource_not_found");
                assertThat(body.get("message").toString()).contains("Merge request not found");
                assertThat(body.get("provider")).isEqualTo("GITLAB");
                assertThat(body.get("resourceType")).isEqualTo("merge_request");
                assertThat(body.get("resourceId")).isEqualTo("123");
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_handle_repository_not_found")
    final void should_handle_repository_not_found() {
      final SCMResourceNotFoundException exception =
          new SCMResourceNotFoundException(
              "Repository does not exist",
              SourceProvider.GITHUB,
              "getRepository",
              "repository",
              "owner/repo",
              null);

      StepVerifier.create(handler.handleResourceNotFoundException(exception))
          .assertNext(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                final Map<String, Object> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.get("resourceType")).isEqualTo("repository");
                assertThat(body.get("resourceId")).isEqualTo("owner/repo");
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("SCMRateLimitException Handling")
  final class RateLimitExceptionTests {

    @Test
    @DisplayName("should_return_too_many_requests_for_rate_limit_exception")
    final void should_return_too_many_requests_for_rate_limit_exception() {
      final Instant resetTime = Instant.parse("2025-01-01T12:00:00Z");
      final SCMRateLimitException exception =
          new SCMRateLimitException(
              "Rate limit exceeded", SourceProvider.GITHUB, "listPullRequests", resetTime, 0, null);

      StepVerifier.create(handler.handleRateLimitException(exception))
          .assertNext(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                final Map<String, Object> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.get("error")).isEqualTo("rate_limit_exceeded");
                assertThat(body.get("message"))
                    .isEqualTo("API rate limit exceeded for SCM provider");
                assertThat(body.get("provider")).isEqualTo("GITHUB");
                assertThat(body.get("resetTime")).isEqualTo(resetTime.toString());
                assertThat(body.get("remainingRequests")).isEqualTo(0);
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_handle_gitlab_rate_limit")
    final void should_handle_gitlab_rate_limit() {
      final Instant resetTime = Instant.now().plusSeconds(3600);
      final SCMRateLimitException exception =
          new SCMRateLimitException(
              "GitLab API rate limit", SourceProvider.GITLAB, "getDiff", resetTime, 5, null);

      StepVerifier.create(handler.handleRateLimitException(exception))
          .assertNext(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                final Map<String, Object> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.get("remainingRequests")).isEqualTo(5);
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("SCMException Handling")
  final class SCMExceptionTests {

    @Test
    @DisplayName("should_return_bad_gateway_for_generic_scm_exception")
    final void should_return_bad_gateway_for_generic_scm_exception() {
      final SCMException exception =
          new SCMException("Connection timeout", SourceProvider.GITLAB, "getDiff");

      StepVerifier.create(handler.handleSCMException(exception))
          .assertNext(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                final Map<String, Object> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.get("error")).isEqualTo("scm_error");
                assertThat(body.get("message").toString()).contains("Connection timeout");
                assertThat(body.get("provider")).isEqualTo("GITLAB");
                assertThat(body.get("context")).isEqualTo("getDiff");
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_handle_scm_exception_with_cause")
    final void should_handle_scm_exception_with_cause() {
      final SCMException exception =
          new SCMException(
              "Network error", SourceProvider.GITHUB, "publishReview", new RuntimeException("I/O"));

      StepVerifier.create(handler.handleSCMException(exception))
          .assertNext(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                final Map<String, Object> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.get("message").toString()).contains("Network error");
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("ServerWebInputException Handling")
  final class ServerWebInputExceptionTests {

    @Test
    @DisplayName("should_return_bad_request_for_invalid_input")
    final void should_return_bad_request_for_invalid_input() {
      final ServerWebInputException exception = new ServerWebInputException("Invalid JSON body");

      StepVerifier.create(handler.handleServerWebInputException(exception))
          .assertNext(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                final Map<String, Object> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.get("error")).isEqualTo("invalid_input");
                assertThat(body.get("message").toString()).contains("Invalid JSON body");
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("IllegalArgumentException Handling")
  final class IllegalArgumentExceptionTests {

    @Test
    @DisplayName("should_return_bad_request_for_illegal_argument")
    final void should_return_bad_request_for_illegal_argument() {
      final IllegalArgumentException exception =
          new IllegalArgumentException("Invalid provider: xyz");

      StepVerifier.create(handler.handleIllegalArgumentException(exception))
          .assertNext(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                final Map<String, Object> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.get("error")).isEqualTo("illegal_argument");
                assertThat(body.get("message")).isEqualTo("Invalid provider: xyz");
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("Generic Exception Handling")
  final class GenericExceptionTests {

    @Test
    @DisplayName("should_return_internal_server_error_for_unexpected_exception")
    final void should_return_internal_server_error_for_unexpected_exception() {
      final Exception exception = new RuntimeException("Unexpected error occurred");

      StepVerifier.create(handler.handleGenericException(exception))
          .assertNext(
              response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                final Map<String, Object> body = response.getBody();
                assertThat(body).isNotNull();
                assertThat(body.get("error")).isEqualTo("internal_server_error");
                assertThat(body.get("message")).isEqualTo("An unexpected error occurred");
                assertThat(body.get("timestamp")).isNotNull();
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("should_not_expose_internal_details_for_generic_exception")
    final void should_not_expose_internal_details_for_generic_exception() {
      final Exception exception = new NullPointerException("Sensitive internal error");

      StepVerifier.create(handler.handleGenericException(exception))
          .assertNext(
              response -> {
                final Map<String, Object> body = response.getBody();
                assertThat(body).isNotNull();
                final String message = (String) body.get("message");
                assertThat(message).doesNotContain("Sensitive");
                assertThat(message).doesNotContain("NullPointerException");
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("Response Structure")
  final class ResponseStructureTests {

    @Test
    @DisplayName("should_include_timestamp_in_all_responses")
    final void should_include_timestamp_in_all_responses() {
      final Instant before = Instant.now();
      final Exception exception = new RuntimeException("test");

      final ResponseEntity<Map<String, Object>> response =
          handler.handleGenericException(exception).block();

      assertThat(response).isNotNull();
      assertThat(response.getBody()).isNotNull();
      final String timestamp = (String) response.getBody().get("timestamp");
      assertThat(timestamp).isNotNull();
      final Instant responseTime = Instant.parse(timestamp);
      assertThat(responseTime).isAfterOrEqualTo(before);
      assertThat(responseTime).isBeforeOrEqualTo(Instant.now());
    }
  }
}
