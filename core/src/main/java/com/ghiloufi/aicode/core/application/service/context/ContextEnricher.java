package com.ghiloufi.aicode.core.application.service.context;

import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalMetadata;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class ContextEnricher {

  public EnrichedDiffAnalysisBundle mergeResults(
      final DiffAnalysisBundle diffBundle, final List<ContextRetrievalResult> strategyResults) {

    if (strategyResults.isEmpty()) {
      log.debug("No strategy results to merge, returning empty context");
      return new EnrichedDiffAnalysisBundle(diffBundle);
    }

    log.debug("=== CONTEXT ENRICHER MERGE ===");
    log.debug("Input strategy results count: {}", strategyResults.size());
    logInputResults(strategyResults);

    final List<ContextMatch> mergedMatches = deduplicateMatches(strategyResults);

    log.debug("After deduplication: {} unique matches", mergedMatches.size());
    logMergedMatches(mergedMatches);

    final ContextRetrievalMetadata mergedMetadata = mergeMetadata(strategyResults);

    final ContextRetrievalResult combinedResult =
        new ContextRetrievalResult(mergedMatches, mergedMetadata);

    log.debug("==============================");

    return new EnrichedDiffAnalysisBundle(
        diffBundle.repositoryIdentifier(),
        diffBundle.structuredDiff(),
        diffBundle.rawDiffText(),
        combinedResult,
        diffBundle.prMetadata());
  }

  private void logInputResults(final List<ContextRetrievalResult> strategyResults) {
    if (!log.isDebugEnabled()) {
      return;
    }

    for (final ContextRetrievalResult result : strategyResults) {
      log.debug(
          "  Strategy '{}': {} matches in {}ms",
          result.metadata().strategyName(),
          result.getTotalMatches(),
          result.metadata().executionTime().toMillis());
      for (final ContextMatch match : result.matches()) {
        log.debug(
            "    - {} (confidence: {}, reason: {})",
            match.filePath(),
            String.format("%.2f", match.confidence()),
            match.reason());
      }
    }
  }

  private void logMergedMatches(final List<ContextMatch> mergedMatches) {
    if (!log.isDebugEnabled()) {
      return;
    }

    for (final ContextMatch match : mergedMatches) {
      log.debug(
          "  Merged: {} (confidence: {}, reason: {})",
          match.filePath(),
          String.format("%.2f", match.confidence()),
          match.reason());
    }
  }

  private List<ContextMatch> deduplicateMatches(final List<ContextRetrievalResult> results) {
    final Map<String, ContextMatch> uniqueMatches = new LinkedHashMap<>();

    for (final ContextRetrievalResult result : results) {
      for (final ContextMatch match : result.matches()) {
        final String filePath = match.filePath();

        if (!uniqueMatches.containsKey(filePath)) {
          uniqueMatches.put(filePath, match);
        } else {
          final ContextMatch existing = uniqueMatches.get(filePath);
          if (match.confidence() > existing.confidence()) {
            uniqueMatches.put(filePath, match);
          }
        }
      }
    }

    return uniqueMatches.values().stream()
        .sorted(Comparator.comparingDouble(ContextMatch::confidence).reversed())
        .toList();
  }

  private ContextRetrievalMetadata mergeMetadata(final List<ContextRetrievalResult> results) {
    final List<String> strategyNames =
        results.stream().map(r -> r.metadata().strategyName()).toList();

    final Duration totalDuration =
        results.stream()
            .map(r -> r.metadata().executionTime())
            .reduce(Duration.ZERO, Duration::plus);

    final int totalCandidates =
        results.stream().mapToInt(r -> r.metadata().totalCandidates()).sum();

    final int highConfidenceCount =
        results.stream().mapToInt(r -> r.metadata().highConfidenceCount()).sum();

    final Map<MatchReason, Integer> combinedReasons = new LinkedHashMap<>();
    for (final ContextRetrievalResult result : results) {
      result
          .metadata()
          .reasonDistribution()
          .forEach((reason, count) -> combinedReasons.merge(reason, count, Integer::sum));
    }

    return new ContextRetrievalMetadata(
        String.join("+", strategyNames),
        totalDuration,
        totalCandidates,
        highConfidenceCount,
        combinedReasons);
  }
}
