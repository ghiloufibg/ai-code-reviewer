package com.ghiloufi.aicode.core.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record CoChangeAnalysisResult(
    String targetFile,
    List<CoChangeMetrics> coChangeMetrics,
    Map<String, Integer> rawFrequencyMap,
    int maxFrequency) {

  public CoChangeAnalysisResult {
    Objects.requireNonNull(targetFile, "Target file cannot be null");
    Objects.requireNonNull(coChangeMetrics, "Co-change metrics cannot be null");
    Objects.requireNonNull(rawFrequencyMap, "Raw frequency map cannot be null");

    if (targetFile.isBlank()) {
      throw new IllegalArgumentException("Target file cannot be blank");
    }

    if (maxFrequency < 0) {
      throw new IllegalArgumentException("Max frequency cannot be negative");
    }
  }

  public List<CoChangeMetrics> getHighConfidenceMatches() {
    return coChangeMetrics.stream().filter(CoChangeMetrics::isHighFrequency).toList();
  }

  public List<CoChangeMetrics> getMediumConfidenceMatches() {
    return coChangeMetrics.stream().filter(CoChangeMetrics::isMediumFrequency).toList();
  }

  public List<ContextMatch> toContextMatches() {
    return coChangeMetrics.stream()
        .map(
            metrics ->
                new ContextMatch(
                    metrics.filePath(),
                    metrics.getMatchReason(),
                    metrics.calculateConfidence(),
                    metrics.formatEvidence()))
        .toList();
  }

  public int getTotalAnalyzedFiles() {
    return coChangeMetrics.size();
  }

  public boolean wasFileAnalyzed(final String filePath) {
    return rawFrequencyMap.containsKey(filePath);
  }

  public int getFrequencyFor(final String filePath) {
    return rawFrequencyMap.getOrDefault(filePath, 0);
  }

  public double getAverageFrequency() {
    if (coChangeMetrics.isEmpty()) {
      return 0.0;
    }

    final int totalFrequency =
        coChangeMetrics.stream().mapToInt(CoChangeMetrics::coChangeCount).sum();

    return (double) totalFrequency / coChangeMetrics.size();
  }

  public boolean hasMatches() {
    return !coChangeMetrics.isEmpty();
  }
}
