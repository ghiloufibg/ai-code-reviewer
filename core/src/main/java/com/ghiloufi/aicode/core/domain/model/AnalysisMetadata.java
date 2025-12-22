package com.ghiloufi.aicode.core.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AnalysisMetadata(
    Instant startTime,
    Instant endTime,
    Duration totalDuration,
    String containerImage,
    String containerId,
    List<String> toolsExecuted,
    Map<String, Duration> toolDurations,
    int filesAnalyzed,
    int linesAnalyzed,
    String repositoryBranch,
    String commitSha,
    boolean cloneSuccessful,
    boolean analysisComplete) {

  public AnalysisMetadata {
    Objects.requireNonNull(startTime, "startTime must not be null");
    toolsExecuted = toolsExecuted != null ? List.copyOf(toolsExecuted) : List.of();
    toolDurations = toolDurations != null ? Map.copyOf(toolDurations) : Map.of();
  }

  public static AnalysisMetadata started(
      final String containerImage,
      final String containerId,
      final String branch,
      final String commitSha) {
    return new AnalysisMetadata(
        Instant.now(),
        null,
        null,
        containerImage,
        containerId,
        List.of(),
        Map.of(),
        0,
        0,
        branch,
        commitSha,
        false,
        false);
  }

  public AnalysisMetadata withCloneSuccess() {
    return new AnalysisMetadata(
        startTime,
        endTime,
        totalDuration,
        containerImage,
        containerId,
        toolsExecuted,
        toolDurations,
        filesAnalyzed,
        linesAnalyzed,
        repositoryBranch,
        commitSha,
        true,
        analysisComplete);
  }

  public AnalysisMetadata withToolCompleted(final String tool, final Duration duration) {
    final List<String> updatedTools =
        java.util.stream.Stream.concat(toolsExecuted.stream(), java.util.stream.Stream.of(tool))
            .toList();

    final Map<String, Duration> updatedDurations =
        java.util.stream.Stream.concat(
                toolDurations.entrySet().stream(),
                java.util.stream.Stream.of(Map.entry(tool, duration)))
            .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return new AnalysisMetadata(
        startTime,
        endTime,
        totalDuration,
        containerImage,
        containerId,
        updatedTools,
        updatedDurations,
        filesAnalyzed,
        linesAnalyzed,
        repositoryBranch,
        commitSha,
        cloneSuccessful,
        analysisComplete);
  }

  public AnalysisMetadata completed(final int files, final int lines) {
    final Instant now = Instant.now();
    return new AnalysisMetadata(
        startTime,
        now,
        Duration.between(startTime, now),
        containerImage,
        containerId,
        toolsExecuted,
        toolDurations,
        files,
        lines,
        repositoryBranch,
        commitSha,
        cloneSuccessful,
        true);
  }

  public boolean hasToolResult(final String tool) {
    return toolsExecuted.contains(tool);
  }

  public Duration getToolDuration(final String tool) {
    return toolDurations.getOrDefault(tool, Duration.ZERO);
  }
}
