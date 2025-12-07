package com.ghiloufi.aicode.llmworker.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import com.ghiloufi.aicode.llmworker.config.WorkerProperties;
import com.ghiloufi.aicode.llmworker.processor.ReviewProcessor;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@DisplayName("ReviewRequestConsumer Integration Tests")
@SuppressWarnings("deprecation")
final class ReviewRequestConsumerIntegrationTest {

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private StringRedisTemplate redisTemplate;
  private WorkerProperties workerProperties;
  private TestProcessor testProcessor;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    final LettuceConnectionFactory connectionFactory =
        new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    connectionFactory.afterPropertiesSet();

    redisTemplate = new StringRedisTemplate(connectionFactory);

    workerProperties =
        new WorkerProperties("test-workers", "test-worker-1", "test:review:requests", 10, 120);

    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    testProcessor = new TestProcessor();

    final String streamKey = workerProperties.getStreamKey();
    try {
      redisTemplate.delete(streamKey);
    } catch (final Exception ignored) {
    }
  }

  @Nested
  @DisplayName("Consumer Group Setup")
  final class ConsumerGroupSetup {

    @Test
    @DisplayName("should_create_consumer_group_on_init")
    final void should_create_consumer_group_on_init() {
      final ReviewRequestConsumer consumer =
          new ReviewRequestConsumer(redisTemplate, testProcessor, workerProperties, objectMapper);

      consumer.init();

      final var groupInfo = redisTemplate.opsForStream().groups(workerProperties.getStreamKey());
      assertThat(groupInfo).isNotNull();

      consumer.shutdown();
    }
  }

  @Nested
  @DisplayName("Message Processing")
  final class MessageProcessing {

    @Test
    @DisplayName("should_process_message_from_stream")
    final void should_process_message_from_stream() {
      final ReviewRequestConsumer consumer =
          new ReviewRequestConsumer(redisTemplate, testProcessor, workerProperties, objectMapper);

      consumer.init();

      final Map<String, String> messageData = new HashMap<>();
      messageData.put("requestId", "test-req-001");
      messageData.put(
          "payload",
          "{\"requestId\":\"test-req-001\",\"provider\":\"GITLAB\",\"repositoryId\":\"test/repo\",\"changeRequestId\":1,\"userPrompt\":\"Review this\",\"createdAt\":\"2024-01-01T00:00:00Z\"}");

      final RecordId recordId =
          redisTemplate.opsForStream().add(workerProperties.getStreamKey(), messageData);

      assertThat(recordId).isNotNull();

      consumer.consumeMessages();

      await()
          .atMost(Duration.ofSeconds(5))
          .untilAsserted(
              () -> {
                assertThat(testProcessor.getProcessedCount()).isGreaterThanOrEqualTo(1);
                assertThat(testProcessor.getProcessedRequestIds()).contains("test-req-001");
              });

      consumer.shutdown();
    }

    @Test
    @DisplayName("should_process_multiple_messages")
    final void should_process_multiple_messages() {
      final ReviewRequestConsumer consumer =
          new ReviewRequestConsumer(redisTemplate, testProcessor, workerProperties, objectMapper);

      consumer.init();

      for (int i = 0; i < 3; i++) {
        final Map<String, String> messageData = new HashMap<>();
        messageData.put("requestId", "batch-req-" + i);
        messageData.put(
            "payload",
            String.format(
                "{\"requestId\":\"batch-req-%d\",\"provider\":\"GITLAB\",\"repositoryId\":\"test/repo\",\"changeRequestId\":%d,\"userPrompt\":\"Review\",\"createdAt\":\"2024-01-01T00:00:00Z\"}",
                i, i));
        redisTemplate.opsForStream().add(workerProperties.getStreamKey(), messageData);
      }

      consumer.consumeMessages();

      await()
          .atMost(Duration.ofSeconds(10))
          .untilAsserted(
              () -> {
                assertThat(testProcessor.getProcessedCount()).isGreaterThanOrEqualTo(3);
              });

      consumer.shutdown();
    }
  }

  @Nested
  @DisplayName("Graceful Shutdown")
  final class GracefulShutdown {

    @Test
    @DisplayName("should_shutdown_gracefully")
    final void should_shutdown_gracefully() {
      final ReviewRequestConsumer consumer =
          new ReviewRequestConsumer(redisTemplate, testProcessor, workerProperties, objectMapper);

      consumer.init();
      consumer.shutdown();
    }
  }

  private static final class TestProcessor extends ReviewProcessor {
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final Map<String, String> processedRequests = new ConcurrentHashMap<>();

    TestProcessor() {
      super(null, null, null, null);
    }

    @Override
    public void process(final String requestId, final AsyncReviewRequest request) {
      processedCount.incrementAndGet();
      processedRequests.put(requestId, request != null ? request.requestId() : "null");
    }

    @Override
    public void process(
        final String requestId, final String requestPayload, final String userPrompt) {
      processedCount.incrementAndGet();
      processedRequests.put(requestId, requestPayload);
    }

    int getProcessedCount() {
      return processedCount.get();
    }

    java.util.Set<String> getProcessedRequestIds() {
      return processedRequests.keySet();
    }
  }
}
