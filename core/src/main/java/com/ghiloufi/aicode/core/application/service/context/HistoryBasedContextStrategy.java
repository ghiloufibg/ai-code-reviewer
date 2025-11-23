package com.ghiloufi.aicode.core.application.service.context;

import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalMetadata;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.port.output.ContextRetrievalStrategy;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class HistoryBasedContextStrategy implements ContextRetrievalStrategy {

  private static final String STRATEGY_NAME = "git-history";
  private static final int DEFAULT_PRIORITY = 20;

  private final GitHistoryCoChangeAnalyzer analyzer;

  public HistoryBasedContextStrategy(final GitHistoryCoChangeAnalyzer analyzer) {
    this.analyzer = Objects.requireNonNull(analyzer, "GitHistoryCoChangeAnalyzer cannot be null");
  }

  @Override
  public Mono<ContextRetrievalResult> retrieveContext(final DiffAnalysisBundle diffBundle) {
    final Instant startTime = Instant.now();
    final RepositoryIdentifier repository = diffBundle.repositoryIdentifier();
    final List<String> changedFiles =
        diffBundle.structuredDiff().files.stream()
            .map(file -> file.newPath)
            .filter(Objects::nonNull)
            .toList();

    return Flux.fromIterable(changedFiles)
        .flatMap(targetFile -> analyzer.analyzeCoChanges(repository, targetFile))
        .flatMap(result -> Flux.fromIterable(result.toContextMatches()))
        .collectList()
        .map(
            matches -> {
              final List<ContextMatch> deduplicated = deduplicateMatches(matches);
              final ContextRetrievalMetadata metadata = buildMetadata(deduplicated, startTime);
              return new ContextRetrievalResult(deduplicated, metadata);
            });
  }

  @Override
  public String getStrategyName() {
    return STRATEGY_NAME;
  }

  @Override
  public int getPriority() {
    return DEFAULT_PRIORITY;
  }

  private List<ContextMatch> deduplicateMatches(final List<ContextMatch> matches) {
    final Map<String, ContextMatch> uniqueMatches = new LinkedHashMap<>();

    for (final ContextMatch match : matches) {
      final String key = match.filePath();

      if (!uniqueMatches.containsKey(key)) {
        uniqueMatches.put(key, match);
      } else {
        final ContextMatch existing = uniqueMatches.get(key);
        if (match.confidence() > existing.confidence()) {
          uniqueMatches.put(key, match);
        }
      }
    }

    return uniqueMatches.values().stream().toList();
  }

  private ContextRetrievalMetadata buildMetadata(
      final List<ContextMatch> matches, final Instant startTime) {

    final Duration executionTime = Duration.between(startTime, Instant.now());
    final int totalCandidates = matches.size();
    final int highConfidenceCount =
        (int) matches.stream().filter(ContextMatch::isHighConfidence).count();

    final Map<MatchReason, Integer> reasonDistribution = calculateReasonDistribution(matches);

    return new ContextRetrievalMetadata(
        STRATEGY_NAME, executionTime, totalCandidates, highConfidenceCount, reasonDistribution);
  }

  private Map<MatchReason, Integer> calculateReasonDistribution(final List<ContextMatch> matches) {
    final Map<MatchReason, Integer> distribution = new LinkedHashMap<>();

    for (final ContextMatch match : matches) {
      distribution.merge(match.reason(), 1, Integer::sum);
    }

    return distribution;
  }
}
