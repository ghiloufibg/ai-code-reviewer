package com.ghiloufi.aicode.llmworker.publisher;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("ReviewResultPublisher Tests")
final class ReviewResultPublisherTest {

  private TestRedisCapture redisCapture;
  private ObjectMapper objectMapper;
  private ReviewResultPublisher publisher;

  @BeforeEach
  void setUp() {
    redisCapture = new TestRedisCapture();
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    publisher = new TestableReviewResultPublisher(redisCapture, objectMapper);
  }

  @Nested
  @DisplayName("Publish with AsyncReviewRequest")
  final class PublishWithAsyncRequest {

    @Test
    @DisplayName("should_publish_successful_result_to_redis")
    void should_publish_successful_result_to_redis() {
      final AsyncReviewRequest request =
          new AsyncReviewRequest("req-123", SourceProvider.GITHUB, "owner/repo", 42, Instant.now());

      final ReviewResult result =
          ReviewResult.builder()
              .summary("Review summary")
              .issues(List.of())
              .nonBlockingNotes(List.of())
              .llmProvider("openai")
              .llmModel("gpt-4o")
              .build();

      publisher.publish("req-123", request, result, "openai", "gpt-4o", 1500);

      assertThat(redisCapture.getLastHashKey()).isEqualTo("review:results:req-123");
      assertThat(redisCapture.getLastHashData().get("requestId")).isEqualTo("req-123");
      assertThat(redisCapture.getLastHashData().get("status")).isEqualTo("COMPLETED");
      assertThat(redisCapture.getLastHashData().get("provider")).isEqualTo("GITHUB");
      assertThat(redisCapture.getLastHashData().get("repositoryId")).isEqualTo("owner/repo");
      assertThat(redisCapture.getLastHashData().get("changeRequestId")).isEqualTo("42");
      assertThat(redisCapture.getLastHashData().get("llmProvider")).isEqualTo("openai");
      assertThat(redisCapture.getLastHashData().get("llmModel")).isEqualTo("gpt-4o");
      assertThat(redisCapture.getLastHashData().get("processingTimeMs")).isEqualTo("1500");
    }

