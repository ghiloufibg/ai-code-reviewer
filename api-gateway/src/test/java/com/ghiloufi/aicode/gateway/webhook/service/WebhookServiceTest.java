package com.ghiloufi.aicode.gateway.webhook.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import com.ghiloufi.aicode.gateway.async.ReviewRequestProducer;
import com.ghiloufi.aicode.gateway.webhook.dto.WebhookRequest;
import com.ghiloufi.aicode.gateway.webhook.exception.AlreadyProcessedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.RecordId;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

final class WebhookServiceTest {

  private TestReviewRequestProducer reviewRequestProducer;
  private TestIdempotencyService idempotencyService;
  private WebhookService webhookService;

  @BeforeEach
  final void setUp() {
    reviewRequestProducer = new TestReviewRequestProducer();
    idempotencyService = new TestIdempotencyService();
    webhookService = new WebhookService(reviewRequestProducer, idempotencyService);
  }

  @Nested
  @DisplayName("Successful Processing")
  final class SuccessfulProcessingTests {

    @Test
    @DisplayName("should_queue_review_request_with_provided_idempotency_key")
    final void should_queue_review_request_with_provided_idempotency_key() {
      final WebhookRequest request =
          new WebhookRequest("github", "owner/repo", 123, "github-actions");
      final String idempotencyKey = "commit-sha-abc123";

      idempotencyService.setIsNew(true);
      reviewRequestProducer.setSuccess(true);

      StepVerifier.create(webhookService.processWebhook(request, idempotencyKey))
          .assertNext(
              response -> {
                assertThat(response.status()).isEqualTo("accepted");
                assertThat(response.message()).isEqualTo("Review request queued for processing");
                assertThat(response.requestId()).isNotBlank();
              })
          .verifyComplete();

      assertThat(idempotencyService.getCheckedKey()).isEqualTo(idempotencyKey);
      assertThat(reviewRequestProducer.getCapturedRequest()).isNotNull();
      assertThat(reviewRequestProducer.getCapturedRequest().provider())
          .isEqualTo(SourceProvider.GITHUB);
      assertThat(reviewRequestProducer.getCapturedRequest().repositoryId()).isEqualTo("owner/repo");
      assertThat(reviewRequestProducer.getCapturedRequest().changeRequestId()).isEqualTo(123);
    }

    @Test
    @DisplayName("should_generate_idempotency_key_when_not_provided")
    final void should_generate_idempotency_key_when_not_provided() {
      final WebhookRequest request =
          new WebhookRequest("gitlab", "group/project", 456, "gitlab-ci");

      idempotencyService.setIsNew(true);
      reviewRequestProducer.setSuccess(true);

      StepVerifier.create(webhookService.processWebhook(request, null))
          .assertNext(
              response -> {
                assertThat(response.status()).isEqualTo("accepted");
              })
          .verifyComplete();

      assertThat(idempotencyService.getCheckedKey()).isEqualTo("group/project:456");
    }

    @Test
    @DisplayName("should_generate_idempotency_key_when_blank")
    final void should_generate_idempotency_key_when_blank() {
      final WebhookRequest request = new WebhookRequest("github", "owner/repo", 1, "jenkins");

      idempotencyService.setIsNew(true);
      reviewRequestProducer.setSuccess(true);

      StepVerifier.create(webhookService.processWebhook(request, "   "))
          .assertNext(
              response -> {
                assertThat(response.status()).isEqualTo("accepted");
              })
          .verifyComplete();

      assertThat(idempotencyService.getCheckedKey()).isEqualTo("owner/repo:1");
    }

    @Test
    @DisplayName("should_map_github_provider_correctly")
    final void should_map_github_provider_correctly() {
      final WebhookRequest request = new WebhookRequest("github", "owner/repo", 10, null);

      idempotencyService.setIsNew(true);
      reviewRequestProducer.setSuccess(true);

      StepVerifier.create(webhookService.processWebhook(request, "test-key"))
          .expectNextCount(1)
          .verifyComplete();

      assertThat(reviewRequestProducer.getCapturedRequest().provider())
          .isEqualTo(SourceProvider.GITHUB);
    }

    @Test
    @DisplayName("should_map_gitlab_provider_correctly")
    final void should_map_gitlab_provider_correctly() {
      final WebhookRequest request = new WebhookRequest("gitlab", "group/project", 20, null);

      idempotencyService.setIsNew(true);
      reviewRequestProducer.setSuccess(true);

      StepVerifier.create(webhookService.processWebhook(request, "test-key"))
          .expectNextCount(1)
          .verifyComplete();

      assertThat(reviewRequestProducer.getCapturedRequest().provider())
          .isEqualTo(SourceProvider.GITLAB);
    }
  }

  @Nested
  @DisplayName("Duplicate Processing")
  final class DuplicateProcessingTests {

    @Test
    @DisplayName("should_reject_already_processed_event")
    final void should_reject_already_processed_event() {
      final WebhookRequest request = new WebhookRequest("github", "owner/repo", 123, null);
      final String idempotencyKey = "duplicate-key";

      idempotencyService.setIsNew(false);

      StepVerifier.create(webhookService.processWebhook(request, idempotencyKey))
          .expectErrorSatisfies(
              error -> {
                assertThat(error).isInstanceOf(AlreadyProcessedException.class);
                final AlreadyProcessedException exception = (AlreadyProcessedException) error;
                assertThat(exception.getIdempotencyKey()).isEqualTo(idempotencyKey);
              })
          .verify();

      assertThat(reviewRequestProducer.getCapturedRequest()).isNull();
    }
  }

  @Nested
  @DisplayName("Error Handling")
  final class ErrorHandlingTests {

    @Test
    @DisplayName("should_propagate_producer_errors")
    final void should_propagate_producer_errors() {
      final WebhookRequest request = new WebhookRequest("github", "owner/repo", 1, null);

      idempotencyService.setIsNew(true);
      reviewRequestProducer.setSuccess(false);

      StepVerifier.create(webhookService.processWebhook(request, "test-key"))
          .expectError(RuntimeException.class)
          .verify();
    }
  }

  private static final class TestReviewRequestProducer extends ReviewRequestProducer {
    private AsyncReviewRequest capturedRequest;
    private boolean success = true;

    TestReviewRequestProducer() {
      super(null, null);
    }

    @Override
    public Mono<RecordId> send(final AsyncReviewRequest request) {
      this.capturedRequest = request;
      if (success) {
        return Mono.just(RecordId.of("1234567890-0"));
      }
      return Mono.error(new RuntimeException("Failed to send to Redis"));
    }

    void setSuccess(final boolean success) {
      this.success = success;
    }

    AsyncReviewRequest getCapturedRequest() {
      return capturedRequest;
    }
  }

  private static final class TestIdempotencyService extends IdempotencyService {
    private String checkedKey;
    private boolean isNew = true;

    TestIdempotencyService() {
      super(null, null);
    }

    @Override
    public Mono<Boolean> checkAndMark(final String idempotencyKey) {
      this.checkedKey = idempotencyKey;
      return Mono.just(isNew);
    }

    void setIsNew(final boolean isNew) {
      this.isNew = isNew;
    }

    String getCheckedKey() {
      return checkedKey;
    }
  }
}
