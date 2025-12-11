package com.ghiloufi.aicode.gateway.webhook.dto;

public record WebhookErrorResponse(String error, String message) {

  public static WebhookErrorResponse validationError(final String message) {
    return new WebhookErrorResponse("validation_error", message);
  }

  public static WebhookErrorResponse internalError(final String message) {
    return new WebhookErrorResponse("internal_error", message);
  }
}
