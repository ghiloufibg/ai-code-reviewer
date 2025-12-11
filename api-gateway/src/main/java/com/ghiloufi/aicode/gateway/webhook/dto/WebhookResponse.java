package com.ghiloufi.aicode.gateway.webhook.dto;

public record WebhookResponse(String requestId, String status, String message) {

  public static WebhookResponse accepted(final String requestId) {
    return new WebhookResponse(requestId, "accepted", "Review request queued for processing");
  }

  public static WebhookResponse alreadyProcessed(final String requestId) {
    return new WebhookResponse(requestId, "already_processed", "Event was already processed");
  }
}
