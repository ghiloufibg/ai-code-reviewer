package com.ghiloufi.aicode.gateway.webhook.exception;

import com.ghiloufi.aicode.gateway.webhook.dto.WebhookResponse;
import lombok.Getter;

@Getter
public final class AlreadyProcessedException extends RuntimeException {

  private final String idempotencyKey;
  private final WebhookResponse response;

  public AlreadyProcessedException(final String idempotencyKey) {
    super("Event with idempotency key '%s' was already processed".formatted(idempotencyKey));
    this.idempotencyKey = idempotencyKey;
    this.response = WebhookResponse.alreadyProcessed(idempotencyKey);
  }
}
