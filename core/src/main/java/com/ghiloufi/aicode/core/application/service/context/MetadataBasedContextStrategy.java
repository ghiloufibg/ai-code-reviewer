package com.ghiloufi.aicode.core.application.service.context;

import com.ghiloufi.aicode.core.domain.model.ContextMatch;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalMetadata;
import com.ghiloufi.aicode.core.domain.model.ContextRetrievalResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.MatchReason;
import com.ghiloufi.aicode.core.domain.port.output.ContextRetrievalStrategy;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import reactor.core.publisher.Mono;

public final class MetadataBasedContextStrategy implements ContextRetrievalStrategy {

  private static final String STRATEGY_NAME = "metadata-based";
  private static final int DEFAULT_PRIORITY = 10;

  private final SCMPort scmPort;
  private final DiffFileReferenceExtractor referenceExtractor;
  private final DirectorySiblingAnalyzer siblingAnalyzer;
  private final EnhancedPathAnalyzer pathAnalyzer;

  public MetadataBasedContextStrategy(final SCMPort scmPort) {
    this.scmPort = Objects.requireNonNull(scmPort, "SCMPort cannot be null");
    this.referenceExtractor = new DiffFileReferenceExtractor();
    this.siblingAnalyzer = new DirectorySiblingAnalyzer();
    this.pathAnalyzer = new EnhancedPathAnalyzer();
  }

  @Override
  public Mono<ContextRetrievalResult> retrieveContext(final DiffAnalysisBundle diffBundle) {
    final Instant startTime = Instant.now();

    return scmPort
        .listRepositoryFiles()
        .map(
            repositoryFiles -> {
              final List<ContextMatch> allMatches = collectAllMatches(diffBundle, repositoryFiles);
              final List<ContextMatch> deduplicated = deduplicateMatches(allMatches);
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

  private List<ContextMatch> collectAllMatches(
      final DiffAnalysisBundle diffBundle, final List<String> repositoryFiles) {

    final List<ContextMatch> allMatches = new ArrayList<>();

    allMatches.addAll(referenceExtractor.extractReferences(diffBundle));

    final List<GitFileModification> modifiedFiles = diffBundle.structuredDiff().files;
    allMatches.addAll(siblingAnalyzer.analyzeSiblings(modifiedFiles, repositoryFiles));
    allMatches.addAll(pathAnalyzer.analyzePathPatterns(modifiedFiles, repositoryFiles));

    return allMatches;
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

    return new ArrayList<>(uniqueMatches.values());
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
