package com.ghiloufi.aicode.agentworker.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.AgentTask;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.async.ReviewStatus;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentResultPublisher {

  private static final String RESULT_KEY_PREFIX = "review:result:";
  private static final Duration RESULT_TTL = Duration.ofHours(24);

  private final SCMPort scmPort;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public void publish(AgentTask task, ReviewResult reviewResult) {
    final var requestId = task.taskId();
    final var startTime = Instant.now();

    try {
      log.info(
          "Publishing review result for task {} to {} (PR #{})",
          requestId,
          task.repository().getDisplayName(),
          task.changeRequest().getNumber());

      publishToSCM(task, reviewResult);
      storeResult(requestId, reviewResult, startTime);

      log.info("Successfully published review result for task {}", requestId);

    } catch (Exception e) {
      log.error("Failed to publish review result for task {}", requestId, e);
      storeFailure(requestId, e.getMessage(), startTime);
      throw new IllegalStateException("Publishing failed: " + e.getMessage(), e);
    }
  }

  private void publishToSCM(AgentTask task, ReviewResult reviewResult) {
    final var repository = task.repository();
    final var changeRequest = task.changeRequest();

    scmPort.publishReview(repository, changeRequest, reviewResult).block();

    log.debug(
        "Published {} issues and {} notes to {} PR #{}",
        reviewResult.getIssues().size(),
        reviewResult.getNonBlockingNotes().size(),
        repository.getProvider(),
        changeRequest.getNumber());
  }

  private void storeResult(String requestId, ReviewResult reviewResult, Instant startTime) {
    try {
      final var resultKey = RESULT_KEY_PREFIX + requestId;
      final var processingTimeMs = Duration.between(startTime, Instant.now()).toMillis();

      final Map<String, String> resultData = new HashMap<>();
      resultData.put("status", ReviewStatus.COMPLETED.name());
      resultData.put("result", objectMapper.writeValueAsString(reviewResult));
      resultData.put("processingTimeMs", String.valueOf(processingTimeMs));
      resultData.put("completedAt", Instant.now().toString());

      redisTemplate.opsForHash().putAll(resultKey, resultData);
      redisTemplate.expire(resultKey, RESULT_TTL);

      log.debug("Stored result for {} with TTL {}", requestId, RESULT_TTL);

    } catch (Exception e) {
      log.warn("Failed to store result in Redis for {}: {}", requestId, e.getMessage());
    }
  }

  private void storeFailure(String requestId, String errorMessage, Instant startTime) {
    try {
      final var resultKey = RESULT_KEY_PREFIX + requestId;
      final var processingTimeMs = Duration.between(startTime, Instant.now()).toMillis();

      final Map<String, String> resultData = new HashMap<>();
      resultData.put("status", ReviewStatus.FAILED.name());
      resultData.put("error", errorMessage);
      resultData.put("processingTimeMs", String.valueOf(processingTimeMs));
      resultData.put("failedAt", Instant.now().toString());

      redisTemplate.opsForHash().putAll(resultKey, resultData);
      redisTemplate.expire(resultKey, RESULT_TTL);

    } catch (Exception e) {
      log.warn("Failed to store failure in Redis for {}: {}", requestId, e.getMessage());
    }
  }
}
