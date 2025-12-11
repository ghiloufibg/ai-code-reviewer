package com.ghiloufi.aicode.gateway.webhook.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class WebhookResponseTest {

  @Nested
  @DisplayName("Factory Methods")
  final class FactoryMethodTests {

    @Test
    @DisplayName("should_create_accepted_response_with_correct_values")
    final void should_create_accepted_response_with_correct_values() {
      final String requestId = "test-request-id-123";

      final WebhookResponse response = WebhookResponse.accepted(requestId);

      assertThat(response.requestId()).isEqualTo(requestId);
      assertThat(response.status()).isEqualTo("accepted");
      assertThat(response.message()).isEqualTo("Review request queued for processing");
    }

    @Test
    @DisplayName("should_create_already_processed_response_with_correct_values")
    final void should_create_already_processed_response_with_correct_values() {
      final String requestId = "duplicate-request-id-456";

      final WebhookResponse response = WebhookResponse.alreadyProcessed(requestId);

      assertThat(response.requestId()).isEqualTo(requestId);
      assertThat(response.status()).isEqualTo("already_processed");
      assertThat(response.message()).isEqualTo("Event was already processed");
    }
  }

  @Nested
  @DisplayName("Record Properties")
  final class RecordPropertiesTests {

    @Test
    @DisplayName("should_create_response_with_custom_values")
    final void should_create_response_with_custom_values() {
      final WebhookResponse response =
          new WebhookResponse("custom-id", "custom-status", "custom-message");

      assertThat(response.requestId()).isEqualTo("custom-id");
      assertThat(response.status()).isEqualTo("custom-status");
      assertThat(response.message()).isEqualTo("custom-message");
    }
  }
}
