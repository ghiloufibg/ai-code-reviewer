package com.ghiloufi.aicode.core.domain.model.async;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AsyncReviewResponse Tests")
final class AsyncReviewResponseTest {

  @Nested
  @DisplayName("Factory Method: success")
  final class SuccessFactoryTests {

    @Test
    @DisplayName("should_create_success_response_with_all_fields")
    final void should_create_success_response_with_all_fields() {
      final ReviewResult result = ReviewResult.builder().summary("Code looks good").build();
      final Instant before = Instant.now();

      final AsyncReviewResponse response =
          AsyncReviewResponse.success("req-123", result, "openai", "gpt-4", 1500L);

      final Instant after = Instant.now();

      assertThat(response.requestId()).isEqualTo("req-123");
      assertThat(response.result()).isEqualTo(result);
      assertThat(response.result().getSummary()).isEqualTo("Code looks good");
      assertThat(response.llmProvider()).isEqualTo("openai");
      assertThat(response.llmModel()).isEqualTo("gpt-4");
      assertThat(response.processingTimeMs()).isEqualTo(1500L);
      assertThat(response.completedAt()).isNotNull();
      assertThat(response.completedAt()).isAfterOrEqualTo(before);
      assertThat(response.completedAt()).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("should_create_success_response_for_anthropic_provider")
    final void should_create_success_response_for_anthropic_provider() {
      final ReviewResult result = ReviewResult.builder().summary("Review complete").build();

      final AsyncReviewResponse response =
          AsyncReviewResponse.success("req-456", result, "anthropic", "claude-3", 2000L);

      assertThat(response.llmProvider()).isEqualTo("anthropic");
      assertThat(response.llmModel()).isEqualTo("claude-3");
    }

    @Test
    @DisplayName("should_set_completed_at_to_current_time")
    final void should_set_completed_at_to_current_time() {
      final ReviewResult result = ReviewResult.builder().summary("Done").build();
      final Instant before = Instant.now();

      final AsyncReviewResponse response =
          AsyncReviewResponse.success("req", result, "provider", "model", 100L);

      assertThat(response.completedAt()).isAfterOrEqualTo(before);
      assertThat(response.completedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("should_accept_zero_processing_time")
    final void should_accept_zero_processing_time() {
      final ReviewResult result = ReviewResult.builder().summary("Fast").build();

      final AsyncReviewResponse response =
          AsyncReviewResponse.success("req", result, "provider", "model", 0L);

      assertThat(response.processingTimeMs()).isEqualTo(0L);
    }

    @Test
    @DisplayName("should_accept_large_processing_time")
    final void should_accept_large_processing_time() {
      final ReviewResult result = ReviewResult.builder().summary("Slow").build();

      final AsyncReviewResponse response =
          AsyncReviewResponse.success("req", result, "provider", "model", Long.MAX_VALUE);

      assertThat(response.processingTimeMs()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @DisplayName("should_accept_null_result")
    final void should_accept_null_result() {
      final AsyncReviewResponse response =
          AsyncReviewResponse.success("req", null, "provider", "model", 100L);

      assertThat(response.result()).isNull();
    }

    @Test
    @DisplayName("should_accept_null_provider")
    final void should_accept_null_provider() {
      final ReviewResult result = ReviewResult.builder().summary("Summary").build();

      final AsyncReviewResponse response =
          AsyncReviewResponse.success("req", result, null, "model", 100L);

      assertThat(response.llmProvider()).isNull();
    }

    @Test
    @DisplayName("should_accept_null_model")
    final void should_accept_null_model() {
      final ReviewResult result = ReviewResult.builder().summary("Summary").build();

      final AsyncReviewResponse response =
          AsyncReviewResponse.success("req", result, "provider", null, 100L);

      assertThat(response.llmModel()).isNull();
    }
  }

  @Nested
  @DisplayName("Direct Construction")
  final class DirectConstructionTests {

    @Test
    @DisplayName("should_allow_direct_construction_with_specific_timestamp")
    final void should_allow_direct_construction_with_specific_timestamp() {
      final ReviewResult result = ReviewResult.builder().summary("Direct").build();
      final Instant specificTime = Instant.parse("2024-06-15T12:00:00Z");

      final AsyncReviewResponse response =
          new AsyncReviewResponse(
              "req-direct", result, "openai", "gpt-4-turbo", 3000L, specificTime);

      assertThat(response.completedAt()).isEqualTo(specificTime);
    }

    @Test
    @DisplayName("should_preserve_all_values_in_direct_construction")
    final void should_preserve_all_values_in_direct_construction() {
      final ReviewResult result = ReviewResult.builder().summary("Test").build();
      final Instant timestamp = Instant.now();

      final AsyncReviewResponse response =
          new AsyncReviewResponse("id", result, "provider", "model", 999L, timestamp);

      assertThat(response.requestId()).isEqualTo("id");
      assertThat(response.result()).isEqualTo(result);
      assertThat(response.llmProvider()).isEqualTo("provider");
      assertThat(response.llmModel()).isEqualTo("model");
      assertThat(response.processingTimeMs()).isEqualTo(999L);
      assertThat(response.completedAt()).isEqualTo(timestamp);
    }
  }

  @Nested
  @DisplayName("Record Equality")
  final class RecordEqualityTests {

    @Test
    @DisplayName("should_be_equal_for_same_values")
    final void should_be_equal_for_same_values() {
      final ReviewResult result = ReviewResult.builder().summary("Same").build();
      final Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");

      final AsyncReviewResponse response1 =
          new AsyncReviewResponse("req", result, "provider", "model", 100L, timestamp);
      final AsyncReviewResponse response2 =
          new AsyncReviewResponse("req", result, "provider", "model", 100L, timestamp);

      assertThat(response1).isEqualTo(response2);
      assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("should_not_be_equal_for_different_processing_times")
    final void should_not_be_equal_for_different_processing_times() {
      final ReviewResult result = ReviewResult.builder().summary("Same").build();
      final Instant timestamp = Instant.now();

      final AsyncReviewResponse response1 =
          new AsyncReviewResponse("req", result, "provider", "model", 100L, timestamp);
      final AsyncReviewResponse response2 =
          new AsyncReviewResponse("req", result, "provider", "model", 200L, timestamp);

      assertThat(response1).isNotEqualTo(response2);
    }

    @Test
    @DisplayName("should_not_be_equal_for_different_providers")
    final void should_not_be_equal_for_different_providers() {
      final ReviewResult result = ReviewResult.builder().summary("Same").build();
      final Instant timestamp = Instant.now();

      final AsyncReviewResponse response1 =
          new AsyncReviewResponse("req", result, "openai", "model", 100L, timestamp);
      final AsyncReviewResponse response2 =
          new AsyncReviewResponse("req", result, "anthropic", "model", 100L, timestamp);

      assertThat(response1).isNotEqualTo(response2);
    }
  }
}
