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

  private final ReactiveStringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final ReviewModeRouter reviewModeRouter;

  public Mono<RecordId> send(final AsyncReviewRequest request) {
    final ReviewModeRouter.StreamKey streamKey = reviewModeRouter.route(request);

    return Mono.fromCallable(() -> serializeRequest(request))
        .flatMap(
            payload -> {
              final var record =
                  StreamRecords.string(Map.of("requestId", request.requestId(), "payload", payload))
                      .withStreamKey(streamKey.getKey());

              return redisTemplate.opsForStream().add(record);
            })
        .doOnSuccess(
            recordId ->
                log.info(
                    "Published review request {} to stream {} with record {} (mode={})",
                    request.requestId(),
                    streamKey.getKey(),
                    recordId,
                    request.reviewMode()))
        .doOnError(
            error ->
                log.error(
                    "Failed to publish review request {} to stream {}",
                    request.requestId(),
                    streamKey.getKey(),
                    error));
  }

  private String serializeRequest(final AsyncReviewRequest request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize review request", e);
    }
  }
}
