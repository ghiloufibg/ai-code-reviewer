package com.ghiloufi.aicode.llmworker.consumer;

import com.ghiloufi.aicode.llmworker.config.WorkerProperties;
import com.ghiloufi.aicode.llmworker.processor.ReviewProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReviewRequestConsumer {

  private static final String PAYLOAD_FIELD = "payload";
  private static final String REQUEST_ID_FIELD = "requestId";

  private final StringRedisTemplate redisTemplate;
  private final ReviewProcessor processor;
  private final WorkerProperties workerProperties;
  private final ExecutorService executor;

  public ReviewRequestConsumer(
      StringRedisTemplate redisTemplate,
      ReviewProcessor processor,
      WorkerProperties workerProperties) {
    this.redisTemplate = redisTemplate;
    this.processor = processor;
    this.workerProperties = workerProperties;
    this.executor = Executors.newVirtualThreadPerTaskExecutor();
  }

  @PostConstruct
  public void init() {
    createConsumerGroupIfNotExists();
    log.info(
        "ReviewRequestConsumer initialized: group={}, consumer={}, stream={}",
        workerProperties.getConsumerGroup(),
        workerProperties.getConsumerId(),
        workerProperties.getStreamKey());
  }

  @PreDestroy
  public void shutdown() {
    log.info("Shutting down ReviewRequestConsumer");
    executor.shutdown();
    try {
      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private void createConsumerGroupIfNotExists() {
    try {
      redisTemplate
          .opsForStream()
          .createGroup(workerProperties.getStreamKey(), workerProperties.getConsumerGroup());
      log.info("Created consumer group: {}", workerProperties.getConsumerGroup());
    } catch (Exception e) {
      log.debug("Consumer group already exists: {}", workerProperties.getConsumerGroup());
    }
  }

  @Scheduled(fixedDelay = 100)
  public void consumeMessages() {
    try {
      final List<MapRecord<String, Object, Object>> messages =
          redisTemplate
              .opsForStream()
              .read(
                  Consumer.from(
                      workerProperties.getConsumerGroup(), workerProperties.getConsumerId()),
                  StreamReadOptions.empty()
                      .count(workerProperties.getBatchSize())
                      .block(Duration.ofSeconds(5)),
                  StreamOffset.create(workerProperties.getStreamKey(), ReadOffset.lastConsumed()));

      if (messages == null || messages.isEmpty()) {
        return;
      }

      log.debug("Received {} messages from stream", messages.size());

      for (final MapRecord<String, Object, Object> message : messages) {
        executor.submit(() -> processMessage(message));
      }
    } catch (Exception e) {
      log.error("Error consuming messages from stream", e);
    }
  }

  private void processMessage(MapRecord<String, Object, Object> message) {
    final String messageId = message.getId().getValue();
    try {
      final String requestId = extractRequestId(message);
      final String payload = extractPayload(message);

      log.info(
          "Processing message {} for request {} on {}",
          messageId,
          requestId,
          Thread.currentThread());

      processor.process(requestId, payload);

      redisTemplate
          .opsForStream()
          .acknowledge(
              workerProperties.getStreamKey(),
              workerProperties.getConsumerGroup(),
              message.getId());

      log.debug("Acknowledged message: {}", messageId);

    } catch (Exception e) {
      log.error("Failed to process message: {}", messageId, e);
    }
  }

  private String extractRequestId(MapRecord<String, Object, Object> message) {
    final Object requestId = message.getValue().get(REQUEST_ID_FIELD);
    if (requestId == null) {
      throw new IllegalArgumentException("Message missing requestId field");
    }
    return requestId.toString();
  }

  private String extractPayload(MapRecord<String, Object, Object> message) {
    final Object payload = message.getValue().get(PAYLOAD_FIELD);
    if (payload == null) {
      throw new IllegalArgumentException("Message missing payload field");
    }
    return payload.toString();
  }
}
