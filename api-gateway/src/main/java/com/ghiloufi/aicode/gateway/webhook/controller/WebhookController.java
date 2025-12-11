package com.ghiloufi.aicode.gateway.webhook.controller;

import com.ghiloufi.aicode.gateway.webhook.dto.WebhookErrorResponse;
import com.ghiloufi.aicode.gateway.webhook.dto.WebhookRequest;
import com.ghiloufi.aicode.gateway.webhook.dto.WebhookResponse;
import com.ghiloufi.aicode.gateway.webhook.exception.AlreadyProcessedException;
import com.ghiloufi.aicode.gateway.webhook.exception.WebhookValidationException;
import com.ghiloufi.aicode.gateway.webhook.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhooks")
public class WebhookController {

  private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

  private final WebhookService webhookService;

  @PostMapping
  public Mono<ResponseEntity<WebhookResponse>> handleWebhook(
      @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) final String idempotencyKey,
      @Valid @RequestBody final WebhookRequest request) {

    log.debug(
        "Received webhook request: provider={}, repo={}, pr={}, source={}",
        request.provider(),
        request.repositoryId(),
        request.changeRequestId(),
        request.triggerSource());

    return webhookService
        .processWebhook(request, idempotencyKey)
        .map(response -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response))
        .onErrorResume(
            AlreadyProcessedException.class,
            e -> Mono.just(ResponseEntity.ok().body(e.getResponse())));
  }

  @ExceptionHandler(WebhookValidationException.class)
  public ResponseEntity<WebhookErrorResponse> handleValidationError(
      final WebhookValidationException e) {
    log.warn("Webhook validation error: {}", e.getMessage());
    return ResponseEntity.badRequest().body(WebhookErrorResponse.validationError(e.getMessage()));
  }

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<WebhookErrorResponse> handleBindException(
      final WebExchangeBindException e) {
    final String message =
        e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(
                fieldError ->
                    "%s: %s".formatted(fieldError.getField(), fieldError.getDefaultMessage()))
            .orElse("Invalid request payload");

    log.warn("Webhook binding error: {}", message);
    return ResponseEntity.badRequest().body(WebhookErrorResponse.validationError(message));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<WebhookErrorResponse> handleIllegalArgument(
      final IllegalArgumentException e) {
    log.warn("Webhook illegal argument: {}", e.getMessage());
    return ResponseEntity.badRequest().body(WebhookErrorResponse.validationError(e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<WebhookErrorResponse> handleGenericError(final Exception e) {
    log.error("Unexpected error processing webhook", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(WebhookErrorResponse.internalError("An unexpected error occurred"));
  }
}
