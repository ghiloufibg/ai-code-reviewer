package com.ghiloufi.aicode.core.domain.model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record PrioritizedFindings(
    List<ReviewResult.Issue> criticalIssues,
    List<ReviewResult.Issue> highPriorityIssues,
    List<ReviewResult.Issue> mediumPriorityIssues,
    List<ReviewResult.Issue> lowPriorityIssues,
    List<ReviewResult.Issue> filteredOut,
    PrioritizationMetrics metrics) {

  public PrioritizedFindings {
    Objects.requireNonNull(metrics, "metrics must not be null");
    criticalIssues = criticalIssues != null ? List.copyOf(criticalIssues) : List.of();
    highPriorityIssues = highPriorityIssues != null ? List.copyOf(highPriorityIssues) : List.of();
    mediumPriorityIssues =
        mediumPriorityIssues != null ? List.copyOf(mediumPriorityIssues) : List.of();
    lowPriorityIssues = lowPriorityIssues != null ? List.copyOf(lowPriorityIssues) : List.of();
    filteredOut = filteredOut != null ? List.copyOf(filteredOut) : List.of();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static PrioritizedFindings empty() {
    return new PrioritizedFindings(
        List.of(), List.of(), List.of(), List.of(), List.of(), PrioritizationMetrics.empty());
  }

  public static PrioritizedFindings fromIssues(
      final List<ReviewResult.Issue> issues,
      final double minConfidenceThreshold,
      final int maxIssuesPerFile) {

    final var partitioned = partitionByPriority(issues);
    final var filtered =
        applyFilters(
            List.of(
                partitioned.getOrDefault(Priority.CRITICAL, List.of()),
                partitioned.getOrDefault(Priority.HIGH, List.of()),
                partitioned.getOrDefault(Priority.MEDIUM, List.of()),
                partitioned.getOrDefault(Priority.LOW, List.of())),
            minConfidenceThreshold,
            maxIssuesPerFile);

    final var metrics = PrioritizationMetrics.calculate(issues, filtered.includedIssues());

    return new PrioritizedFindings(
        filtered.critical(),
        filtered.high(),
        filtered.medium(),
        filtered.low(),
        filtered.filteredOut(),
        metrics);
  }

  public List<ReviewResult.Issue> allPrioritizedIssues() {
    return java.util.stream.Stream.of(
            criticalIssues, highPriorityIssues, mediumPriorityIssues, lowPriorityIssues)
        .flatMap(List::stream)
        .toList();
  }

  public int totalIncludedCount() {
    return criticalIssues.size()
        + highPriorityIssues.size()
        + mediumPriorityIssues.size()
        + lowPriorityIssues.size();
  }

  public int totalFilteredCount() {
    return filteredOut.size();
  }

  public boolean hasCriticalIssues() {
    return !criticalIssues.isEmpty();
  }

  public boolean hasHighPriorityIssues() {
    return !highPriorityIssues.isEmpty();
  }

  public boolean isEmpty() {
    return totalIncludedCount() == 0;
  }

  private static Map<Priority, List<ReviewResult.Issue>> partitionByPriority(
      final List<ReviewResult.Issue> issues) {
    return issues.stream().collect(Collectors.groupingBy(PrioritizedFindings::determinePriority));
  }

  private static Priority determinePriority(final ReviewResult.Issue issue) {
    final var severity = issue.getSeverity();
    if (severity == null) {
      return Priority.MEDIUM;
    }

    return switch (severity.toLowerCase()) {
      case "critical", "blocker" -> Priority.CRITICAL;
      case "error", "high" -> Priority.HIGH;
      case "warning", "medium" -> Priority.MEDIUM;
      case "info", "low", "suggestion" -> Priority.LOW;
      default -> Priority.MEDIUM;
    };
  }

  private static FilteredResult applyFilters(
      final List<List<ReviewResult.Issue>> priorityGroups,
      final double minConfidenceThreshold,
      final int maxIssuesPerFile) {

    final var critical = priorityGroups.get(0);
    final var high = priorityGroups.get(1);
    final var medium = priorityGroups.get(2);
    final var low = priorityGroups.get(3);

    final var allIssues =
        java.util.stream.Stream.of(critical, high, medium, low).flatMap(List::stream).toList();

    final var filteredByConfidence =
        allIssues.stream()
            .filter(
                issue ->
                    issue.getConfidenceScore() == null
                        || issue.getConfidenceScore() >= minConfidenceThreshold)
            .toList();

    final var filteredOut =
        allIssues.stream().filter(issue -> !filteredByConfidence.contains(issue)).toList();

    final var byFile =
        filteredByConfidence.stream()
            .collect(
                Collectors.groupingBy(
                    issue -> issue.getFile() != null ? issue.getFile() : "unknown"));

    final List<ReviewResult.Issue> includedAfterFileLimit = new java.util.ArrayList<>();
    final List<ReviewResult.Issue> excludedByFileLimit = new java.util.ArrayList<>();

    for (final var entry : byFile.entrySet()) {
      final var fileIssues = entry.getValue();
      final var sorted =
          fileIssues.stream()
              .sorted(
                  Comparator.comparingInt((ReviewResult.Issue i) -> determinePriority(i).ordinal())
                      .thenComparing(
                          i -> i.getConfidenceScore() != null ? -i.getConfidenceScore() : 0.0))
              .toList();

      if (sorted.size() <= maxIssuesPerFile) {
        includedAfterFileLimit.addAll(sorted);
      } else {
        includedAfterFileLimit.addAll(sorted.subList(0, maxIssuesPerFile));
        excludedByFileLimit.addAll(sorted.subList(maxIssuesPerFile, sorted.size()));
      }
    }

    final var allFilteredOut =
        java.util.stream.Stream.concat(filteredOut.stream(), excludedByFileLimit.stream()).toList();

    final var finalCritical =
        includedAfterFileLimit.stream()
            .filter(i -> determinePriority(i) == Priority.CRITICAL)
            .toList();
    final var finalHigh =
        includedAfterFileLimit.stream().filter(i -> determinePriority(i) == Priority.HIGH).toList();
    final var finalMedium =
        includedAfterFileLimit.stream()
            .filter(i -> determinePriority(i) == Priority.MEDIUM)
            .toList();
    final var finalLow =
        includedAfterFileLimit.stream().filter(i -> determinePriority(i) == Priority.LOW).toList();

    return new FilteredResult(
        finalCritical, finalHigh, finalMedium, finalLow, allFilteredOut, includedAfterFileLimit);
  }

  private record FilteredResult(
      List<ReviewResult.Issue> critical,
      List<ReviewResult.Issue> high,
      List<ReviewResult.Issue> medium,
      List<ReviewResult.Issue> low,
      List<ReviewResult.Issue> filteredOut,
      List<ReviewResult.Issue> includedIssues) {}

  public enum Priority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
  }

  public record PrioritizationMetrics(
      int totalInputIssues,
      int totalOutputIssues,
      int filteredByConfidence,
      int filteredByFileLimit,
      double averageConfidence,
      Map<String, Integer> countsByPriority) {

    public PrioritizationMetrics {
      countsByPriority = countsByPriority != null ? Map.copyOf(countsByPriority) : Map.of();
    }

    public static PrioritizationMetrics empty() {
      return new PrioritizationMetrics(0, 0, 0, 0, 0.0, Map.of());
    }

    public static PrioritizationMetrics calculate(
        final List<ReviewResult.Issue> input, final List<ReviewResult.Issue> output) {

      final var totalInput = input.size();
      final var totalOutput = output.size();

      final var avgConfidence =
          output.stream()
              .filter(i -> i.getConfidenceScore() != null)
              .mapToDouble(ReviewResult.Issue::getConfidenceScore)
              .average()
              .orElse(0.0);

      final var counts =
          output.stream()
              .collect(
                  Collectors.groupingBy(
                      i -> determinePriority(i).name(), Collectors.summingInt(i -> 1)));

      return new PrioritizationMetrics(
          totalInput, totalOutput, totalInput - totalOutput, 0, avgConfidence, counts);
    }

    public double filterRate() {
      return totalInputIssues > 0
          ? (double) (totalInputIssues - totalOutputIssues) / totalInputIssues
          : 0.0;
    }
  }

  public static final class Builder {
    private List<ReviewResult.Issue> criticalIssues = List.of();
    private List<ReviewResult.Issue> highPriorityIssues = List.of();
    private List<ReviewResult.Issue> mediumPriorityIssues = List.of();
    private List<ReviewResult.Issue> lowPriorityIssues = List.of();
    private List<ReviewResult.Issue> filteredOut = List.of();
    private PrioritizationMetrics metrics = PrioritizationMetrics.empty();

    private Builder() {}

    public Builder criticalIssues(final List<ReviewResult.Issue> issues) {
      this.criticalIssues = issues;
      return this;
    }

    public Builder highPriorityIssues(final List<ReviewResult.Issue> issues) {
      this.highPriorityIssues = issues;
      return this;
    }

    public Builder mediumPriorityIssues(final List<ReviewResult.Issue> issues) {
      this.mediumPriorityIssues = issues;
      return this;
    }

    public Builder lowPriorityIssues(final List<ReviewResult.Issue> issues) {
      this.lowPriorityIssues = issues;
      return this;
    }

    public Builder filteredOut(final List<ReviewResult.Issue> issues) {
      this.filteredOut = issues;
      return this;
    }

    public Builder metrics(final PrioritizationMetrics metrics) {
      this.metrics = metrics;
      return this;
    }

    public PrioritizedFindings build() {
      return new PrioritizedFindings(
          criticalIssues,
          highPriorityIssues,
          mediumPriorityIssues,
          lowPriorityIssues,
          filteredOut,
          metrics);
    }
  }
}
