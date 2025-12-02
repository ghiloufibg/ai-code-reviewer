package com.ghiloufi.aicode.core.application.service.context;

import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.port.output.ContextRetrievalStrategy;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ContextOrchestrator {

  private final List<ContextRetrievalStrategy> strategies;
  private final ContextEnricher contextEnricher;
  private final ContextRetrievalConfig config;

  public ContextOrchestrator(
      final List<ContextRetrievalStrategy> strategies,
      final ContextEnricher contextEnricher,
      final ContextRetrievalConfig config) {
    this.strategies = strategies;
    this.contextEnricher = contextEnricher;
    this.config = config;
  }

  public Mono<EnrichedDiffAnalysisBundle> retrieveEnrichedContext(
      final DiffAnalysisBundle diffBundle) {

    Objects.requireNonNull(diffBundle, "DiffAnalysisBundle cannot be null");

    if (!config.enabled()) {
      log.debug("Context retrieval is disabled");
      return Mono.just(new EnrichedDiffAnalysisBundle(diffBundle));
    }

    if (shouldSkipDueToSize(diffBundle)) {
      log.info(
          "Skipping context retrieval for large diff: {} lines", diffBundle.getTotalLineCount());
      return Mono.just(new EnrichedDiffAnalysisBundle(diffBundle));
    }

    final List<ContextRetrievalStrategy> enabledStrategies = getEnabledStrategies();

    if (enabledStrategies.isEmpty()) {
      log.warn("No enabled context retrieval strategies found");
      return Mono.just(new EnrichedDiffAnalysisBundle(diffBundle));
    }

    log.debug("Executing {} context retrieval strategies in parallel", enabledStrategies.size());

    return Flux.fromIterable(enabledStrategies)
        .flatMap(strategy -> executeStrategyWithTimeout(strategy, diffBundle))
        .collectList()
        .map(results -> contextEnricher.mergeResults(diffBundle, results))
        .doOnSuccess(
            enriched -> {
              if (enriched.hasContext()) {
                log.info(
                    "Context retrieval complete: {} matches from {} strategies",
                    enriched.getContextMatchCount(),
                    enriched.contextResult().metadata().strategyName());
                logContextDetails(enriched);
              }
            })
        .defaultIfEmpty(new EnrichedDiffAnalysisBundle(diffBundle));
  }

  private Mono<ContextRetrievalResult> executeStrategyWithTimeout(
      final ContextRetrievalStrategy strategy, final DiffAnalysisBundle diffBundle) {

    return strategy
        .retrieveContext(diffBundle)
        .timeout(Duration.ofSeconds(config.strategyTimeoutSeconds()))
        .doOnSuccess(
            result ->
                log.debug(
                    "Strategy '{}' found {} matches in {}ms",
                    strategy.getStrategyName(),
                    result.getTotalMatches(),
                    result.metadata().executionTime().toMillis()))
        .onErrorResume(
            error -> {
              log.warn(
                  "Strategy '{}' failed or timed out: {}",
                  strategy.getStrategyName(),
                  error.getMessage());
              return Mono.empty();
            });
  }

  private List<ContextRetrievalStrategy> getEnabledStrategies() {
    return strategies.stream()
        .filter(strategy -> config.isStrategyEnabled(strategy.getStrategyName()))
        .sorted(Comparator.comparingInt(ContextRetrievalStrategy::getPriority))
        .toList();
  }

  private boolean shouldSkipDueToSize(final DiffAnalysisBundle diffBundle) {
    return config.rollout().skipLargeDiffs()
        && diffBundle.getTotalLineCount() > config.rollout().maxDiffLines();
  }

  private void logContextDetails(final EnrichedDiffAnalysisBundle enriched) {
    if (!log.isDebugEnabled()) {
      return;
    }

    final var contextResult = enriched.contextResult();
    final var metadata = contextResult.metadata();

    log.debug("=== CONTEXT ORCHESTRATOR OUTPUT ===");
    log.debug("Strategy: {}", metadata.strategyName());
    log.debug("Execution time: {}ms", metadata.executionTime().toMillis());
    log.debug("Total candidates evaluated: {}", metadata.totalCandidates());
    log.debug("High confidence matches: {}", metadata.highConfidenceCount());
    log.debug("Total matches: {}", enriched.getContextMatchCount());

    for (final var match : contextResult.matches()) {
      log.debug(
          "  Match: {} | confidence={} | reason={} | evidence={}",
          match.filePath(),
          String.format("%.2f", match.confidence()),
          match.reason(),
          match.evidence());
    }

    log.debug("Reason distribution: {}", metadata.reasonDistribution());
    log.debug("===================================");
  }
}
