package com.ghiloufi.aicode.gateway.webhook.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.gateway.webhook.dto.WebhookRequest;
import com.ghiloufi.aicode.gateway.webhook.dto.WebhookResponse;
import com.ghiloufi.aicode.gateway.webhook.exception.AlreadyProcessedException;
import com.ghiloufi.aicode.gateway.webhook.service.WebhookService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

final class WebhookControllerTest {

  private WebTestClient webTestClient;
  private TestWebhookService webhookService;

  @BeforeEach
  final void setUp() {
    webhookService = new TestWebhookService();
    final WebhookController controller = new WebhookController(webhookService);

    webTestClient =
        WebTestClient.bindToController(controller)
            .configureClient()
            .responseTimeout(Duration.ofSeconds(5))
            .build();
  }

  @Nested
  @DisplayName("Successful Webhook Processing")
  final class SuccessfulProcessingTests {

    @Test
    @DisplayName("should_return_202_accepted_for_new_webhook")
    final void should_return_202_accepted_for_new_webhook() {
      webhookService.setResponse(WebhookResponse.accepted("test-request-id"));

      webTestClient
          .post()
          .uri("/webhooks")
          .contentType(MediaType.APPLICATION_JSON)
          .header("X-Idempotency-Key", "commit-sha-123")
          .bodyValue(
              """
              {
                "provider": "github",
                "repositoryId": "owner/repo",
                "changeRequestId": 123,
                "triggerSource": "github-actions"
              }
              """)
          .exchange()
          .expectStatus()
          .isAccepted()
          .expectBody()
          .jsonPath("$.requestId")
          .isEqualTo("test-request-id")
          .jsonPath("$.status")
          .isEqualTo("accepted")
          .jsonPath("$.message")
          .isEqualTo("Review request queued for processing");

      assertThat(webhookService.getCapturedIdempotencyKey()).isEqualTo("commit-sha-123");
    }

    @Test
    @DisplayName("should_accept_gitlab_provider")
    final void should_accept_gitlab_provider() {
      webhookService.setResponse(WebhookResponse.accepted("gitlab-request-id"));

      webTestClient
          .post()
          .uri("/webhooks")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
              """
              {
                "provider": "gitlab",
                "repositoryId": "group/project",
                "changeRequestId": 456,
                "triggerSource": "gitlab-ci"
              }
              """)
          .exchange()
          .expectStatus()
          .isAccepted()
          .expectBody()
          .jsonPath("$.status")
          .isEqualTo("accepted");

      assertThat(webhookService.getCapturedRequest().provider()).isEqualTo("gitlab");
      assertThat(webhookService.getCapturedRequest().repositoryId()).isEqualTo("group/project");
      assertThat(webhookService.getCapturedRequest().changeRequestId()).isEqualTo(456);
    }

    @Test
    @DisplayName("should_accept_request_without_trigger_source")
    final void should_accept_request_without_trigger_source() {
      webhookService.setResponse(WebhookResponse.accepted("no-source-id"));

      webTestClient
          .post()
          .uri("/webhooks")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
              """
              {
                "provider": "github",
                "repositoryId": "owner/repo",
                "changeRequestId": 1
              }
              """)
          .exchange()
          .expectStatus()
          .isAccepted();

