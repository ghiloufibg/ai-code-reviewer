package com.ghiloufi.aicode.gateway.webhook.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.gateway.webhook.config.WebhookProperties;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

final class ApiKeyAuthenticationFilterTest {

  private static final String VALID_API_KEY = "test-api-key-12345678901234567890";
  private static final String ANOTHER_VALID_API_KEY = "another-api-key-12345678901234567890";
  private static final String INVALID_API_KEY = "invalid-key";

  @Nested
  @DisplayName("Non-Webhook Requests")
  final class NonWebhookRequestTests {

    @Test
    @DisplayName("should_pass_through_non_webhook_requests")
    final void should_pass_through_non_webhook_requests() {
      final WebhookProperties properties = createEnabledProperties(Set.of(VALID_API_KEY));
      final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(properties);

      final MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/reviews").build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(chain.wasFilterCalled()).isTrue();
    }

    @Test
    @DisplayName("should_not_require_api_key_for_other_endpoints")
    final void should_not_require_api_key_for_other_endpoints() {
      final WebhookProperties properties = createEnabledProperties(Set.of(VALID_API_KEY));
      final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(properties);

      final MockServerHttpRequest request =
          MockServerHttpRequest.post("/api/v1/reviews/gitlab/test/change-requests/1/review")
              .build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(chain.wasFilterCalled()).isTrue();
    }
  }

  @Nested
  @DisplayName("Webhook Authentication")
  final class WebhookAuthenticationTests {

    @Test
    @DisplayName("should_authenticate_with_valid_api_key")
    final void should_authenticate_with_valid_api_key() {
      final WebhookProperties properties = createEnabledProperties(Set.of(VALID_API_KEY));
      final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(properties);

      final MockServerHttpRequest request =
          MockServerHttpRequest.post("/webhooks").header("X-API-Key", VALID_API_KEY).build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(chain.wasFilterCalled()).isTrue();
    }

    @Test
    @DisplayName("should_authenticate_with_any_valid_api_key_from_set")
    final void should_authenticate_with_any_valid_api_key_from_set() {
      final WebhookProperties properties =
          createEnabledProperties(Set.of(VALID_API_KEY, ANOTHER_VALID_API_KEY));
      final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(properties);

      final MockServerHttpRequest request =
          MockServerHttpRequest.post("/webhooks")
              .header("X-API-Key", ANOTHER_VALID_API_KEY)
              .build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(chain.wasFilterCalled()).isTrue();
    }

    @Test
    @DisplayName("should_reject_missing_api_key")
    final void should_reject_missing_api_key() {
      final WebhookProperties properties = createEnabledProperties(Set.of(VALID_API_KEY));
      final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(properties);

      final MockServerHttpRequest request = MockServerHttpRequest.post("/webhooks").build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(chain.wasFilterCalled()).isFalse();
      assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("should_reject_blank_api_key")
    final void should_reject_blank_api_key() {
      final WebhookProperties properties = createEnabledProperties(Set.of(VALID_API_KEY));
      final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(properties);

      final MockServerHttpRequest request =
          MockServerHttpRequest.post("/webhooks").header("X-API-Key", "   ").build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(chain.wasFilterCalled()).isFalse();
      assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("should_reject_invalid_api_key")
    final void should_reject_invalid_api_key() {
      final WebhookProperties properties = createEnabledProperties(Set.of(VALID_API_KEY));
      final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(properties);

      final MockServerHttpRequest request =
          MockServerHttpRequest.post("/webhooks").header("X-API-Key", INVALID_API_KEY).build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(chain.wasFilterCalled()).isFalse();
      assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("Webhook Disabled State")
  final class WebhookDisabledTests {

    @Test
    @DisplayName("should_reject_requests_when_webhook_disabled")
    final void should_reject_requests_when_webhook_disabled() {
      final WebhookProperties properties = createDisabledProperties(Set.of(VALID_API_KEY));
      final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(properties);

      final MockServerHttpRequest request =
          MockServerHttpRequest.post("/webhooks").header("X-API-Key", VALID_API_KEY).build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(chain.wasFilterCalled()).isFalse();
      assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
  }

  @Nested
  @DisplayName("No API Keys Configured")
  final class NoApiKeysConfiguredTests {

    @Test
    @DisplayName("should_reject_requests_when_no_api_keys_configured")
    final void should_reject_requests_when_no_api_keys_configured() {
      final WebhookProperties properties = createEnabledProperties(Set.of());
      final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(properties);

      final MockServerHttpRequest request =
          MockServerHttpRequest.post("/webhooks").header("X-API-Key", VALID_API_KEY).build();
      final MockServerWebExchange exchange = MockServerWebExchange.from(request);
      final TestWebFilterChain chain = new TestWebFilterChain();

      StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

      assertThat(chain.wasFilterCalled()).isFalse();
      assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  private WebhookProperties createEnabledProperties(final Set<String> apiKeys) {
    return new WebhookProperties(apiKeys, Duration.ofHours(24), 60, Set.of(), true);
  }

  private WebhookProperties createDisabledProperties(final Set<String> apiKeys) {
    return new WebhookProperties(apiKeys, Duration.ofHours(24), 60, Set.of(), false);
  }

  private static final class TestWebFilterChain implements WebFilterChain {
    private boolean filterCalled = false;

    @Override
    public Mono<Void> filter(final org.springframework.web.server.ServerWebExchange exchange) {
      filterCalled = true;
      return Mono.empty();
    }

    boolean wasFilterCalled() {
      return filterCalled;
    }
  }
}
