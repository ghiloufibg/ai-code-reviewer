package com.ghiloufi.aicode.gateway.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewRequestProducer {

  private static final String STREAM_KEY = "review:requests";

  private final ReactiveStringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public Mono<RecordId> send(AsyncReviewRequest request) {
    return Mono.fromCallable(() -> serializeRequest(request))
        .flatMap(
            payload -> {
              final var record =
                  StreamRecords.string(Map.of("requestId", request.requestId(), "payload", payload))
                      .withStreamKey(STREAM_KEY);

              return redisTemplate.opsForStream().add(record);
            })
        .doOnSuccess(
            recordId ->
                log.info(
                    "Published review request {} to stream {} with record {}",
                    request.requestId(),
                    STREAM_KEY,
                    recordId))
        .doOnError(
            error ->
                log.error(
                    "Failed to publish review request {} to stream", request.requestId(), error));
  }

  private String serializeRequest(AsyncReviewRequest request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize review request", e);
    }
  }
}
