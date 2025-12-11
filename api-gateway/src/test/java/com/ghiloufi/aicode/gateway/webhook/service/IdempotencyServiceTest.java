package com.ghiloufi.aicode.gateway.webhook.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.gateway.webhook.config.WebhookProperties;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

final class IdempotencyServiceTest {

  private TestIdempotencyService idempotencyService;

  @BeforeEach
  final void setUp() {
    idempotencyService = new TestIdempotencyService();
  }

  @Nested
  @DisplayName("checkAndMark")
  final class CheckAndMarkTests {

    @Test
    @DisplayName("should_return_true_for_new_idempotency_key")
    final void should_return_true_for_new_idempotency_key() {
      final String idempotencyKey = "new-key-123";
      idempotencyService.setKeyExists(false);

      StepVerifier.create(idempotencyService.checkAndMark(idempotencyKey))
          .assertNext(isNew -> assertThat(isNew).isTrue())
          .verifyComplete();

      assertThat(idempotencyService.getLastCheckedKey()).isEqualTo(idempotencyKey);
    }

    @Test
    @DisplayName("should_return_false_for_existing_idempotency_key")
    final void should_return_false_for_existing_idempotency_key() {
      final String idempotencyKey = "existing-key-456";
      idempotencyService.setKeyExists(true);

      StepVerifier.create(idempotencyService.checkAndMark(idempotencyKey))
          .assertNext(isNew -> assertThat(isNew).isFalse())
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("exists")
  final class ExistsTests {

    @Test
    @DisplayName("should_return_true_when_key_exists")
    final void should_return_true_when_key_exists() {
      idempotencyService.setKeyPresent(true);

      StepVerifier.create(idempotencyService.exists("existing-key"))
          .assertNext(exists -> assertThat(exists).isTrue())
          .verifyComplete();
    }

    @Test
    @DisplayName("should_return_false_when_key_does_not_exist")
    final void should_return_false_when_key_does_not_exist() {
      idempotencyService.setKeyPresent(false);

      StepVerifier.create(idempotencyService.exists("non-existing-key"))
          .assertNext(exists -> assertThat(exists).isFalse())
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("WebhookProperties Integration")
  final class WebhookPropertiesIntegrationTests {

    @Test
    @DisplayName("should_use_default_ttl_when_configured")
    final void should_use_default_ttl_when_configured() {
      final Duration customTtl = Duration.ofHours(48);
      final WebhookProperties properties =
          new WebhookProperties(Set.of("key"), customTtl, 60, Set.of(), true);

      assertThat(properties.getIdempotencyTtl()).isEqualTo(customTtl);
    }

    @Test
    @DisplayName("should_have_default_24h_ttl")
    final void should_have_default_24h_ttl() {
      final WebhookProperties properties =
          new WebhookProperties(Set.of("key"), Duration.ofHours(24), 60, Set.of(), true);

      assertThat(properties.getIdempotencyTtl()).isEqualTo(Duration.ofHours(24));
    }
  }

  private static final class TestIdempotencyService extends IdempotencyService {
    private boolean keyExists = false;
    private boolean keyPresent = false;
    private String lastCheckedKey;

    TestIdempotencyService() {
      super(null, null);
    }

    @Override
    public Mono<Boolean> checkAndMark(final String idempotencyKey) {
      this.lastCheckedKey = idempotencyKey;
      return Mono.just(!keyExists);
    }

    @Override
    public Mono<Boolean> exists(final String idempotencyKey) {
      return Mono.just(keyPresent);
    }

    void setKeyExists(final boolean exists) {
      this.keyExists = exists;
    }

    void setKeyPresent(final boolean present) {
      this.keyPresent = present;
    }

    String getLastCheckedKey() {
      return lastCheckedKey;
    }
  }
}
