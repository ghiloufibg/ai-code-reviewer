package com.ghiloufi.aicode.core.infrastructure.resilience;

import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class Resilience {

  private final RetryRegistry retryRegistry;

  public <T> Function<Mono<T>, Mono<T>> criticalMono(final String instanceName) {
    final Retry retry = retryRegistry.retry(instanceName);
    return mono ->
        mono.transformDeferred(RetryOperator.of(retry))
            .doOnError(e -> log.error("CRITICAL [{}]: Failed after retries", instanceName, e));
  }

  public <T> Function<Mono<T>, Mono<T>> bestEffortMono(final String instanceName) {
    final Retry retry = retryRegistry.retry(instanceName);
    return mono ->
        mono.transformDeferred(RetryOperator.of(retry))
            .doOnError(
                e -> log.warn("DEGRADED [{}]: Failed after retries, continuing", instanceName, e))
            .onErrorResume(e -> Mono.empty());
  }

  public <T> Function<Mono<T>, Mono<T>> optionalMono(final String instanceName) {
    return mono ->
        mono.doOnError(e -> log.debug("SKIPPED [{}]: {}", instanceName, e.getMessage()))
            .onErrorResume(e -> Mono.empty());
  }

  public <T> Function<Flux<T>, Flux<T>> criticalFlux(final String instanceName) {
    final Retry retry = retryRegistry.retry(instanceName);
    return flux ->
        flux.transformDeferred(RetryOperator.of(retry))
            .doOnError(e -> log.error("CRITICAL [{}]: Failed after retries", instanceName, e));
  }

  public <T> Function<Flux<T>, Flux<T>> bestEffortFlux(final String instanceName) {
    final Retry retry = retryRegistry.retry(instanceName);
    return flux ->
        flux.transformDeferred(RetryOperator.of(retry))
            .doOnError(
                e -> log.warn("DEGRADED [{}]: Failed after retries, continuing", instanceName, e))
            .onErrorResume(e -> Flux.empty());
  }

  public <T> Function<Flux<T>, Flux<T>> optionalFlux(final String instanceName) {
    return flux ->
        flux.doOnError(e -> log.debug("SKIPPED [{}]: {}", instanceName, e.getMessage()))
            .onErrorResume(e -> Flux.empty());
  }
}
