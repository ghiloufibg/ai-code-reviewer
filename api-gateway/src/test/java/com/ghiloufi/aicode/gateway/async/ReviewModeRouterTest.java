package com.ghiloufi.aicode.gateway.async;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewMode;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ReviewModeRouterTest {

  private ReviewModeRouter router;

  @BeforeEach
  final void setUp() {
    router = new ReviewModeRouter();
  }

  @Nested
  @DisplayName("route(AsyncReviewRequest)")
  final class RouteAsyncReviewRequestTests {

    @Test
    @DisplayName("should_route_diff_mode_request_to_diff_stream")
    final void should_route_diff_mode_request_to_diff_stream() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(
              "req-1", SourceProvider.GITHUB, "owner/repo", 1, ReviewMode.DIFF);

      final ReviewModeRouter.StreamKey result = router.route(request);

      assertThat(result).isEqualTo(ReviewModeRouter.StreamKey.DIFF_REQUESTS);
      assertThat(result.getKey()).isEqualTo("review:requests");
    }

    @Test
    @DisplayName("should_route_agentic_mode_request_to_agent_stream")
    final void should_route_agentic_mode_request_to_agent_stream() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(
              "req-2", SourceProvider.GITLAB, "group/project", 42, ReviewMode.AGENTIC);

      final ReviewModeRouter.StreamKey result = router.route(request);

      assertThat(result).isEqualTo(ReviewModeRouter.StreamKey.AGENT_REQUESTS);
      assertThat(result.getKey()).isEqualTo("review:agent-requests");
    }

    @Test
    @DisplayName("should_route_github_diff_request_correctly")
    final void should_route_github_diff_request_correctly() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create("req-github", SourceProvider.GITHUB, "org/repo", 100);

      final ReviewModeRouter.StreamKey result = router.route(request);

      assertThat(result).isEqualTo(ReviewModeRouter.StreamKey.DIFF_REQUESTS);
    }

    @Test
    @DisplayName("should_route_gitlab_agentic_request_correctly")
    final void should_route_gitlab_agentic_request_correctly() {
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(
              "req-gitlab", SourceProvider.GITLAB, "group/project", 200, ReviewMode.AGENTIC);

      final ReviewModeRouter.StreamKey result = router.route(request);

      assertThat(result).isEqualTo(ReviewModeRouter.StreamKey.AGENT_REQUESTS);
    }
  }

  @Nested
  @DisplayName("route(ReviewMode)")
  final class RouteReviewModeTests {

    @Test
    @DisplayName("should_route_diff_mode_to_diff_stream")
    final void should_route_diff_mode_to_diff_stream() {
      final ReviewModeRouter.StreamKey result = router.route(ReviewMode.DIFF);

      assertThat(result).isEqualTo(ReviewModeRouter.StreamKey.DIFF_REQUESTS);
    }

    @Test
    @DisplayName("should_route_agentic_mode_to_agent_stream")
    final void should_route_agentic_mode_to_agent_stream() {
      final ReviewModeRouter.StreamKey result = router.route(ReviewMode.AGENTIC);

      assertThat(result).isEqualTo(ReviewModeRouter.StreamKey.AGENT_REQUESTS);
    }
  }

  @Nested
  @DisplayName("StreamKey enum")
  final class StreamKeyEnumTests {

    @Test
    @DisplayName("should_have_correct_key_for_diff_requests")
    final void should_have_correct_key_for_diff_requests() {
      assertThat(ReviewModeRouter.StreamKey.DIFF_REQUESTS.getKey()).isEqualTo("review:requests");
    }

    @Test
    @DisplayName("should_have_correct_key_for_agent_requests")
    final void should_have_correct_key_for_agent_requests() {
      assertThat(ReviewModeRouter.StreamKey.AGENT_REQUESTS.getKey())
          .isEqualTo("review:agent-requests");
    }

    @Test
    @DisplayName("should_have_two_stream_keys")
    final void should_have_two_stream_keys() {
      assertThat(ReviewModeRouter.StreamKey.values()).hasSize(2);
    }
  }
}
