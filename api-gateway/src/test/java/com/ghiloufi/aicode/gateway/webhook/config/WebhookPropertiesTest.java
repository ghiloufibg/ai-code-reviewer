package com.ghiloufi.aicode.gateway.webhook.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class WebhookPropertiesTest {

  @Nested
  @DisplayName("hasApiKeys")
  final class HasApiKeysTests {

    @Test
    @DisplayName("should_return_true_when_api_keys_configured")
    final void should_return_true_when_api_keys_configured() {
      final WebhookProperties properties =
          new WebhookProperties(Set.of("key1", "key2"), Duration.ofHours(24), 60, Set.of(), true);

      assertThat(properties.hasApiKeys()).isTrue();
    }

    @Test
    @DisplayName("should_return_false_when_api_keys_empty")
    final void should_return_false_when_api_keys_empty() {
      final WebhookProperties properties =
          new WebhookProperties(Set.of(), Duration.ofHours(24), 60, Set.of(), true);

      assertThat(properties.hasApiKeys()).isFalse();
    }

    @Test
    @DisplayName("should_return_false_when_api_keys_null")
    final void should_return_false_when_api_keys_null() {
      final WebhookProperties properties =
          new WebhookProperties(null, Duration.ofHours(24), 60, Set.of(), true);

      assertThat(properties.hasApiKeys()).isFalse();
    }
  }

  @Nested
  @DisplayName("Configuration Values")
  final class ConfigurationValuesTests {

    @Test
    @DisplayName("should_return_configured_idempotency_ttl")
    final void should_return_configured_idempotency_ttl() {
      final Duration expectedTtl = Duration.ofHours(48);
      final WebhookProperties properties =
          new WebhookProperties(Set.of("key"), expectedTtl, 60, Set.of(), true);

      assertThat(properties.getIdempotencyTtl()).isEqualTo(expectedTtl);
    }

    @Test
    @DisplayName("should_return_configured_rate_limit")
    final void should_return_configured_rate_limit() {
      final int expectedRateLimit = 120;
      final WebhookProperties properties =
          new WebhookProperties(
              Set.of("key"), Duration.ofHours(24), expectedRateLimit, Set.of(), true);

      assertThat(properties.getRateLimitPerMinute()).isEqualTo(expectedRateLimit);
    }

    @Test
    @DisplayName("should_return_configured_ip_whitelist")
    final void should_return_configured_ip_whitelist() {
      final Set<String> expectedWhitelist = Set.of("192.168.1.0/24", "10.0.0.0/8");
      final WebhookProperties properties =
          new WebhookProperties(Set.of("key"), Duration.ofHours(24), 60, expectedWhitelist, true);

      assertThat(properties.getIpWhitelist())
          .containsExactlyInAnyOrderElementsOf(expectedWhitelist);
    }

    @Test
    @DisplayName("should_return_enabled_status")
    final void should_return_enabled_status() {
      final WebhookProperties enabledProperties =
          new WebhookProperties(Set.of("key"), Duration.ofHours(24), 60, Set.of(), true);

      final WebhookProperties disabledProperties =
          new WebhookProperties(Set.of("key"), Duration.ofHours(24), 60, Set.of(), false);

      assertThat(enabledProperties.isEnabled()).isTrue();
      assertThat(disabledProperties.isEnabled()).isFalse();
    }
  }
}
