package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewSummaryFormatter")
final class ReviewSummaryFormatterTest {

  private ReviewSummaryFormatter formatter;

  @BeforeEach
  final void setUp() {
    formatter = new ReviewSummaryFormatter();
  }

  @Nested
  @DisplayName("when formatting review summary")
  final class FormatSummary {

    @Test
    @DisplayName("should_format_empty_result")
    final void should_format_empty_result() {
      final ReviewResult result = new ReviewResult();
      result.issues = new ArrayList<>();
      result.non_blocking_notes = new ArrayList<>();
      result.summary = "";

      final String summary = formatter.formatSummary(result);

      assertThat(summary).contains("📊 ACCUMULATED REVIEW SUMMARY");
      assertThat(summary).contains("✅ Issues Found: 0");
      assertThat(summary).contains("📝 Non-Blocking Notes: 0");
      assertThat(summary).contains("📄 Summary: 0 characters");
      assertThat(summary).contains("=".repeat(80));
    }

    @Test
    @DisplayName("should_format_result_with_issues_only")
    final void should_format_result_with_issues_only() {
      final ReviewResult result = new ReviewResult();
      result.issues = new ArrayList<>();
      result.non_blocking_notes = new ArrayList<>();
      result.summary = "Test summary";

      final ReviewResult.Issue issue = new ReviewResult.Issue();
      issue.severity = "HIGH";
      issue.file = "Main.java";
      issue.start_line = 42;
      issue.title = "Security vulnerability";
      result.issues.add(issue);

      final String summary = formatter.formatSummary(result);

      assertThat(summary).contains("⚠️  Issues Found: 1");
      assertThat(summary).contains("1. [HIGH] Security vulnerability (Main.java:42)");
      assertThat(summary).contains("📝 Non-Blocking Notes: 0");
    }

    @Test
    @DisplayName("should_format_result_with_notes_only")
    final void should_format_result_with_notes_only() {
      final ReviewResult result = new ReviewResult();
      result.issues = new ArrayList<>();
      result.non_blocking_notes = new ArrayList<>();
      result.summary = "Test summary";

      final ReviewResult.Note note = new ReviewResult.Note();
      note.file = "Helper.java";
      note.line = 15;
      note.note = "Consider refactoring";
      result.non_blocking_notes.add(note);

      final String summary = formatter.formatSummary(result);

      assertThat(summary).contains("✅ Issues Found: 0");
      assertThat(summary).contains("📝 Non-Blocking Notes: 1");
      assertThat(summary).contains("1. [Helper.java:15] Consider refactoring");
    }

    @Test
    @DisplayName("should_format_result_with_multiple_issues_and_notes")
    final void should_format_result_with_multiple_issues_and_notes() {
      final ReviewResult result = new ReviewResult();
      result.issues = new ArrayList<>();
      result.non_blocking_notes = new ArrayList<>();
      result.summary = "Complete review with issues and notes";

      final ReviewResult.Issue issue1 = new ReviewResult.Issue();
      issue1.severity = "HIGH";
      issue1.file = "Auth.java";
      issue1.start_line = 10;
      issue1.title = "SQL injection risk";
      result.issues.add(issue1);

      final ReviewResult.Issue issue2 = new ReviewResult.Issue();
      issue2.severity = "MEDIUM";
      issue2.file = "Cache.java";
      issue2.start_line = 25;
      issue2.title = "Performance bottleneck";
      result.issues.add(issue2);

      final ReviewResult.Note note = new ReviewResult.Note();
      note.file = "Utils.java";
      note.line = 5;
      note.note = "Code duplication";
      result.non_blocking_notes.add(note);

      final String summary = formatter.formatSummary(result);

      assertThat(summary).contains("⚠️  Issues Found: 2");
      assertThat(summary).contains("1. [HIGH] SQL injection risk (Auth.java:10)");
      assertThat(summary).contains("2. [MEDIUM] Performance bottleneck (Cache.java:25)");
      assertThat(summary).contains("📝 Non-Blocking Notes: 1");
      assertThat(summary).contains("1. [Utils.java:5] Code duplication");
      assertThat(summary).contains("📄 Summary: 37 characters");
    }
  }
}
