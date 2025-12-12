package com.ghiloufi.aicode.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import com.ghiloufi.aicode.core.domain.model.async.ReviewStatus;
import com.ghiloufi.aicode.gateway.async.ReviewRequestProducer;
import com.ghiloufi.aicode.gateway.dto.ReviewStatusResponse;
import com.ghiloufi.aicode.gateway.dto.ReviewSubmissionResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class AsyncReviewControllerTest {

  private WebTestClient webTestClient;
  private TestReviewRequestProducer producer;
  private ReactiveStringRedisTemplate redisTemplate;
  private ReactiveHashOperations<String, Object, Object> hashOperations;
  private ObjectMapper objectMapper;

  @BeforeEach
  @SuppressWarnings("unchecked")
  final void setUp() {
    producer = new TestReviewRequestProducer();
    redisTemplate = mock(ReactiveStringRedisTemplate.class);
    hashOperations = mock(ReactiveHashOperations.class);
    when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    objectMapper = new ObjectMapper();
    final AsyncReviewController controller =
        new AsyncReviewController(producer, redisTemplate, objectMapper);

    webTestClient =
        WebTestClient.bindToController(controller)
            .configureClient()
            .responseTimeout(Duration.ofSeconds(5))
            .build();
  }

  @Nested
  @DisplayName("submitAsyncReview")
  final class SubmitAsyncReviewTests {

    @Test
    @DisplayName("should_submit_async_review_for_github_provider")
    final void should_submit_async_review_for_github_provider() {
      producer.setSuccess(true);

      webTestClient
          .post()
          .uri("/api/v1/async-reviews/github/owner%2Frepo/change-requests/123")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isAccepted()
          .expectBody(ReviewSubmissionResponse.class)
          .value(
              response -> {
                assertThat(response.requestId()).isNotBlank();
                assertThat(response.status()).isEqualTo(ReviewStatus.PENDING);
                assertThat(response.statusUrl()).contains("/api/v1/async-reviews/");
                assertThat(response.statusUrl()).endsWith("/status");
              });

      assertThat(producer.getCapturedRequest()).isNotNull();
      assertThat(producer.getCapturedRequest().repositoryId()).isEqualTo("owner/repo");
      assertThat(producer.getCapturedRequest().changeRequestId()).isEqualTo(123);
    }

    @Test
    @DisplayName("should_submit_async_review_for_gitlab_provider")
    final void should_submit_async_review_for_gitlab_provider() {
      producer.setSuccess(true);

      webTestClient
          .post()
          .uri("/api/v1/async-reviews/gitlab/group%2Fproject/change-requests/456")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isAccepted()
          .expectBody(ReviewSubmissionResponse.class)
          .value(
              response -> {
                assertThat(response.requestId()).isNotBlank();
                assertThat(response.status()).isEqualTo(ReviewStatus.PENDING);
              });

      assertThat(producer.getCapturedRequest()).isNotNull();
      assertThat(producer.getCapturedRequest().repositoryId()).isEqualTo("group/project");
      assertThat(producer.getCapturedRequest().changeRequestId()).isEqualTo(456);
    }

    @Test
    @DisplayName("should_return_error_response_when_producer_fails")
    final void should_return_error_response_when_producer_fails() {
      producer.setSuccess(false);

      webTestClient
          .post()
          .uri("/api/v1/async-reviews/github/owner%2Frepo/change-requests/1")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .is5xxServerError()
          .expectBody(ReviewSubmissionResponse.class)
          .value(
              response -> {
                assertThat(response.status()).isEqualTo(ReviewStatus.FAILED);
              });
    }
  }

  @Nested
  @DisplayName("getStatus")
  final class GetStatusTests {

    @Test
    @DisplayName("should_return_pending_status_when_result_not_found")
    final void should_return_pending_status_when_result_not_found() {
      when(hashOperations.entries(anyString())).thenReturn(Flux.empty());

      webTestClient
          .get()
          .uri("/api/v1/async-reviews/request-id-123/status")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(ReviewStatusResponse.class)
          .value(
              response -> {
                assertThat(response.requestId()).isEqualTo("request-id-123");
                assertThat(response.status()).isEqualTo(ReviewStatus.PENDING);
              });
    }

    @Test
    @DisplayName("should_return_failed_status_with_error_message")
    final void should_return_failed_status_with_error_message() {
      final Map<Object, Object> resultData = new HashMap<>();
      resultData.put("status", "FAILED");
      resultData.put("error", "Review processing failed");
      when(hashOperations.entries(anyString()))
          .thenReturn(Flux.fromIterable(resultData.entrySet()));

      webTestClient
          .get()
          .uri("/api/v1/async-reviews/failed-request/status")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(ReviewStatusResponse.class)
          .value(
              response -> {
                assertThat(response.requestId()).isEqualTo("failed-request");
                assertThat(response.status()).isEqualTo(ReviewStatus.FAILED);
                assertThat(response.error()).isEqualTo("Review processing failed");
              });
    }

    @Test
    @DisplayName("should_return_processing_status_when_not_completed")
    final void should_return_processing_status_when_not_completed() {
      final Map<Object, Object> resultData = new HashMap<>();
      resultData.put("status", "PROCESSING");
      when(hashOperations.entries(anyString()))
          .thenReturn(Flux.fromIterable(resultData.entrySet()));

      webTestClient
          .get()
          .uri("/api/v1/async-reviews/processing-request/status")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(ReviewStatusResponse.class)
          .value(
              response -> {
                assertThat(response.requestId()).isEqualTo("processing-request");
                assertThat(response.status()).isEqualTo(ReviewStatus.PROCESSING);
              });
    }

    @Test
    @DisplayName("should_return_completed_status_with_result")
    final void should_return_completed_status_with_result() throws Exception {
      final ReviewResult reviewResult =
          ReviewResult.builder().summary("Test review summary").build();
      final String resultJson = objectMapper.writeValueAsString(reviewResult);

      final Map<Object, Object> resultData = new HashMap<>();
      resultData.put("status", "COMPLETED");
      resultData.put("result", resultJson);
      resultData.put("processingTimeMs", "1500");
      when(hashOperations.entries(anyString()))
          .thenReturn(Flux.fromIterable(resultData.entrySet()));

      webTestClient
          .get()
          .uri("/api/v1/async-reviews/completed-request/status")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(ReviewStatusResponse.class)
          .value(
              response -> {
                assertThat(response.requestId()).isEqualTo("completed-request");
                assertThat(response.status()).isEqualTo(ReviewStatus.COMPLETED);
                assertThat(response.result()).isNotNull();
                assertThat(response.result().getSummary()).isEqualTo("Test review summary");
                assertThat(response.processingTimeMs()).isEqualTo(1500L);
              });
    }

    @Test
    @DisplayName("should_return_error_response_when_redis_fails")
    final void should_return_error_response_when_redis_fails() {
      when(hashOperations.entries(anyString()))
          .thenReturn(Flux.error(new RuntimeException("Redis connection failed")));

      webTestClient
          .get()
          .uri("/api/v1/async-reviews/error-request/status")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .is5xxServerError();
    }
  }

  @Nested
  @DisplayName("getResult")
  final class GetResultTests {

    @Test
    @DisplayName("should_return_not_found_when_result_not_exists")
    final void should_return_not_found_when_result_not_exists() {
      when(hashOperations.entries(anyString())).thenReturn(Flux.empty());

      webTestClient
          .get()
          .uri("/api/v1/async-reviews/non-existing")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isNotFound();
    }

    @Test
    @DisplayName("should_return_completed_result")
    final void should_return_completed_result() throws Exception {
      final ReviewResult reviewResult = ReviewResult.builder().summary("Completed review").build();
      final String resultJson = objectMapper.writeValueAsString(reviewResult);

      final Map<Object, Object> resultData = new HashMap<>();
      resultData.put("status", "COMPLETED");
      resultData.put("result", resultJson);
      resultData.put("processingTimeMs", "2000");
      when(hashOperations.entries(anyString()))
          .thenReturn(Flux.fromIterable(resultData.entrySet()));

      webTestClient
          .get()
          .uri("/api/v1/async-reviews/completed-id")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(ReviewStatusResponse.class)
          .value(
              response -> {
                assertThat(response.status()).isEqualTo(ReviewStatus.COMPLETED);
                assertThat(response.result().getSummary()).isEqualTo("Completed review");
              });
    }

    @Test
    @DisplayName("should_return_failed_result")
    final void should_return_failed_result() {
      final Map<Object, Object> resultData = new HashMap<>();
      resultData.put("status", "FAILED");
      resultData.put("error", "Processing error");
      when(hashOperations.entries(anyString()))
          .thenReturn(Flux.fromIterable(resultData.entrySet()));

      webTestClient
          .get()
          .uri("/api/v1/async-reviews/failed-id")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(ReviewStatusResponse.class)
          .value(
              response -> {
                assertThat(response.status()).isEqualTo(ReviewStatus.FAILED);
                assertThat(response.error()).isEqualTo("Processing error");
              });
    }

    @Test
    @DisplayName("should_return_processing_status_for_incomplete_result")
    final void should_return_processing_status_for_incomplete_result() {
      final Map<Object, Object> resultData = new HashMap<>();
      resultData.put("status", "PROCESSING");
      when(hashOperations.entries(anyString()))
          .thenReturn(Flux.fromIterable(resultData.entrySet()));

      webTestClient
          .get()
          .uri("/api/v1/async-reviews/in-progress-id")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(ReviewStatusResponse.class)
          .value(
              response -> {
                assertThat(response.status()).isEqualTo(ReviewStatus.PROCESSING);
              });
    }

    @Test
    @DisplayName("should_return_failed_when_json_parsing_fails")
    final void should_return_failed_when_json_parsing_fails() {
      final Map<Object, Object> resultData = new HashMap<>();
      resultData.put("status", "COMPLETED");
      resultData.put("result", "invalid-json");
      resultData.put("processingTimeMs", "1000");
      when(hashOperations.entries(anyString()))
          .thenReturn(Flux.fromIterable(resultData.entrySet()));

      webTestClient
          .get()
          .uri("/api/v1/async-reviews/parse-error-id")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(ReviewStatusResponse.class)
          .value(
              response -> {
                assertThat(response.status()).isEqualTo(ReviewStatus.FAILED);
                assertThat(response.error()).contains("Failed to parse result");
              });
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
}
