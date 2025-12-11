package com.ghiloufi.aicode.gateway.controller;

import static com.ghiloufi.aicode.gateway.util.LogSanitizer.sanitize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import com.ghiloufi.aicode.core.domain.model.async.ReviewStatus;
import com.ghiloufi.aicode.gateway.async.ReviewRequestProducer;
import com.ghiloufi.aicode.gateway.dto.ReviewStatusResponse;
import com.ghiloufi.aicode.gateway.dto.ReviewSubmissionResponse;
import jakarta.validation.constraints.Positive;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/async-reviews")
@Validated
@RequiredArgsConstructor
public class AsyncReviewController {

  private static final String RESULT_KEY_PREFIX = "review:results:";

  private final ReviewRequestProducer producer;
  private final ReactiveStringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @PostMapping(
      value = "/{provider}/{repositoryId}/change-requests/{changeRequestId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<ReviewSubmissionResponse>> submitAsyncReview(
      @PathVariable final String provider,
      @PathVariable final String repositoryId,
      @PathVariable @Positive final int changeRequestId) {

    final SourceProvider sourceProvider = SourceProvider.fromString(provider);
    final String decodedRepositoryId = URLDecoder.decode(repositoryId, StandardCharsets.UTF_8);
    final String requestId = UUID.randomUUID().toString();

    log.info(
        "Submitting async review: requestId={}, provider={}, repository={}, changeRequest={}",
        requestId,
        sourceProvider,
        decodedRepositoryId,
        changeRequestId);

    final AsyncReviewRequest request =
        AsyncReviewRequest.create(requestId, sourceProvider, decodedRepositoryId, changeRequestId);

    return producer
        .send(request)
        .map(
            recordId -> {
              final String statusUrl = "/api/v1/async-reviews/" + requestId + "/status";
              final ReviewSubmissionResponse response =
                  ReviewSubmissionResponse.pending(requestId, statusUrl);

              log.info("Async review submitted: requestId={}, recordId={}", requestId, recordId);

              return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            })
        .doOnError(
            error -> log.error("Failed to submit async review: requestId={}", requestId, error))
        .onErrorResume(
            error ->
                Mono.just(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ReviewSubmissionResponse(requestId, ReviewStatus.FAILED, null))))
        .timeout(Duration.ofSeconds(10));
  }

  @GetMapping(value = "/{requestId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<ReviewStatusResponse>> getStatus(
      @PathVariable final String requestId) {

    log.debug("Checking status for request: {}", sanitize(requestId));

    final String resultKey = RESULT_KEY_PREFIX + requestId;

    return redisTemplate
        .opsForHash()
        .entries(resultKey)
        .collectMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString())
        .flatMap(
            resultData -> {
              if (resultData.isEmpty()) {
                return Mono.just(ResponseEntity.ok(ReviewStatusResponse.pending(requestId)));
              }

              final String status = resultData.get("status");
              if ("FAILED".equals(status)) {
                final String error = resultData.get("error");
                return Mono.just(ResponseEntity.ok(ReviewStatusResponse.failed(requestId, error)));
              }

              if ("COMPLETED".equals(status)) {
                return parseCompletedResult(requestId, resultData);
              }

              return Mono.just(ResponseEntity.ok(ReviewStatusResponse.processing(requestId)));
            })
        .doOnError(error -> log.error("Failed to get status for request: {}", requestId, error))
        .onErrorResume(
            error ->
                Mono.just(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ReviewStatusResponse.failed(requestId, "Failed to retrieve status"))))
        .timeout(Duration.ofSeconds(10));
  }

  @GetMapping(value = "/{requestId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<ReviewStatusResponse>> getResult(
      @PathVariable final String requestId) {

    log.debug("Getting result for request: {}", sanitize(requestId));

    final String resultKey = RESULT_KEY_PREFIX + requestId;

    return redisTemplate
        .opsForHash()
        .entries(resultKey)
        .collectMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString())
        .flatMap(
            resultData -> {
              if (resultData.isEmpty()) {
                return Mono.just(ResponseEntity.notFound().<ReviewStatusResponse>build());
              }

              final String status = resultData.get("status");
              if ("COMPLETED".equals(status)) {
                return parseCompletedResult(requestId, resultData);
              }

              if ("FAILED".equals(status)) {
                final String error = resultData.get("error");
                return Mono.just(ResponseEntity.ok(ReviewStatusResponse.failed(requestId, error)));
              }

              return Mono.just(ResponseEntity.ok(ReviewStatusResponse.processing(requestId)));
            })
        .doOnError(error -> log.error("Failed to get result for request: {}", requestId, error))
        .onErrorResume(
            error ->
                Mono.just(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ReviewStatusResponse.failed(requestId, "Failed to retrieve result"))))
        .timeout(Duration.ofSeconds(10));
  }

  private Mono<ResponseEntity<ReviewStatusResponse>> parseCompletedResult(
      final String requestId, final Map<String, String> resultData) {
    try {
      final String resultJson = resultData.get("result");
      final ReviewResult result = objectMapper.readValue(resultJson, ReviewResult.class);
      final long processingTimeMs = Long.parseLong(resultData.get("processingTimeMs"));

      return Mono.just(
          ResponseEntity.ok(ReviewStatusResponse.completed(requestId, result, processingTimeMs)));
    } catch (JsonProcessingException | NumberFormatException e) {
      log.error("Failed to parse result for request: {}", requestId, e);
      return Mono.just(
          ResponseEntity.ok(ReviewStatusResponse.failed(requestId, "Failed to parse result")));
    }
  }
}