      assertThat(webhookService.getCapturedRequest().triggerSource()).isNull();
    }
  }

  @Nested
  @DisplayName("Idempotent Replay")
  final class IdempotentReplayTests {

    @Test
    @DisplayName("should_return_200_ok_for_already_processed_event")
    final void should_return_200_ok_for_already_processed_event() {
      webhookService.setAlreadyProcessed(true, "duplicate-key");

      webTestClient
          .post()
          .uri("/webhooks")
          .contentType(MediaType.APPLICATION_JSON)
          .header("X-Idempotency-Key", "duplicate-key")
          .bodyValue(
              """
              {
                "provider": "github",
                "repositoryId": "owner/repo",
                "changeRequestId": 123
              }
              """)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.status")
          .isEqualTo("already_processed")
          .jsonPath("$.message")
          .isEqualTo("Event was already processed");
    }
  }

  @Nested
  @DisplayName("Validation Errors")
  final class ValidationErrorTests {

    @Test
    @DisplayName("should_return_400_for_missing_provider")
    final void should_return_400_for_missing_provider() {
      webTestClient
          .post()
          .uri("/webhooks")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
              """
              {
                "repositoryId": "owner/repo",
                "changeRequestId": 123
              }
              """)
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.error")
          .isEqualTo("validation_error");
    }

    @Test
    @DisplayName("should_return_400_for_invalid_provider")
    final void should_return_400_for_invalid_provider() {
      webTestClient
          .post()
          .uri("/webhooks")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
              """
              {
                "provider": "bitbucket",
                "repositoryId": "owner/repo",
                "changeRequestId": 123
              }
              """)
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.error")
          .isEqualTo("validation_error")
          .jsonPath("$.message")
          .value(
              message ->
                  assertThat((String) message).contains("Provider must be 'github' or 'gitlab'"));
    }

    @Test
    @DisplayName("should_return_400_for_missing_repository_id")
    final void should_return_400_for_missing_repository_id() {
      webTestClient
          .post()
          .uri("/webhooks")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
              """
              {
                "provider": "github",
                "changeRequestId": 123
              }
              """)
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.error")
          .isEqualTo("validation_error");
    }

    @Test
    @DisplayName("should_return_400_for_missing_change_request_id")
    final void should_return_400_for_missing_change_request_id() {
      webTestClient
          .post()
          .uri("/webhooks")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
              """
              {
                "provider": "github",
                "repositoryId": "owner/repo"
              }
              """)
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.error")
          .isEqualTo("validation_error");
    }

    @Test
    @DisplayName("should_return_400_for_negative_change_request_id")
    final void should_return_400_for_negative_change_request_id() {
      webTestClient
          .post()
          .uri("/webhooks")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
              """
              {
                "provider": "github",
                "repositoryId": "owner/repo",
                "changeRequestId": -1
              }
              """)
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.error")
          .isEqualTo("validation_error")
          .jsonPath("$.message")
          .value(
              message ->
                  assertThat((String) message).contains("Change request ID must be positive"));
    }
  }

  @Nested
  @DisplayName("Internal Errors")
  final class InternalErrorTests {

    @Test
    @DisplayName("should_return_500_for_unexpected_errors")
    final void should_return_500_for_unexpected_errors() {
      webhookService.setError(new RuntimeException("Unexpected error"));

      webTestClient
          .post()
          .uri("/webhooks")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
              """
              {
                "provider": "github",
                "repositoryId": "owner/repo",
                "changeRequestId": 123
              }
              """)
          .exchange()
          .expectStatus()
          .is5xxServerError()
          .expectBody()
          .jsonPath("$.error")
          .isEqualTo("internal_error")
          .jsonPath("$.message")
          .isEqualTo("An unexpected error occurred");
    }
  }

  private static final class TestWebhookService extends WebhookService {
    private WebhookRequest capturedRequest;
    private String capturedIdempotencyKey;
    private WebhookResponse response;
    private boolean alreadyProcessed = false;
    private String alreadyProcessedKey;
    private Throwable error;

    TestWebhookService() {
      super(null, null);
    }

    @Override
    public Mono<WebhookResponse> processWebhook(
        final WebhookRequest request, final String idempotencyKey) {
      this.capturedRequest = request;
      this.capturedIdempotencyKey = idempotencyKey;

      if (error != null) {
        return Mono.error(error);
      }

      if (alreadyProcessed) {
        return Mono.error(new AlreadyProcessedException(alreadyProcessedKey));
      }

      return Mono.just(response);
    }

    void setResponse(final WebhookResponse response) {
      this.response = response;
    }

    void setAlreadyProcessed(final boolean alreadyProcessed, final String key) {
      this.alreadyProcessed = alreadyProcessed;
      this.alreadyProcessedKey = key;
    }

    void setError(final Throwable error) {
      this.error = error;
    }

    WebhookRequest getCapturedRequest() {
      return capturedRequest;
    }

    String getCapturedIdempotencyKey() {
      return capturedIdempotencyKey;
    }
  }
}
