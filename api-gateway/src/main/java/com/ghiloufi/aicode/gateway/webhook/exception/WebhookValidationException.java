package com.ghiloufi.aicode.gateway.webhook.exception;

public final class WebhookValidationException extends RuntimeException {

  public WebhookValidationException(final String message) {
    super(message);
  }

  public WebhookValidationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
