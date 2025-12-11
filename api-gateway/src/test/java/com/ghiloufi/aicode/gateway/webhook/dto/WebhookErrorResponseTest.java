package com.ghiloufi.aicode.gateway.webhook.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class WebhookErrorResponseTest {

  @Nested
  @DisplayName("Factory Methods")
  final class FactoryMethodTests {

    @Test
    @DisplayName("should_create_validation_error_response")
    final void should_create_validation_error_response() {
      final String errorMessage = "Provider is required";

      final WebhookErrorResponse response = WebhookErrorResponse.validationError(errorMessage);

      assertThat(response.error()).isEqualTo("validation_error");
      assertThat(response.message()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("should_create_internal_error_response")
    final void should_create_internal_error_response() {
      final String errorMessage = "An unexpected error occurred";

      final WebhookErrorResponse response = WebhookErrorResponse.internalError(errorMessage);

      assertThat(response.error()).isEqualTo("internal_error");
      assertThat(response.message()).isEqualTo(errorMessage);
    }
  }

  @Nested
  @DisplayName("Record Properties")
  final class RecordPropertiesTests {

    @Test
    @DisplayName("should_create_response_with_custom_values")
    final void should_create_response_with_custom_values() {
      final WebhookErrorResponse response =
          new WebhookErrorResponse("custom_error", "custom message");

      assertThat(response.error()).isEqualTo("custom_error");
      assertThat(response.message()).isEqualTo("custom message");
    }
  }
}
