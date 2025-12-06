package com.ghiloufi.aicode.gateway.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import com.ghiloufi.aicode.core.domain.port.input.ReviewManagementUseCase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "async.review.consumer.enabled", havingValue = "true")
public class ReviewResultConsumer {

  private static final String RESULT_KEY_PREFIX = "review:results:";
  private static final String STATUS_CHANNEL_PREFIX = "review:status:";
  private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

  private final ReactiveStringRedisTemplate redisTemplate;
  private final ReviewManagementUseCase reviewManagementUseCase;
  private final ObjectMapper objectMapper;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private Disposable subscription;

  @PostConstruct
  public void start() {
    if (running.compareAndSet(false, true)) {
      log.info("Starting ReviewResultConsumer");
      startListening();
    }
  }

  @PreDestroy
  public void stop() {
    if (running.compareAndSet(true, false)) {
      log.info("Stopping ReviewResultConsumer");
      if (subscription != null && !subscription.isDisposed()) {
        subscription.dispose();
      }
    }
  }

  private void startListening() {
    subscription =
        redisTemplate
            .listenToPattern(STATUS_CHANNEL_PREFIX + "*")
            .subscribeOn(Schedulers.boundedElastic())
            .filter(message -> "COMPLETED".equals(message.getMessage()))
            .flatMap(
                message -> {
                  final String channel = message.getChannel();
                  final String requestId = channel.replace(STATUS_CHANNEL_PREFIX, "");

                  log.info("Received completion notification for request: {}", requestId);

                  return processCompletedReview(requestId);
                })
            .doOnError(error -> log.error("Error in ReviewResultConsumer", error))
            .retry()
            .subscribe();
  }

  private Mono<Void> processCompletedReview(String requestId) {
    final String resultKey = RESULT_KEY_PREFIX + requestId;

    return redisTemplate
        .opsForHash()
        .entries(resultKey)
        .collectMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString())
        .flatMap(resultData -> publishToScm(requestId, resultData))
        .doOnSuccess(v -> log.info("Processed completed review: {}", requestId))
        .doOnError(error -> log.error("Failed to process completed review: {}", requestId, error))
        .onErrorResume(error -> Mono.empty());
  }

  private Mono<Void> publishToScm(String requestId, Map<String, String> resultData) {
    try {
      final String status = resultData.get("status");
      if (!"COMPLETED".equals(status)) {
        log.warn("Review {} is not completed, status: {}", requestId, status);
        return Mono.empty();
      }

      final String resultJson = resultData.get("result");
      final ReviewResult result = objectMapper.readValue(resultJson, ReviewResult.class);

      final String requestJson = resultData.get("request");
      if (requestJson == null) {
        log.warn("No request data found for review: {}", requestId);
        return Mono.empty();
      }

      final AsyncReviewRequest request =
          objectMapper.readValue(requestJson, AsyncReviewRequest.class);

      return reviewManagementUseCase
          .publishReviewFromAsync(
              request.provider(), request.repositoryId(), request.changeRequestId(), result)
          .doOnSuccess(v -> log.info("Published async review to SCM: {}", requestId))
          .doOnError(
              error -> log.error("Failed to publish async review to SCM: {}", requestId, error));

    } catch (JsonProcessingException e) {
      log.error("Failed to parse result data for review: {}", requestId, e);
      return Mono.empty();
    }
  }
}
