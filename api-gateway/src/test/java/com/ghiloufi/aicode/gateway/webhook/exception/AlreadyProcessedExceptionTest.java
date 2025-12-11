package com.ghiloufi.aicode.gateway.webhook.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.gateway.webhook.dto.WebhookResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class AlreadyProcessedExceptionTest {

  @Nested
  @DisplayName("Exception Properties")
  final class ExceptionPropertiesTests {

    @Test
    @DisplayName("should_create_exception_with_idempotency_key")
    final void should_create_exception_with_idempotency_key() {
      final String idempotencyKey = "test-idempotency-key-123";

      final AlreadyProcessedException exception = new AlreadyProcessedException(idempotencyKey);

      assertThat(exception.getIdempotencyKey()).isEqualTo(idempotencyKey);
    }

    @Test
    @DisplayName("should_create_exception_with_descriptive_message")
    final void should_create_exception_with_descriptive_message() {
      final String idempotencyKey = "test-idempotency-key-456";

      final AlreadyProcessedException exception = new AlreadyProcessedException(idempotencyKey);

      assertThat(exception.getMessage())
          .isEqualTo("Event with idempotency key 'test-idempotency-key-456' was already processed");
    }

    @Test
    @DisplayName("should_create_already_processed_response")
    final void should_create_already_processed_response() {
      final String idempotencyKey = "test-key";

      final AlreadyProcessedException exception = new AlreadyProcessedException(idempotencyKey);
      final WebhookResponse response = exception.getResponse();

      assertThat(response.requestId()).isEqualTo(idempotencyKey);
      assertThat(response.status()).isEqualTo("already_processed");
      assertThat(response.message()).isEqualTo("Event was already processed");
    }
  }
}
