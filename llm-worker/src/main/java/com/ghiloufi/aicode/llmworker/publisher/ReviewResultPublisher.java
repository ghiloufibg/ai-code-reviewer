package com.ghiloufi.aicode.llmworker.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewResultPublisher {

  private static final String RESULT_KEY_PREFIX = "review:results:";
  private static final String STATUS_CHANNEL_PREFIX = "review:status:";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public void publish(
      String requestId,
      ReviewResult result,
      String llmProvider,
      String llmModel,
      long processingTimeMs) {
    try {
      final Map<String, String> resultData = new HashMap<>();
      resultData.put("requestId", requestId);
      resultData.put("status", "COMPLETED");
      resultData.put("result", objectMapper.writeValueAsString(result));
      resultData.put("llmProvider", llmProvider);
      resultData.put("llmModel", llmModel);
      resultData.put("processingTimeMs", String.valueOf(processingTimeMs));
      resultData.put("completedAt", Instant.now().toString());

      final String resultKey = RESULT_KEY_PREFIX + requestId;
      redisTemplate.opsForHash().putAll(resultKey, resultData);

      redisTemplate.convertAndSend(STATUS_CHANNEL_PREFIX + requestId, "COMPLETED");

      log.info("Published result for request {} to {}", requestId, resultKey);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize result for request {}", requestId, e);
      publishError(requestId, "Serialization error: " + e.getMessage());
    }
  }

  public void publishError(String requestId, String errorMessage) {
    final Map<String, String> errorData = new HashMap<>();
    errorData.put("requestId", requestId);
    errorData.put("status", "FAILED");
    errorData.put("error", errorMessage);
    errorData.put("completedAt", Instant.now().toString());

    final String resultKey = RESULT_KEY_PREFIX + requestId;
    redisTemplate.opsForHash().putAll(resultKey, errorData);

    redisTemplate.convertAndSend(STATUS_CHANNEL_PREFIX + requestId, "FAILED");

    log.warn("Published error for request {} to {}", requestId, resultKey);
  }
}
