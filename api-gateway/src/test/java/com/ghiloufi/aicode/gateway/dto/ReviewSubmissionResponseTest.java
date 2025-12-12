package com.ghiloufi.aicode.gateway.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.async.ReviewStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewSubmissionResponse Tests")
final class ReviewSubmissionResponseTest {

  @Nested
  @DisplayName("Factory Method: pending")
  final class PendingFactoryTests {

    @Test
    @DisplayName("should_create_pending_response_with_status_url")
    final void should_create_pending_response_with_status_url() {
      final String requestId = "req-submit-123";
      final String statusUrl = "/api/v1/reviews/req-submit-123/status";

      final ReviewSubmissionResponse response =
          ReviewSubmissionResponse.pending(requestId, statusUrl);

      assertThat(response.requestId()).isEqualTo(requestId);
      assertThat(response.status()).isEqualTo(ReviewStatus.PENDING);
      assertThat(response.statusUrl()).isEqualTo(statusUrl);
    }

    @Test
    @DisplayName("should_accept_null_request_id")
    final void should_accept_null_request_id() {
      final ReviewSubmissionResponse response =
          ReviewSubmissionResponse.pending(null, "/status/null");

      assertThat(response.requestId()).isNull();
      assertThat(response.status()).isEqualTo(ReviewStatus.PENDING);
    }

    @Test
    @DisplayName("should_accept_null_status_url")
    final void should_accept_null_status_url() {
      final ReviewSubmissionResponse response = ReviewSubmissionResponse.pending("req-123", null);

      assertThat(response.statusUrl()).isNull();
      assertThat(response.status()).isEqualTo(ReviewStatus.PENDING);
    }

    @Test
    @DisplayName("should_accept_absolute_url_as_status_url")
    final void should_accept_absolute_url_as_status_url() {
      final String absoluteUrl = "https://api.example.com/reviews/req-123/status";

      final ReviewSubmissionResponse response =
          ReviewSubmissionResponse.pending("req-123", absoluteUrl);

      assertThat(response.statusUrl()).isEqualTo(absoluteUrl);
    }
  }

  @Nested
  @DisplayName("Direct Construction")
  final class DirectConstructionTests {

    @Test
    @DisplayName("should_allow_direct_construction_with_any_status")
    final void should_allow_direct_construction_with_any_status() {
      final ReviewSubmissionResponse response =
          new ReviewSubmissionResponse("req-456", ReviewStatus.PROCESSING, "/status/456");

      assertThat(response.requestId()).isEqualTo("req-456");
      assertThat(response.status()).isEqualTo(ReviewStatus.PROCESSING);
      assertThat(response.statusUrl()).isEqualTo("/status/456");
    }

    @Test
    @DisplayName("should_create_response_with_completed_status")
    final void should_create_response_with_completed_status() {
      final ReviewSubmissionResponse response =
          new ReviewSubmissionResponse("req-done", ReviewStatus.COMPLETED, "/results/done");

      assertThat(response.status()).isEqualTo(ReviewStatus.COMPLETED);
    }

    @Test
    @DisplayName("should_create_response_with_failed_status")
    final void should_create_response_with_failed_status() {
      final ReviewSubmissionResponse response =
          new ReviewSubmissionResponse("req-err", ReviewStatus.FAILED, "/errors/err");

      assertThat(response.status()).isEqualTo(ReviewStatus.FAILED);
    }
  }

  @Nested
  @DisplayName("Record Equality")
  final class RecordEqualityTests {

    @Test
    @DisplayName("should_be_equal_for_same_values")
    final void should_be_equal_for_same_values() {
      final ReviewSubmissionResponse response1 =
          ReviewSubmissionResponse.pending("req-eq", "/status/eq");
      final ReviewSubmissionResponse response2 =
          ReviewSubmissionResponse.pending("req-eq", "/status/eq");

      assertThat(response1).isEqualTo(response2);
      assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("should_not_be_equal_for_different_request_ids")
    final void should_not_be_equal_for_different_request_ids() {
      final ReviewSubmissionResponse response1 =
          ReviewSubmissionResponse.pending("req-1", "/status");
      final ReviewSubmissionResponse response2 =
          ReviewSubmissionResponse.pending("req-2", "/status");

      assertThat(response1).isNotEqualTo(response2);
    }
  }
}
