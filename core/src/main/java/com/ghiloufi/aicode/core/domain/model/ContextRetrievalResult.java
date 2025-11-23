package com.ghiloufi.aicode.core.domain.model;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ContextRetrievalResult(
    List<ContextMatch> matches, ContextRetrievalMetadata metadata) {

  private static final ContextRetrievalResult EMPTY =
      new ContextRetrievalResult(
          List.of(), new ContextRetrievalMetadata("none", Duration.ZERO, 0, 0, Map.of()));

  public ContextRetrievalResult {
    Objects.requireNonNull(matches, "Matches cannot be null");
    Objects.requireNonNull(metadata, "Metadata cannot be null");
  }

  public static ContextRetrievalResult empty() {
    return EMPTY;
  }

  public int getTotalMatches() {
    return matches.size();
  }

  public List<ContextMatch> getHighConfidenceMatches() {
    return matches.stream().filter(ContextMatch::isHighConfidence).toList();
  }

  public List<ContextMatch> getMatchesByReason(final MatchReason reason) {
    return matches.stream().filter(match -> match.reason() == reason).toList();
  }

  public boolean isEmpty() {
    return matches.isEmpty();
  }
}
