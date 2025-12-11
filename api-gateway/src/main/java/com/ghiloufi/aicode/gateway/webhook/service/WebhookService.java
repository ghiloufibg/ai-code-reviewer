package com.ghiloufi.aicode.gateway.webhook.service;

import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import com.ghiloufi.aicode.gateway.async.ReviewRequestProducer;
import com.ghiloufi.aicode.gateway.webhook.dto.WebhookRequest;
import com.ghiloufi.aicode.gateway.webhook.dto.WebhookResponse;
import com.ghiloufi.aicode.gateway.webhook.exception.AlreadyProcessedException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

  private final ReviewRequestProducer reviewRequestProducer;
  private final IdempotencyService idempotencyService;

  public Mono<WebhookResponse> processWebhook(
      final WebhookRequest request, final String idempotencyKey) {
    final String effectiveKey = resolveIdempotencyKey(request, idempotencyKey);

    log.info(
        "Processing webhook request for {}/{} PR#{} with idempotency key: {}",
        request.provider(),
        request.repositoryId(),
        request.changeRequestId(),
        effectiveKey);

    return idempotencyService
        .checkAndMark(effectiveKey)
        .flatMap(
            isNew -> {
              if (Boolean.FALSE.equals(isNew)) {
                log.info("Webhook event already processed: {}", effectiveKey);
                return Mono.error(new AlreadyProcessedException(effectiveKey));
              }
              return queueReviewRequest(request, effectiveKey);
            });
  }

  private String resolveIdempotencyKey(final WebhookRequest request, final String providedKey) {
    if (providedKey != null && !providedKey.isBlank()) {
      return providedKey;
    }
    return "%s:%d".formatted(request.repositoryId(), request.changeRequestId());
  }

  private Mono<WebhookResponse> queueReviewRequest(
      final WebhookRequest request, final String eventId) {
    final String requestId = UUID.randomUUID().toString();
    final AsyncReviewRequest reviewRequest = mapToReviewRequest(request, requestId);

    return reviewRequestProducer
        .send(reviewRequest)
        .doOnSuccess(
            recordId ->
                log.info(
                    "Webhook request queued successfully: requestId={}, recordId={}",
                    requestId,
                    recordId))
        .doOnError(error -> log.error("Failed to queue webhook request: {}", requestId, error))
        .thenReturn(WebhookResponse.accepted(requestId));
  }

  private AsyncReviewRequest mapToReviewRequest(
      final WebhookRequest request, final String requestId) {
    final SourceProvider provider = SourceProvider.fromString(request.provider());
    return AsyncReviewRequest.create(
        requestId, provider, request.repositoryId(), request.changeRequestId());
  }
}
