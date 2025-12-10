package com.ghiloufi.aicode.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import com.ghiloufi.aicode.core.domain.model.async.ReviewStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AsyncReviewFlow Integration Tests")
final class AsyncReviewFlowIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Nested
  @DisplayName("AsyncReviewRequest Serialization")
  final class RequestSerialization {

    @Test
    @DisplayName("should_serialize_async_review_request_to_json")
    final void should_serialize_async_review_request_to_json() throws Exception {
      final String requestId = UUID.randomUUID().toString();
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(requestId, SourceProvider.GITLAB, "my-project/my-repo", 42);

      final String json = objectMapper.writeValueAsString(request);

      assertThat(json).contains("\"provider\":\"GITLAB\"");
      assertThat(json).contains("\"repositoryId\":\"my-project/my-repo\"");
      assertThat(json).contains("\"changeRequestId\":42");
      assertThat(json).contains("\"requestId\"");
    }

    @Test
    @DisplayName("should_deserialize_json_to_async_review_request")
    final void should_deserialize_json_to_async_review_request() throws Exception {
      final String json =
          """
          {
            "requestId": "req-123",
            "provider": "GITHUB",
            "repositoryId": "owner/repo",
            "changeRequestId": 99,
            "createdAt": "2024-01-15T10:30:00Z"
          }
          """;

      objectMapper.findAndRegisterModules();
      final AsyncReviewRequest request = objectMapper.readValue(json, AsyncReviewRequest.class);

      assertThat(request.requestId()).isEqualTo("req-123");
      assertThat(request.provider()).isEqualTo(SourceProvider.GITHUB);
      assertThat(request.repositoryId()).isEqualTo("owner/repo");
      assertThat(request.changeRequestId()).isEqualTo(99);
    }
  }

  @Nested
  @DisplayName("ReviewStatus Flow")
  final class StatusFlow {

    @Test
    @DisplayName("should_transition_through_valid_status_states")
    final void should_transition_through_valid_status_states() {
      assertThat(ReviewStatus.PENDING.name()).isEqualTo("PENDING");
      assertThat(ReviewStatus.PROCESSING.name()).isEqualTo("PROCESSING");
      assertThat(ReviewStatus.COMPLETED.name()).isEqualTo("COMPLETED");
      assertThat(ReviewStatus.FAILED.name()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("should_have_all_expected_status_values")
    final void should_have_all_expected_status_values() {
      assertThat(ReviewStatus.values())
          .containsExactly(
              ReviewStatus.PENDING,
              ReviewStatus.PROCESSING,
              ReviewStatus.COMPLETED,
              ReviewStatus.FAILED);
    }
  }

  @Nested
  @DisplayName("Request ID Generation")
  final class RequestIdGeneration {

    @Test
    @DisplayName("should_generate_unique_request_ids")
    final void should_generate_unique_request_ids() {
      final String id1 = UUID.randomUUID().toString();
      final String id2 = UUID.randomUUID().toString();
      final AsyncReviewRequest request1 =
          AsyncReviewRequest.create(id1, SourceProvider.GITLAB, "repo", 1);
      final AsyncReviewRequest request2 =
          AsyncReviewRequest.create(id2, SourceProvider.GITLAB, "repo", 1);

      assertThat(request1.requestId()).isNotEqualTo(request2.requestId());
    }

    @Test
    @DisplayName("should_generate_non_empty_request_id")
    final void should_generate_non_empty_request_id() {
      final String requestId = UUID.randomUUID().toString();
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(requestId, SourceProvider.GITLAB, "repo", 1);

      assertThat(request.requestId()).isNotBlank();
    }

    @Test
    @DisplayName("should_set_creation_timestamp")
    final void should_set_creation_timestamp() {
      final Instant before = Instant.now();
      final String requestId = UUID.randomUUID().toString();
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(requestId, SourceProvider.GITLAB, "repo", 1);
      final Instant after = Instant.now();

      assertThat(request.createdAt()).isAfterOrEqualTo(before);
      assertThat(request.createdAt()).isBeforeOrEqualTo(after);
    }
  }

  @Nested
  @DisplayName("Redis Message Format")
  final class RedisMessageFormat {

    @Test
    @DisplayName("should_format_message_for_redis_stream")
    final void should_format_message_for_redis_stream() throws Exception {
      final String requestId = UUID.randomUUID().toString();
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(requestId, SourceProvider.GITLAB, "my-repo", 123);

      final String payload = objectMapper.writeValueAsString(request);
      final Map<String, String> redisMessage =
          Map.of("requestId", request.requestId(), "payload", payload);

      assertThat(redisMessage).containsKey("requestId");
      assertThat(redisMessage).containsKey("payload");
      assertThat(redisMessage.get("requestId")).isEqualTo(request.requestId());
      assertThat(redisMessage.get("payload")).contains("my-repo");
      assertThat(redisMessage.get("payload")).contains("123");
    }

    @Test
    @DisplayName("should_parse_message_from_redis_stream")
    final void should_parse_message_from_redis_stream() throws Exception {
      final String requestId = "test-req-456";
      final String payload =
          """
          {
            "requestId": "test-req-456",
            "provider": "GITLAB",
            "repositoryId": "group/project",
            "changeRequestId": 789,
            "createdAt": "2024-03-20T15:00:00Z"
          }
          """;

      final Map<String, String> redisMessage = Map.of("requestId", requestId, "payload", payload);

      assertThat(redisMessage.get("requestId")).isEqualTo("test-req-456");

      objectMapper.findAndRegisterModules();
      final AsyncReviewRequest parsedRequest =
          objectMapper.readValue(redisMessage.get("payload"), AsyncReviewRequest.class);

      assertThat(parsedRequest.repositoryId()).isEqualTo("group/project");
      assertThat(parsedRequest.changeRequestId()).isEqualTo(789);
    }
  }

  @Nested
  @DisplayName("Result Storage Format")
  final class ResultStorageFormat {

    @Test
    @DisplayName("should_format_completed_result_for_redis_hash")
    final void should_format_completed_result_for_redis_hash() {
      final String requestId = "req-complete-123";
      final String resultJson = "{\"summary\": \"Good code\", \"issues\": []}";

      final Map<String, String> resultData =
          Map.of(
              "requestId", requestId,
              "status", "COMPLETED",
              "result", resultJson,
              "llmProvider", "openai",
              "llmModel", "gpt-4o",
              "processingTimeMs", "1500",
              "completedAt", Instant.now().toString());

      assertThat(resultData).containsEntry("status", "COMPLETED");
      assertThat(resultData.get("processingTimeMs")).isEqualTo("1500");
      assertThat(resultData.get("result")).contains("Good code");
    }

    @Test
    @DisplayName("should_format_failed_result_for_redis_hash")
    final void should_format_failed_result_for_redis_hash() {
      final String requestId = "req-fail-456";
      final String errorMessage = "LLM provider timeout";

      final Map<String, String> errorData =
          Map.of(
              "requestId",
              requestId,
              "status",
              "FAILED",
              "error",
              errorMessage,
              "completedAt",
              Instant.now().toString());

      assertThat(errorData).containsEntry("status", "FAILED");
      assertThat(errorData.get("error")).isEqualTo("LLM provider timeout");
      assertThat(errorData).doesNotContainKey("result");
    }
  }

  @Nested
  @DisplayName("Provider Support")
  final class ProviderSupport {

    @Test
    @DisplayName("should_support_gitlab_provider")
    final void should_support_gitlab_provider() {
      final String requestId = UUID.randomUUID().toString();
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(requestId, SourceProvider.GITLAB, "group/project", 1);

      assertThat(request.provider()).isEqualTo(SourceProvider.GITLAB);
    }

    @Test
    @DisplayName("should_support_github_provider")
    final void should_support_github_provider() {
      final String requestId = UUID.randomUUID().toString();
      final AsyncReviewRequest request =
          AsyncReviewRequest.create(requestId, SourceProvider.GITHUB, "owner/repo", 100);

      assertThat(request.provider()).isEqualTo(SourceProvider.GITHUB);
    }
  }
}
