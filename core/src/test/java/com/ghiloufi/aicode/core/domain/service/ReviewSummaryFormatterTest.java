package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.util.List;
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
      final ReviewResult result = ReviewResult.builder().summary("").build();

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
      final ReviewResult.Issue issue =
          ReviewResult.Issue.issueBuilder()
              .severity("HIGH")
              .file("Main.java")
              .startLine(42)
              .title("Security vulnerability")
              .build();

      final ReviewResult result =
          ReviewResult.builder().summary("Test summary").issues(List.of(issue)).build();

      final String summary = formatter.formatSummary(result);

      assertThat(summary).contains("⚠️  Issues Found: 1");
      assertThat(summary).contains("1. [HIGH] Security vulnerability (Main.java:42)");
      assertThat(summary).contains("📝 Non-Blocking Notes: 0");
    }

    @Test
    @DisplayName("should_format_result_with_notes_only")
    final void should_format_result_with_notes_only() {
      final ReviewResult.Note note =
          ReviewResult.Note.noteBuilder()
              .file("Helper.java")
              .line(15)
              .note("Consider refactoring")
              .build();

      final ReviewResult result =
          ReviewResult.builder().summary("Test summary").nonBlockingNotes(List.of(note)).build();

      final String summary = formatter.formatSummary(result);

      assertThat(summary).contains("✅ Issues Found: 0");
      assertThat(summary).contains("📝 Non-Blocking Notes: 1");
      assertThat(summary).contains("1. [Helper.java:15] Consider refactoring");
    }

    @Test
    @DisplayName("should_format_result_with_multiple_issues_and_notes")
    final void should_format_result_with_multiple_issues_and_notes() {
      final ReviewResult.Issue issue1 =
          ReviewResult.Issue.issueBuilder()
              .severity("HIGH")
              .file("Auth.java")
              .startLine(10)
              .title("SQL injection risk")
              .build();

      final ReviewResult.Issue issue2 =
          ReviewResult.Issue.issueBuilder()
              .severity("MEDIUM")
              .file("Cache.java")
              .startLine(25)
              .title("Performance bottleneck")
              .build();

      final ReviewResult.Note note =
          ReviewResult.Note.noteBuilder()
              .file("Utils.java")
              .line(5)
              .note("Code duplication")
              .build();

      final ReviewResult result =
          ReviewResult.builder()
              .summary("Complete review with issues and notes")
              .issues(List.of(issue1, issue2))
              .nonBlockingNotes(List.of(note))
              .build();

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
