package com.ghiloufi.aicode.core.domain.model.async;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewMode;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AsyncReviewRequest Tests")
final class AsyncReviewRequestTest {

  @Nested
  @DisplayName("Factory Method: create")
  final class CreateFactoryTests {

    @Test
    @DisplayName("should_create_request_with_all_fields")
    final void should_create_request_with_all_fields() {
      final Instant before = Instant.now();

      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req-123", SourceProvider.GITHUB, "owner/repo", 42);

      final Instant after = Instant.now();

      assertThat(request.requestId()).isEqualTo("req-123");
      assertThat(request.provider()).isEqualTo(SourceProvider.GITHUB);
      assertThat(request.repositoryId()).isEqualTo("owner/repo");
      assertThat(request.changeRequestId()).isEqualTo(42);
      assertThat(request.reviewMode()).isEqualTo(ReviewMode.DIFF);
      assertThat(request.createdAt()).isNotNull();
      assertThat(request.createdAt()).isAfterOrEqualTo(before);
      assertThat(request.createdAt()).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("should_create_request_for_gitlab_provider")
    final void should_create_request_for_gitlab_provider() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req-gitlab", SourceProvider.GITLAB, "group/project", 99);

      assertThat(request.provider()).isEqualTo(SourceProvider.GITLAB);
      assertThat(request.repositoryId()).isEqualTo("group/project");
    }

    @Test
    @DisplayName("should_set_created_at_to_current_time")
    final void should_set_created_at_to_current_time() {
      final Instant before = Instant.now();

      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req-time", SourceProvider.GITHUB, "repo", 1);

      assertThat(request.createdAt()).isAfterOrEqualTo(before);
      assertThat(request.createdAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("should_accept_null_request_id")
    final void should_accept_null_request_id() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(null, SourceProvider.GITHUB, "repo", 1);

      assertThat(request.requestId()).isNull();
    }

    @Test
    @DisplayName("should_accept_null_provider")
    final void should_accept_null_provider() {
      final AsyncReviewRequest request = AsyncReviewRequest.create("req", null, "repo", 1);

      assertThat(request.provider()).isNull();
    }

    @Test
    @DisplayName("should_accept_large_change_request_id")
    final void should_accept_large_change_request_id() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req", SourceProvider.GITHUB, "repo", Integer.MAX_VALUE);

      assertThat(request.changeRequestId()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("should_default_to_diff_review_mode")
    final void should_default_to_diff_review_mode() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req", SourceProvider.GITHUB, "repo", 1);

      assertThat(request.reviewMode()).isEqualTo(ReviewMode.DIFF);
    }

    @Test
    @DisplayName("should_create_request_with_agentic_review_mode")
    final void should_create_request_with_agentic_review_mode() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(
              "req-agentic", SourceProvider.GITHUB, "repo", 1, ReviewMode.AGENTIC);

      assertThat(request.reviewMode()).isEqualTo(ReviewMode.AGENTIC);
    }

    @Test
    @DisplayName("should_create_request_with_diff_review_mode")
    final void should_create_request_with_diff_review_mode() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req-diff", SourceProvider.GITHUB, "repo", 1, ReviewMode.DIFF);

