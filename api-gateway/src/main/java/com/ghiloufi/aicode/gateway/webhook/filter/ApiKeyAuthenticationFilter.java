package com.ghiloufi.aicode.gateway.webhook.filter;

import com.ghiloufi.aicode.gateway.webhook.config.WebhookProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyAuthenticationFilter implements WebFilter {

  private static final String WEBHOOK_PATH = "/webhooks";
  private static final String API_KEY_HEADER = "X-API-Key";
  private static final String UNAUTHORIZED_RESPONSE =
      "{\"error\":\"unauthorized\",\"message\":\"%s\"}";

  private final WebhookProperties properties;

  @Override
  public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
    final String path = exchange.getRequest().getPath().value();

    if (!path.equals(WEBHOOK_PATH)) {
      return chain.filter(exchange);
    }

    if (!properties.isEnabled()) {
      log.debug("Webhook endpoint is disabled, rejecting request");
      return forbidden(exchange, "Webhook endpoint is disabled");
    }

    if (!properties.hasApiKeys()) {
      log.warn("No API keys configured for webhook endpoint");
      return unauthorized(exchange, "Webhook authentication not configured");
    }

    final String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);

    if (apiKey == null || apiKey.isBlank()) {
      log.debug("Missing API key in request to webhook endpoint");
      return unauthorized(exchange, "Missing API key");
    }

    if (!isValidApiKey(apiKey)) {
      log.warn("Invalid API key attempt on webhook endpoint");
      return unauthorized(exchange, "Invalid API key");
    }

    log.debug("API key validated successfully for webhook request");
    return chain.filter(exchange);
  }

  private boolean isValidApiKey(final String apiKey) {
    final byte[] providedKeyBytes = apiKey.getBytes(StandardCharsets.UTF_8);

    return properties.getApiKeys().stream()
        .anyMatch(
            configuredKey -> {
              final byte[] configuredKeyBytes = configuredKey.getBytes(StandardCharsets.UTF_8);
              return MessageDigest.isEqual(configuredKeyBytes, providedKeyBytes);
            });
  }

  private Mono<Void> unauthorized(final ServerWebExchange exchange, final String message) {
    return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, message);
  }

  private Mono<Void> forbidden(final ServerWebExchange exchange, final String message) {
    return writeErrorResponse(exchange, HttpStatus.FORBIDDEN, message);
  }

  private Mono<Void> writeErrorResponse(
      final ServerWebExchange exchange, final HttpStatus status, final String message) {
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    final byte[] responseBody =
        String.format(UNAUTHORIZED_RESPONSE, message).getBytes(StandardCharsets.UTF_8);
    return exchange
        .getResponse()
        .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(responseBody)));
  }
}
