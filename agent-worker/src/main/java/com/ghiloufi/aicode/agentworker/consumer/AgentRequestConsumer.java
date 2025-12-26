package com.ghiloufi.aicode.agentworker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.agentworker.agent.CodeReviewAgent;
import com.ghiloufi.aicode.agentworker.config.AgentWorkerProperties;
import com.ghiloufi.aicode.core.domain.model.AgentConfiguration;
import com.ghiloufi.aicode.core.domain.model.AgentTask;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
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
public class AgentRequestConsumer {

  private static final String PAYLOAD_FIELD = "payload";
  private static final String REQUEST_ID_FIELD = "requestId";

  private final StringRedisTemplate redisTemplate;
  private final CodeReviewAgent agent;
  private final AgentWorkerProperties properties;
  private final ObjectMapper objectMapper;
  private final ExecutorService executor;

  public AgentRequestConsumer(
      final StringRedisTemplate redisTemplate,
      final CodeReviewAgent agent,
      final AgentWorkerProperties properties,
      final ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.agent = agent;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.executor = Executors.newVirtualThreadPerTaskExecutor();
  }

  @PostConstruct
  public void init() {
    createConsumerGroupIfNotExists();
    log.info(
        "AgentRequestConsumer initialized: group={}, consumer={}, stream={}",
        properties.getConsumer().getConsumerGroup(),
        properties.getConsumer().getConsumerId(),
        properties.getConsumer().getStreamKey());
  }

  @PreDestroy
  public void shutdown() {
    log.info("Shutting down AgentRequestConsumer");
    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private void createConsumerGroupIfNotExists() {
    final var streamKey = properties.getConsumer().getStreamKey();
    final var consumerGroup = properties.getConsumer().getConsumerGroup();
    try {
      redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), consumerGroup);
      log.info("Created consumer group '{}' on stream '{}'", consumerGroup, streamKey);
    } catch (Exception e) {
      if (isBusyGroupError(e)) {
        log.debug("Consumer group '{}' already exists on stream '{}'", consumerGroup, streamKey);
      } else {
        log.error("Failed to create consumer group '{}': {}", consumerGroup, e.getMessage());
        throw new IllegalStateException("Cannot initialize Redis consumer group", e);
      }
    }
  }

  private boolean isBusyGroupError(Exception e) {
    Throwable cause = e.getCause();
    return cause != null && cause.getMessage() != null && cause.getMessage().contains("BUSYGROUP");
  }

  @Scheduled(fixedDelay = 500)
  public void consumeMessages() {
    try {
      final List<MapRecord<String, Object, Object>> messages =
          redisTemplate
              .opsForStream()
              .read(
                  Consumer.from(
                      properties.getConsumer().getConsumerGroup(),
                      properties.getConsumer().getConsumerId()),
                  StreamReadOptions.empty()
                      .count(properties.getConsumer().getBatchSize())
                      .block(Duration.ofSeconds(5)),
                  StreamOffset.create(
                      properties.getConsumer().getStreamKey(), ReadOffset.lastConsumed()));

      if (messages == null || messages.isEmpty()) {
        return;
      }

      log.debug("Received {} agent request messages from stream", messages.size());

      for (final MapRecord<String, Object, Object> message : messages) {
        executor.submit(() -> processMessage(message));
      }
    } catch (Exception e) {
      log.error("Error consuming messages from stream", e);
    }
  }

  private void processMessage(final MapRecord<String, Object, Object> message) {
    final var messageId = message.getId().getValue();
    try {
      final var requestId = extractRequestId(message);
      final var payload = extractPayload(message);

      log.info(
          "Processing agent request {} (message {}) on {}",
          requestId,
          messageId,
          Thread.currentThread());

      final var request = objectMapper.readValue(payload, AsyncReviewRequest.class);
      log.debug(
          "Parsed agent review request: {} for {} PR #{}",
          request.requestId(),
          request.provider(),
          request.changeRequestId());

      final var task = buildAgentTask(request);
      final var completedTask = agent.execute(task);

      log.info(
          "Agent task {} completed with status: {}",
          completedTask.taskId(),
          completedTask.currentStatus());

      redisTemplate
          .opsForStream()
          .acknowledge(
              properties.getConsumer().getStreamKey(),
              properties.getConsumer().getConsumerGroup(),
              message.getId());

      log.debug("Acknowledged message: {}", messageId);

    } catch (final Exception e) {
      log.error("Failed to process agent request message: {}", messageId, e);
    }
  }

  private AgentTask buildAgentTask(AsyncReviewRequest request) {
    final var repository = RepositoryIdentifier.create(request.provider(), request.repositoryId());

    final var changeRequest =
        ChangeRequestIdentifier.create(request.provider(), request.changeRequestId());

    final var aggregationConfig =
        new AgentConfiguration.AggregationConfig(
            properties.getAggregation().getDeduplication().isEnabled(),
            properties.getAggregation().getDeduplication().getSimilarityThreshold(),
            properties.getAggregation().getFiltering().getMinConfidence(),
            properties.getAggregation().getFiltering().getMaxIssuesPerFile());

    final var configuration =
        new AgentConfiguration(AgentConfiguration.DockerConfig.defaults(), aggregationConfig);

    return AgentTask.create(repository, changeRequest, configuration, request.requestId());
  }

  private String extractRequestId(MapRecord<String, Object, Object> message) {
    final var requestId = message.getValue().get(REQUEST_ID_FIELD);
    if (requestId == null) {
      throw new IllegalArgumentException("Message missing requestId field");
    }
    return requestId.toString();
  }

  private String extractPayload(MapRecord<String, Object, Object> message) {
    final var payload = message.getValue().get(PAYLOAD_FIELD);
    if (payload == null) {
      throw new IllegalArgumentException("Message missing payload field");
    }
    return payload.toString();
  }
}
