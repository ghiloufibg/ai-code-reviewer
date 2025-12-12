package com.ghiloufi.aicode.gateway.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.async.ReviewStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewStatusResponse Tests")
final class ReviewStatusResponseTest {

  @Nested
  @DisplayName("Factory Method: pending")
  final class PendingFactoryTests {

    @Test
    @DisplayName("should_create_pending_response_with_request_id")
    final void should_create_pending_response_with_request_id() {
      final String requestId = "req-12345";

      final ReviewStatusResponse response = ReviewStatusResponse.pending(requestId);

      assertThat(response.requestId()).isEqualTo(requestId);
      assertThat(response.status()).isEqualTo(ReviewStatus.PENDING);
      assertThat(response.result()).isNull();
      assertThat(response.error()).isNull();
      assertThat(response.processingTimeMs()).isNull();
    }

    @Test
    @DisplayName("should_accept_null_request_id_for_pending")
    final void should_accept_null_request_id_for_pending() {
      final ReviewStatusResponse response = ReviewStatusResponse.pending(null);

      assertThat(response.requestId()).isNull();
      assertThat(response.status()).isEqualTo(ReviewStatus.PENDING);
    }
  }

  @Nested
  @DisplayName("Factory Method: processing")
  final class ProcessingFactoryTests {

    @Test
    @DisplayName("should_create_processing_response_with_request_id")
    final void should_create_processing_response_with_request_id() {
      final String requestId = "req-67890";

      final ReviewStatusResponse response = ReviewStatusResponse.processing(requestId);

      assertThat(response.requestId()).isEqualTo(requestId);
      assertThat(response.status()).isEqualTo(ReviewStatus.PROCESSING);
      assertThat(response.result()).isNull();
      assertThat(response.error()).isNull();
      assertThat(response.processingTimeMs()).isNull();
    }
  }

  @Nested
  @DisplayName("Factory Method: completed")
  final class CompletedFactoryTests {

    @Test
    @DisplayName("should_create_completed_response_with_result_and_processing_time")
    final void should_create_completed_response_with_result_and_processing_time() {
      final String requestId = "req-completed";
      final ReviewResult result = ReviewResult.builder().summary("Summary of code review").build();
      final long processingTimeMs = 1500L;

      final ReviewStatusResponse response =
          ReviewStatusResponse.completed(requestId, result, processingTimeMs);

      assertThat(response.requestId()).isEqualTo(requestId);
      assertThat(response.status()).isEqualTo(ReviewStatus.COMPLETED);
      assertThat(response.result()).isEqualTo(result);
      assertThat(response.result().getSummary()).isEqualTo("Summary of code review");
      assertThat(response.error()).isNull();
      assertThat(response.processingTimeMs()).isEqualTo(processingTimeMs);
    }

    @Test
    @DisplayName("should_accept_zero_processing_time")
    final void should_accept_zero_processing_time() {
      final ReviewResult result = ReviewResult.builder().summary("Quick review").build();

      final ReviewStatusResponse response = ReviewStatusResponse.completed("req-fast", result, 0L);

      assertThat(response.processingTimeMs()).isEqualTo(0L);
    }

    @Test
    @DisplayName("should_accept_null_result")
    final void should_accept_null_result() {
      final ReviewStatusResponse response = ReviewStatusResponse.completed("req-null", null, 100L);

      assertThat(response.result()).isNull();
      assertThat(response.status()).isEqualTo(ReviewStatus.COMPLETED);
    }
  }

  @Nested
  @DisplayName("Factory Method: failed")
  final class FailedFactoryTests {

    @Test
    @DisplayName("should_create_failed_response_with_error_message")
    final void should_create_failed_response_with_error_message() {
      final String requestId = "req-failed";
      final String error = "LLM service unavailable";

      final ReviewStatusResponse response = ReviewStatusResponse.failed(requestId, error);

      assertThat(response.requestId()).isEqualTo(requestId);
      assertThat(response.status()).isEqualTo(ReviewStatus.FAILED);
      assertThat(response.result()).isNull();
      assertThat(response.error()).isEqualTo(error);
      assertThat(response.processingTimeMs()).isNull();
    }

    @Test
    @DisplayName("should_accept_null_error_message")
    final void should_accept_null_error_message() {
      final ReviewStatusResponse response = ReviewStatusResponse.failed("req-null-err", null);

      assertThat(response.error()).isNull();
      assertThat(response.status()).isEqualTo(ReviewStatus.FAILED);
    }

    @Test
    @DisplayName("should_accept_empty_error_message")
    final void should_accept_empty_error_message() {
      final ReviewStatusResponse response = ReviewStatusResponse.failed("req-empty-err", "");

      assertThat(response.error()).isEmpty();
      assertThat(response.status()).isEqualTo(ReviewStatus.FAILED);
    }
  }

  @Nested
  @DisplayName("Record Equality")
  final class RecordEqualityTests {

    @Test
    @DisplayName("should_be_equal_for_same_values")
    final void should_be_equal_for_same_values() {
      final ReviewStatusResponse response1 = ReviewStatusResponse.pending("req-123");
      final ReviewStatusResponse response2 = ReviewStatusResponse.pending("req-123");

      assertThat(response1).isEqualTo(response2);
      assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("should_not_be_equal_for_different_status")
    final void should_not_be_equal_for_different_status() {
      final ReviewStatusResponse pending = ReviewStatusResponse.pending("req-123");
      final ReviewStatusResponse processing = ReviewStatusResponse.processing("req-123");

      assertThat(pending).isNotEqualTo(processing);
    }
  }
}
