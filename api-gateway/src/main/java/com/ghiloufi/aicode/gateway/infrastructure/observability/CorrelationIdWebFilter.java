package com.ghiloufi.aicode.gateway.infrastructure.observability;

import com.ghiloufi.aicode.core.infrastructure.observability.CorrelationIdHolder;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

  @Override
  public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
    final String correlationId = extractOrGenerateCorrelationId(exchange);

    exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

    log.debug("Processing request with correlationId={}", correlationId);

    return chain
        .filter(exchange)
        .contextWrite(ctx -> CorrelationIdHolder.withCorrelationId(ctx, correlationId));
  }

  private String extractOrGenerateCorrelationId(final ServerWebExchange exchange) {
    return Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER))
        .filter(id -> !id.isBlank())
        .orElseGet(() -> UUID.randomUUID().toString());
  }
}
