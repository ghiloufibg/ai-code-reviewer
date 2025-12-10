package com.ghiloufi.aicode.core.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewMetrics {

  private final MeterRegistry meterRegistry;

  private Counter reviewsInitiated;
  private Counter reviewsCompleted;
  private Counter reviewsFailed;
  private Counter issuesFound;
  private Counter lowConfidenceFiltered;
  private Timer reviewDuration;
  private Timer llmResponseTime;
  private AtomicInteger activeReviews;

  @PostConstruct
  public void init() {
    reviewsInitiated = meterRegistry.counter("reviews.initiated.total");
    reviewsCompleted = meterRegistry.counter("reviews.completed.total");
    reviewsFailed = meterRegistry.counter("reviews.failed.total");
    issuesFound = meterRegistry.counter("reviews.issues.found.total");
    lowConfidenceFiltered = meterRegistry.counter("reviews.issues.filtered.low_confidence");

    reviewDuration = meterRegistry.timer("reviews.duration");
    llmResponseTime = meterRegistry.timer("llm.response.time");

    activeReviews = new AtomicInteger(0);
    meterRegistry.gauge("reviews.active", activeReviews);

    log.info("Review metrics initialized");
  }

  public void recordReviewInitiated(final String repository, final String provider) {
    reviewsInitiated.increment();
    activeReviews.incrementAndGet();
    meterRegistry
        .counter("reviews.initiated", "repository", repository, "provider", provider)
        .increment();
  }

  public void recordReviewCompleted(
      final String repository, final String provider, final int issueCount, final long durationMs) {
    reviewsCompleted.increment();
    activeReviews.decrementAndGet();
    issuesFound.increment(issueCount);
    reviewDuration.record(Duration.ofMillis(durationMs));
    meterRegistry
        .counter("reviews.completed", "repository", repository, "provider", provider)
        .increment();
    log.debug(
        "Review completed: repository={}, provider={}, issues={}, durationMs={}",
        repository,
        provider,
        issueCount,
        durationMs);
  }

  public void recordReviewFailed(
      final String repository, final String provider, final String reason) {
    reviewsFailed.increment();
    activeReviews.decrementAndGet();
    meterRegistry
        .counter("reviews.failed", "repository", repository, "provider", provider, "reason", reason)
        .increment();
    log.debug("Review failed: repository={}, provider={}, reason={}", repository, provider, reason);
  }

  public void recordLlmResponse(
      final String provider, final long durationMs, final boolean success) {
    llmResponseTime.record(Duration.ofMillis(durationMs));
    meterRegistry
        .counter("llm.requests", "provider", provider, "success", String.valueOf(success))
        .increment();
  }

  public void recordLowConfidenceFiltered(final int filteredCount) {
    lowConfidenceFiltered.increment(filteredCount);
  }

  public Timer.Sample startReviewTimer() {
    return Timer.start(meterRegistry);
  }

  public void stopReviewTimer(final Timer.Sample sample) {
    sample.stop(reviewDuration);
  }

  public int getActiveReviewCount() {
    return activeReviews.get();
  }
}