    @Test
    @DisplayName("should_publish_status_channel_message")
    void should_publish_status_channel_message() {
      final AsyncReviewRequest request =
          new AsyncReviewRequest(
              "req-456", SourceProvider.GITLAB, "group/project", 99, Instant.now());

      final ReviewResult result =
          ReviewResult.builder()
              .summary("Summary")
              .issues(List.of())
              .nonBlockingNotes(List.of())
              .build();

      publisher.publish("req-456", request, result, "anthropic", "claude-3", 2000);

      assertThat(redisCapture.getLastChannel()).isEqualTo("review:status:req-456");
      assertThat(redisCapture.getLastMessage()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("should_include_completed_at_timestamp")
    void should_include_completed_at_timestamp() {
      final AsyncReviewRequest request =
          new AsyncReviewRequest("req-789", SourceProvider.GITHUB, "test/repo", 1, Instant.now());

      final ReviewResult result =
          ReviewResult.builder()
              .summary("Test")
              .issues(List.of())
              .nonBlockingNotes(List.of())
              .build();

      publisher.publish("req-789", request, result, "openai", "gpt-4o", 500);

      assertThat(redisCapture.getLastHashData().get("completedAt")).isNotNull();
    }
  }

  @Nested
  @DisplayName("Publish with Legacy String Payload")
  @SuppressWarnings("deprecation")
  final class PublishWithLegacyPayload {

    @Test
    @DisplayName("should_publish_result_with_string_payload")
    void should_publish_result_with_string_payload() {
      final String requestPayload = "{\"key\": \"value\"}";

      final ReviewResult result =
          ReviewResult.builder()
              .summary("Legacy review")
              .issues(List.of())
              .nonBlockingNotes(List.of())
              .build();

      publisher.publish("legacy-req", requestPayload, result, "gemini", "gemini-pro", 3000);

      assertThat(redisCapture.getLastHashData().get("requestId")).isEqualTo("legacy-req");
      assertThat(redisCapture.getLastHashData().get("status")).isEqualTo("COMPLETED");
      assertThat(redisCapture.getLastHashData().get("request")).isEqualTo(requestPayload);
      assertThat(redisCapture.getLastHashData().get("llmProvider")).isEqualTo("gemini");
      assertThat(redisCapture.getLastHashData().get("llmModel")).isEqualTo("gemini-pro");
    }

    @Test
    @DisplayName("should_publish_status_for_legacy_request")
    void should_publish_status_for_legacy_request() {
      final ReviewResult result =
          ReviewResult.builder()
              .summary("Summary")
              .issues(List.of())
              .nonBlockingNotes(List.of())
              .build();

      publisher.publish("legacy-123", "{}", result, "ollama", "llama3", 1000);

      assertThat(redisCapture.getLastChannel()).isEqualTo("review:status:legacy-123");
      assertThat(redisCapture.getLastMessage()).isEqualTo("COMPLETED");
    }
  }

  @Nested
  @DisplayName("Publish Error")
  final class PublishError {

    @Test
    @DisplayName("should_publish_error_with_failed_status")
    void should_publish_error_with_failed_status() {
      publisher.publishError("err-req", "LLM timeout occurred");

      assertThat(redisCapture.getLastHashData().get("requestId")).isEqualTo("err-req");
      assertThat(redisCapture.getLastHashData().get("status")).isEqualTo("FAILED");
      assertThat(redisCapture.getLastHashData().get("error")).isEqualTo("LLM timeout occurred");
    }

    @Test
    @DisplayName("should_publish_failed_status_to_channel")
    void should_publish_failed_status_to_channel() {
      publisher.publishError("err-456", "Connection failed");

      assertThat(redisCapture.getLastChannel()).isEqualTo("review:status:err-456");
      assertThat(redisCapture.getLastMessage()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("should_include_completed_at_in_error")
    void should_include_completed_at_in_error() {
      publisher.publishError("err-789", "Error message");

      assertThat(redisCapture.getLastHashData().get("completedAt")).isNotNull();
    }
  }

  @Nested
  @DisplayName("Serialization Error Handling")
  final class SerializationErrorHandling {

    @Test
    @DisplayName("should_publish_error_when_serialization_fails")
    void should_publish_error_when_serialization_fails() {
      final ObjectMapper failingMapper = new FailingObjectMapper();
      final ReviewResultPublisher failingPublisher =
          new TestableReviewResultPublisher(redisCapture, failingMapper);

      final AsyncReviewRequest request =
          new AsyncReviewRequest("ser-err", SourceProvider.GITHUB, "test/repo", 1, Instant.now());

      final ReviewResult result =
          ReviewResult.builder()
              .summary("Test")
              .issues(List.of())
              .nonBlockingNotes(List.of())
              .build();

      failingPublisher.publish("ser-err", request, result, "openai", "gpt-4o", 100);

      assertThat(redisCapture.getLastHashData().get("status")).isEqualTo("FAILED");
      assertThat(redisCapture.getLastHashData().get("error")).contains("Serialization error");
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("should_publish_error_when_legacy_serialization_fails")
    void should_publish_error_when_legacy_serialization_fails() {
      final ObjectMapper failingMapper = new FailingObjectMapper();
      final ReviewResultPublisher failingPublisher =
          new TestableReviewResultPublisher(redisCapture, failingMapper);

      final ReviewResult result =
          ReviewResult.builder()
              .summary("Test")
              .issues(List.of())
              .nonBlockingNotes(List.of())
              .build();

      failingPublisher.publish("ser-err-2", "{}", result, "openai", "gpt-4o", 100);

      assertThat(redisCapture.getLastHashData().get("status")).isEqualTo("FAILED");
    }
  }

  private static final class TestRedisCapture {
    private final Map<String, Map<String, String>> hashStorage = new ConcurrentHashMap<>();
    private final Map<String, String> channelMessages = new ConcurrentHashMap<>();
    private String lastHashKey;
    private String lastChannel;
    private String lastMessage;

    void putAllHash(final String key, final Map<?, ?> data) {
      final Map<String, String> stringMap = new HashMap<>();
      data.forEach((k, v) -> stringMap.put(k.toString(), v.toString()));
      hashStorage.put(key, stringMap);
      lastHashKey = key;
    }

    void sendMessage(final String channel, final String message) {
      channelMessages.put(channel, message);
      lastChannel = channel;
      lastMessage = message;
    }

    String getLastHashKey() {
      return lastHashKey;
    }

    Map<String, String> getLastHashData() {
      return hashStorage.get(lastHashKey);
    }

    String getLastChannel() {
      return lastChannel;
    }

    String getLastMessage() {
      return lastMessage;
    }
  }

  private static final class TestableReviewResultPublisher extends ReviewResultPublisher {
    private final TestRedisCapture capture;
    private final ObjectMapper mapper;

    TestableReviewResultPublisher(final TestRedisCapture capture, final ObjectMapper objectMapper) {
      super(new NoOpStringRedisTemplate(), objectMapper);
      this.capture = capture;
      this.mapper = objectMapper;
    }

    @Override
    public void publish(
        final String requestId,
        final AsyncReviewRequest request,
        final ReviewResult result,
        final String llmProvider,
        final String llmModel,
        final long processingTimeMs) {
      try {
        final Map<String, String> resultData = new HashMap<>();
        resultData.put("requestId", requestId);
        resultData.put("status", "COMPLETED");
        resultData.put("request", mapper.writeValueAsString(request));
        resultData.put("result", mapper.writeValueAsString(result));
        resultData.put("provider", request.provider().name());
        resultData.put("repositoryId", request.repositoryId());
        resultData.put("changeRequestId", String.valueOf(request.changeRequestId()));
        resultData.put("llmProvider", llmProvider);
        resultData.put("llmModel", llmModel);
        resultData.put("processingTimeMs", String.valueOf(processingTimeMs));
        resultData.put("completedAt", Instant.now().toString());

        capture.putAllHash("review:results:" + requestId, resultData);
        capture.sendMessage("review:status:" + requestId, "COMPLETED");
      } catch (final JsonProcessingException e) {
        publishError(requestId, "Serialization error: " + e.getMessage());
      }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void publish(
        final String requestId,
        final String requestPayload,
        final ReviewResult result,
        final String llmProvider,
        final String llmModel,
        final long processingTimeMs) {
      try {
        final Map<String, String> resultData = new HashMap<>();
        resultData.put("requestId", requestId);
        resultData.put("status", "COMPLETED");
        resultData.put("request", requestPayload);
        resultData.put("result", mapper.writeValueAsString(result));
        resultData.put("llmProvider", llmProvider);
        resultData.put("llmModel", llmModel);
        resultData.put("processingTimeMs", String.valueOf(processingTimeMs));
        resultData.put("completedAt", Instant.now().toString());

        capture.putAllHash("review:results:" + requestId, resultData);
        capture.sendMessage("review:status:" + requestId, "COMPLETED");
      } catch (final JsonProcessingException e) {
        publishError(requestId, "Serialization error: " + e.getMessage());
      }
    }

    @Override
    public void publishError(final String requestId, final String errorMessage) {
      final Map<String, String> errorData = new HashMap<>();
      errorData.put("requestId", requestId);
      errorData.put("status", "FAILED");
      errorData.put("error", errorMessage);
      errorData.put("completedAt", Instant.now().toString());

      capture.putAllHash("review:results:" + requestId, errorData);
      capture.sendMessage("review:status:" + requestId, "FAILED");
    }
  }

  private static final class NoOpStringRedisTemplate extends StringRedisTemplate {
    NoOpStringRedisTemplate() {
      super();
    }

    @Override
    public void setConnectionFactory(final RedisConnectionFactory connectionFactory) {
      // No-op
    }
  }

  private static final class FailingObjectMapper extends ObjectMapper {
    @Override
    public String writeValueAsString(final Object value) throws JsonProcessingException {
      throw new JsonProcessingException("Simulated serialization failure") {};
    }
  }
}
