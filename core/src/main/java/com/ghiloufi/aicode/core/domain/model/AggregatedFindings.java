package com.ghiloufi.aicode.core.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AggregatedFindings(
    List<ReviewResult.Issue> issues,
    List<ReviewResult.Note> notes,
    String summary,
    Map<String, Integer> findingCountsBySource,
    Map<String, Integer> findingCountsBySeverity,
    double overallConfidence,
    int totalFindingsBeforeDedup,
    int totalFindingsAfterDedup,
    int totalFindingsFiltered) {

  public AggregatedFindings {
    Objects.requireNonNull(summary, "summary must not be null");
    issues = issues != null ? List.copyOf(issues) : List.of();
    notes = notes != null ? List.copyOf(notes) : List.of();
    findingCountsBySource =
        findingCountsBySource != null ? Map.copyOf(findingCountsBySource) : Map.of();
    findingCountsBySeverity =
        findingCountsBySeverity != null ? Map.copyOf(findingCountsBySeverity) : Map.of();

    if (overallConfidence < 0 || overallConfidence > 1) {
      throw new IllegalArgumentException("overallConfidence must be between 0 and 1");
    }
  }

  public static AggregatedFindings empty() {
    return new AggregatedFindings(
        List.of(), List.of(), "No findings", Map.of(), Map.of(), 1.0, 0, 0, 0);
  }

  public static Builder builder() {
    return new Builder();
  }

  public int totalIssueCount() {
    return issues.size();
  }

  public int totalNoteCount() {
    return notes.size();
  }

  public int totalFindingsCount() {
    return issues.size() + notes.size();
  }

  public int deduplicatedCount() {
    return totalFindingsBeforeDedup - totalFindingsAfterDedup;
  }

  public boolean hasHighSeverityIssues() {
    return issues.stream()
        .anyMatch(
            issue ->
                "error".equalsIgnoreCase(issue.getSeverity())
                    || "critical".equalsIgnoreCase(issue.getSeverity()));
  }

  public boolean hasSecurityIssues() {
    return issues.stream()
        .anyMatch(
            issue ->
                issue.getTitle() != null && issue.getTitle().toLowerCase().contains("security"));
  }

  public List<ReviewResult.Issue> issuesByFile(final String file) {
    return issues.stream().filter(issue -> file.equals(issue.getFile())).toList();
  }

  public List<ReviewResult.Issue> issuesBySeverity(final String severity) {
    return issues.stream().filter(issue -> severity.equalsIgnoreCase(issue.getSeverity())).toList();
  }

  public ReviewResult toReviewResult(final String llmProvider, final String llmModel) {
    return ReviewResult.builder()
        .summary(summary)
        .issues(issues)
        .nonBlockingNotes(notes)
        .llmProvider(llmProvider)
        .llmModel(llmModel)
        .filesAnalyzed(countUniqueFiles())
        .build();
  }

  private int countUniqueFiles() {
    return (int) issues.stream().map(ReviewResult.Issue::getFile).distinct().count();
  }

  public static final class Builder {
    private List<ReviewResult.Issue> issues = List.of();
    private List<ReviewResult.Note> notes = List.of();
    private String summary = "";
    private Map<String, Integer> findingCountsBySource = Map.of();
    private Map<String, Integer> findingCountsBySeverity = Map.of();
    private double overallConfidence = 1.0;
    private int totalFindingsBeforeDedup = 0;
    private int totalFindingsAfterDedup = 0;
    private int totalFindingsFiltered = 0;

    private Builder() {}

    public Builder issues(final List<ReviewResult.Issue> issues) {
      this.issues = issues;
      return this;
    }

    public Builder notes(final List<ReviewResult.Note> notes) {
      this.notes = notes;
      return this;
    }

    public Builder summary(final String summary) {
      this.summary = summary;
      return this;
    }

    public Builder findingCountsBySource(final Map<String, Integer> counts) {
      this.findingCountsBySource = counts;
      return this;
    }

    public Builder findingCountsBySeverity(final Map<String, Integer> counts) {
      this.findingCountsBySeverity = counts;
      return this;
    }

    public Builder overallConfidence(final double confidence) {
      this.overallConfidence = confidence;
      return this;
    }

    public Builder totalFindingsBeforeDedup(final int count) {
      this.totalFindingsBeforeDedup = count;
      return this;
    }

    public Builder totalFindingsAfterDedup(final int count) {
      this.totalFindingsAfterDedup = count;
      return this;
    }

    public Builder totalFindingsFiltered(final int count) {
      this.totalFindingsFiltered = count;
      return this;
    }

    public AggregatedFindings build() {
      return new AggregatedFindings(
          issues,
          notes,
          summary,
          findingCountsBySource,
          findingCountsBySeverity,
          overallConfidence,
          totalFindingsBeforeDedup,
          totalFindingsAfterDedup,
          totalFindingsFiltered);
    }
  }
}