      assertThat(request.reviewMode()).isEqualTo(ReviewMode.DIFF);
    }
  }

  @Nested
  @DisplayName("Direct Construction")
  final class DirectConstructionTests {

    @Test
    @DisplayName("should_allow_direct_construction_with_specific_timestamp")
    final void should_allow_direct_construction_with_specific_timestamp() {
      final Instant specificTime = Instant.parse("2024-06-15T10:30:00Z");

      final AsyncReviewRequest request =
          new AsyncReviewRequest(
              "req-specific", SourceProvider.GITLAB, "project", 50, ReviewMode.DIFF, specificTime);

      assertThat(request.createdAt()).isEqualTo(specificTime);
    }

    @Test
    @DisplayName("should_preserve_all_values_in_direct_construction")
    final void should_preserve_all_values_in_direct_construction() {
      final Instant timestamp = Instant.now();

      final AsyncReviewRequest request =
          new AsyncReviewRequest(
              "id", SourceProvider.GITHUB, "owner/repo", 123, ReviewMode.AGENTIC, timestamp);

      assertThat(request.requestId()).isEqualTo("id");
      assertThat(request.provider()).isEqualTo(SourceProvider.GITHUB);
      assertThat(request.repositoryId()).isEqualTo("owner/repo");
      assertThat(request.changeRequestId()).isEqualTo(123);
      assertThat(request.reviewMode()).isEqualTo(ReviewMode.AGENTIC);
      assertThat(request.createdAt()).isEqualTo(timestamp);
    }
  }

  @Nested
  @DisplayName("Record Equality")
  final class RecordEqualityTests {

    @Test
    @DisplayName("should_be_equal_for_same_values")
    final void should_be_equal_for_same_values() {
      final Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");

      final AsyncReviewRequest request1 =
          new AsyncReviewRequest(
              "req", SourceProvider.GITHUB, "repo", 1, ReviewMode.DIFF, timestamp);
      final AsyncReviewRequest request2 =
          new AsyncReviewRequest(
              "req", SourceProvider.GITHUB, "repo", 1, ReviewMode.DIFF, timestamp);

      assertThat(request1).isEqualTo(request2);
      assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    @DisplayName("should_not_be_equal_for_different_timestamps")
    final void should_not_be_equal_for_different_timestamps() {
      final AsyncReviewRequest request1 =
          new AsyncReviewRequest(
              "req",
              SourceProvider.GITHUB,
              "repo",
              1,
              ReviewMode.DIFF,
              Instant.parse("2024-01-01T00:00:00Z"));
      final AsyncReviewRequest request2 =
          new AsyncReviewRequest(
              "req",
              SourceProvider.GITHUB,
              "repo",
              1,
              ReviewMode.DIFF,
              Instant.parse("2024-01-02T00:00:00Z"));

      assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    @DisplayName("should_not_be_equal_for_different_providers")
    final void should_not_be_equal_for_different_providers() {
      final Instant timestamp = Instant.now();

      final AsyncReviewRequest request1 =
          new AsyncReviewRequest(
              "req", SourceProvider.GITHUB, "repo", 1, ReviewMode.DIFF, timestamp);
      final AsyncReviewRequest request2 =
          new AsyncReviewRequest(
              "req", SourceProvider.GITLAB, "repo", 1, ReviewMode.DIFF, timestamp);

      assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    @DisplayName("should_not_be_equal_for_different_review_modes")
    final void should_not_be_equal_for_different_review_modes() {
      final Instant timestamp = Instant.now();

      final AsyncReviewRequest request1 =
          new AsyncReviewRequest(
              "req", SourceProvider.GITHUB, "repo", 1, ReviewMode.DIFF, timestamp);
      final AsyncReviewRequest request2 =
          new AsyncReviewRequest(
              "req", SourceProvider.GITHUB, "repo", 1, ReviewMode.AGENTIC, timestamp);

      assertThat(request1).isNotEqualTo(request2);
    }
  }

  @Nested
  @DisplayName("isAgenticMode")
  final class IsAgenticModeTests {

    @Test
    @DisplayName("should_return_true_when_agentic_mode")
    final void should_return_true_when_agentic_mode() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req", SourceProvider.GITHUB, "repo", 1, ReviewMode.AGENTIC);

      assertThat(request.isAgenticMode()).isTrue();
    }

    @Test
    @DisplayName("should_return_false_when_diff_mode")
    final void should_return_false_when_diff_mode() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req", SourceProvider.GITHUB, "repo", 1, ReviewMode.DIFF);

      assertThat(request.isAgenticMode()).isFalse();
    }

    @Test
    @DisplayName("should_return_false_for_default_create")
    final void should_return_false_for_default_create() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req", SourceProvider.GITHUB, "repo", 1);

      assertThat(request.isAgenticMode()).isFalse();
    }
  }
}
