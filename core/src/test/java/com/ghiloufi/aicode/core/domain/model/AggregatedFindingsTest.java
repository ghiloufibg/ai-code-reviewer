package com.ghiloufi.aicode.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class AggregatedFindingsTest {

  private static ReviewResult.Issue createIssue(
      final String file, final String severity, final String title) {
    return ReviewResult.Issue.issueBuilder()
        .file(file)
        .startLine(1)
        .severity(severity)
        .title(title)
        .suggestion("Fix it")
        .build();
  }

  private static ReviewResult.Note createNote(final String file) {
    return ReviewResult.Note.noteBuilder().file(file).line(1).note("Note").build();
  }

  @Test
  void should_create_empty_aggregated_findings() {
    final var empty = AggregatedFindings.empty();

    assertThat(empty.issues()).isEmpty();
    assertThat(empty.notes()).isEmpty();
    assertThat(empty.summary()).isEqualTo("No findings");
    assertThat(empty.findingCountsBySource()).isEmpty();
    assertThat(empty.findingCountsBySeverity()).isEmpty();
    assertThat(empty.overallConfidence()).isEqualTo(1.0);
    assertThat(empty.totalFindingsBeforeDedup()).isZero();
    assertThat(empty.totalFindingsAfterDedup()).isZero();
    assertThat(empty.totalFindingsFiltered()).isZero();
  }

  @Test
  void should_build_aggregated_findings() {
    final var issues = List.of(createIssue("file1.java", "error", "Bug"));
    final var notes = List.of(createNote("file2.java"));

    final var findings =
        AggregatedFindings.builder()
            .issues(issues)
            .notes(notes)
            .summary("Found 1 issue and 1 note")
            .findingCountsBySource(Map.of("checkstyle", 1))
            .findingCountsBySeverity(Map.of("error", 1))
            .overallConfidence(0.85)
            .totalFindingsBeforeDedup(10)
            .totalFindingsAfterDedup(8)
            .totalFindingsFiltered(5)
            .build();

    assertThat(findings.issues()).hasSize(1);
    assertThat(findings.notes()).hasSize(1);
    assertThat(findings.summary()).isEqualTo("Found 1 issue and 1 note");
    assertThat(findings.findingCountsBySource()).containsEntry("checkstyle", 1);
    assertThat(findings.findingCountsBySeverity()).containsEntry("error", 1);
    assertThat(findings.overallConfidence()).isEqualTo(0.85);
    assertThat(findings.totalFindingsBeforeDedup()).isEqualTo(10);
    assertThat(findings.totalFindingsAfterDedup()).isEqualTo(8);
    assertThat(findings.totalFindingsFiltered()).isEqualTo(5);
  }

  @Test
  void should_throw_when_summary_is_null() {
    assertThatThrownBy(
            () ->
                new AggregatedFindings(
                    List.of(), List.of(), null, Map.of(), Map.of(), 1.0, 0, 0, 0))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("summary");
  }

  @Test
  void should_throw_when_confidence_out_of_range() {
    assertThatThrownBy(
            () ->
                new AggregatedFindings(List.of(), List.of(), "s", Map.of(), Map.of(), 1.5, 0, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("overallConfidence");

    assertThatThrownBy(
            () ->
                new AggregatedFindings(
                    List.of(), List.of(), "s", Map.of(), Map.of(), -0.1, 0, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("overallConfidence");
  }

  @Test
  void should_handle_null_lists() {
    final var findings = new AggregatedFindings(null, null, "summary", null, null, 1.0, 0, 0, 0);

    assertThat(findings.issues()).isEmpty();
    assertThat(findings.notes()).isEmpty();
    assertThat(findings.findingCountsBySource()).isEmpty();
    assertThat(findings.findingCountsBySeverity()).isEmpty();
  }

  @Test
  void should_count_issues_and_notes() {
    final var findings =
        AggregatedFindings.builder()
            .issues(List.of(createIssue("f1", "error", "t1"), createIssue("f2", "warning", "t2")))
            .notes(List.of(createNote("f3")))
            .summary("s")
            .build();

    assertThat(findings.totalIssueCount()).isEqualTo(2);
    assertThat(findings.totalNoteCount()).isEqualTo(1);
    assertThat(findings.totalFindingsCount()).isEqualTo(3);
  }

  @Test
  void should_calculate_deduplicated_count() {
    final var findings =
        AggregatedFindings.builder()
            .summary("s")
            .totalFindingsBeforeDedup(100)
            .totalFindingsAfterDedup(85)
            .build();

    assertThat(findings.deduplicatedCount()).isEqualTo(15);
  }

  @Test
  void should_detect_high_severity_issues() {
    final var withError =
        AggregatedFindings.builder()
            .issues(List.of(createIssue("f", "error", "t")))
            .summary("s")
            .build();

    final var withCritical =
        AggregatedFindings.builder()
            .issues(List.of(createIssue("f", "critical", "t")))
            .summary("s")
            .build();

    final var withWarning =
        AggregatedFindings.builder()
            .issues(List.of(createIssue("f", "warning", "t")))
            .summary("s")
            .build();

    assertThat(withError.hasHighSeverityIssues()).isTrue();
    assertThat(withCritical.hasHighSeverityIssues()).isTrue();
    assertThat(withWarning.hasHighSeverityIssues()).isFalse();
  }

  @Test
  void should_detect_security_issues() {
    final var withSecurity =
        AggregatedFindings.builder()
            .issues(List.of(createIssue("f", "error", "Security: CVE-2021-1234")))
            .summary("s")
            .build();

    final var withoutSecurity =
        AggregatedFindings.builder()
            .issues(List.of(createIssue("f", "error", "Bug: null pointer")))
            .summary("s")
            .build();

    assertThat(withSecurity.hasSecurityIssues()).isTrue();
    assertThat(withoutSecurity.hasSecurityIssues()).isFalse();
  }

  @Test
  void should_filter_issues_by_file() {
    final var findings =
        AggregatedFindings.builder()
            .issues(
                List.of(
                    createIssue("File1.java", "error", "t1"),
                    createIssue("File2.java", "error", "t2"),
                    createIssue("File1.java", "warning", "t3")))
            .summary("s")
            .build();

    assertThat(findings.issuesByFile("File1.java")).hasSize(2);
    assertThat(findings.issuesByFile("File2.java")).hasSize(1);
    assertThat(findings.issuesByFile("File3.java")).isEmpty();
  }

  @Test
  void should_filter_issues_by_severity() {
    final var findings =
        AggregatedFindings.builder()
            .issues(
                List.of(
                    createIssue("f1", "error", "t1"),
                    createIssue("f2", "warning", "t2"),
                    createIssue("f3", "error", "t3")))
            .summary("s")
            .build();

    assertThat(findings.issuesBySeverity("error")).hasSize(2);
    assertThat(findings.issuesBySeverity("warning")).hasSize(1);
    assertThat(findings.issuesBySeverity("info")).isEmpty();
  }

  @Test
  void should_convert_to_review_result() {
    final var issues =
        List.of(
            createIssue("File1.java", "error", "t1"), createIssue("File2.java", "warning", "t2"));
    final var notes = List.of(createNote("File3.java"));

    final var findings =
        AggregatedFindings.builder()
            .issues(issues)
            .notes(notes)
            .summary("Found 2 issues and 1 note")
            .build();

    final var result = findings.toReviewResult("openai", "gpt-4o");

    assertThat(result.getSummary()).isEqualTo("Found 2 issues and 1 note");
    assertThat(result.getIssues()).hasSize(2);
    assertThat(result.getNonBlockingNotes()).hasSize(1);
    assertThat(result.getLlmProvider()).isEqualTo("openai");
    assertThat(result.getLlmModel()).isEqualTo("gpt-4o");
    assertThat(result.getFilesAnalyzed()).isEqualTo(2);
  }

  @Test
  void should_make_defensive_copies() {
    final var mutableIssues = new ArrayList<>(List.of(createIssue("f", "error", "t")));
    final var mutableNotes = new ArrayList<>(List.of(createNote("f")));
    final var mutableSourceCounts = new HashMap<>(Map.of("tool", 1));
    final var mutableSeverityCounts = new HashMap<>(Map.of("error", 1));

    final var findings =
        new AggregatedFindings(
            mutableIssues,
            mutableNotes,
            "s",
            mutableSourceCounts,
            mutableSeverityCounts,
            1.0,
            0,
            0,
            0);

    mutableIssues.clear();
    mutableNotes.clear();
    mutableSourceCounts.clear();
    mutableSeverityCounts.clear();

    assertThat(findings.issues()).hasSize(1);
    assertThat(findings.notes()).hasSize(1);
    assertThat(findings.findingCountsBySource()).containsEntry("tool", 1);
    assertThat(findings.findingCountsBySeverity()).containsEntry("error", 1);
  }
}
