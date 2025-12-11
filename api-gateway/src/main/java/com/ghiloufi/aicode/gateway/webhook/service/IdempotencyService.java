package com.ghiloufi.aicode.gateway.webhook.service;

import com.ghiloufi.aicode.gateway.webhook.config.WebhookProperties;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

  private static final String KEY_PREFIX = "webhook:idempotency:";

  private final ReactiveStringRedisTemplate redisTemplate;
  private final WebhookProperties properties;

  public Mono<Boolean> checkAndMark(final String idempotencyKey) {
    final String redisKey = KEY_PREFIX + idempotencyKey;
    final Duration ttl = properties.getIdempotencyTtl();
    final String timestamp = Instant.now().toString();

    return redisTemplate
        .opsForValue()
        .setIfAbsent(redisKey, timestamp, ttl)
        .doOnNext(
            isNew -> {
              if (Boolean.TRUE.equals(isNew)) {
                log.debug("Marked idempotency key as processed: {}", idempotencyKey);
              } else {
                log.debug("Idempotency key already exists: {}", idempotencyKey);
              }
            })
        .doOnError(
            error -> log.error("Failed to check idempotency key: {}", idempotencyKey, error));
  }

  public Mono<Boolean> exists(final String idempotencyKey) {
    final String redisKey = KEY_PREFIX + idempotencyKey;
    return redisTemplate
        .hasKey(redisKey)
        .doOnError(
            error ->
                log.error("Failed to check if idempotency key exists: {}", idempotencyKey, error));
  }
}
