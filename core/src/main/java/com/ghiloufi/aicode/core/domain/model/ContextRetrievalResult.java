package com.ghiloufi.aicode.core.domain.model;

import java.util.List;
import java.util.Objects;

public record ContextRetrievalResult(
    List<ContextMatch> matches, ContextRetrievalMetadata metadata) {

  public ContextRetrievalResult {
    Objects.requireNonNull(matches, "Matches cannot be null");
    Objects.requireNonNull(metadata, "Metadata cannot be null");
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
