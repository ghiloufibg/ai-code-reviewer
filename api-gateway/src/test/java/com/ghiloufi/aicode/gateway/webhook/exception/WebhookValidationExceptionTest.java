package com.ghiloufi.aicode.gateway.webhook.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class WebhookValidationExceptionTest {

  @Nested
  @DisplayName("Exception Creation")
  final class ExceptionCreationTests {

    @Test
    @DisplayName("should_create_exception_with_message")
    final void should_create_exception_with_message() {
      final String message = "Invalid provider value";

      final WebhookValidationException exception = new WebhookValidationException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("should_create_exception_with_message_and_cause")
    final void should_create_exception_with_message_and_cause() {
      final String message = "Failed to parse request";
      final Throwable cause = new IllegalArgumentException("Invalid JSON");

      final WebhookValidationException exception = new WebhookValidationException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
    }
  }
}
