package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewResultFormatter")
final class ReviewResultFormatterTest {

  private ReviewResultFormatter formatter;

  @BeforeEach
  final void setUp() {
    formatter = new ReviewResultFormatter();
  }

  @Nested
  @DisplayName("when formatting empty review result")
  final class EmptyReviewResult {

    @Test
    @DisplayName("should_return_no_issues_message_when_no_issues_notes_or_summary")
    final void should_return_no_issues_message_when_no_issues_notes_or_summary() {
      final ReviewResult reviewResult = ReviewResult.builder().build();

      final String formatted = formatter.format(reviewResult);

      assertThat(formatted).contains("## 🤖 AI Code Review");
      assertThat(formatted).contains("✅ No issues found in this code review.");
    }

    @Test
    @DisplayName("should_return_no_issues_message_when_summary_is_blank")
    final void should_return_no_issues_message_when_summary_is_blank() {
      final ReviewResult reviewResult = ReviewResult.builder().summary("   ").build();

      final String formatted = formatter.format(reviewResult);

      assertThat(formatted).contains("✅ No issues found in this code review.");
    }
  }

  @Nested
  @DisplayName("when formatting review result with summary")
  final class WithSummary {

    @Test
    @DisplayName("should_include_summary_when_present")
    final void should_include_summary_when_present() {
      final ReviewResult reviewResult =
          ReviewResult.builder().summary("Overall the code looks good").build();

      final String formatted = formatter.format(reviewResult);

      assertThat(formatted).contains("Overall the code looks good");
    }

    @Test
    @DisplayName("should_not_include_summary_when_null")
    final void should_not_include_summary_when_null() {
      final ReviewResult reviewResult = ReviewResult.builder().build();

      final String formatted = formatter.format(reviewResult);

      assertThat(formatted).doesNotContain("Overall");
    }
  }

  @Nested
  @DisplayName("when formatting review result with issues")
  final class WithIssues {

    @Test
    @DisplayName("should_include_issues_section_with_count")
    final void should_include_issues_section_with_count() {
      final ReviewResult.Issue issue1 =
          createIssue("CRITICAL", "Null pointer risk", "Main.java", 42, "Add null check");
      final ReviewResult.Issue issue2 =
          createIssue("WARNING", "Unused variable", "Utils.java", 15, null);

      final ReviewResult reviewResult =
          ReviewResult.builder().issues(List.of(issue1, issue2)).build();

      final String formatted = formatter.format(reviewResult);

      assertThat(formatted).contains("## 🔴 Issues Found (2)");
    }

    @Test
    @DisplayName("should_format_issue_with_all_fields")
    final void should_format_issue_with_all_fields() {
      final ReviewResult.Issue issue =
          createIssue("CRITICAL", "Null pointer risk", "Main.java", 42, "Add null check");

      final ReviewResult reviewResult = ReviewResult.builder().issues(List.of(issue)).build();

      final String formatted = formatter.format(reviewResult);

      assertThat(formatted).contains("### CRITICAL: Null pointer risk");
      assertThat(formatted).contains("**File:** `Main.java`");
      assertThat(formatted).contains("**Line:** 42");
      assertThat(formatted).contains("**Suggestion:** Add null check");
    }

    @Test
    @DisplayName("should_format_issue_without_suggestion")
    final void should_format_issue_without_suggestion() {
      final ReviewResult.Issue issue =
          createIssue("WARNING", "Unused variable", "Utils.java", 15, null);

      final ReviewResult reviewResult = ReviewResult.builder().issues(List.of(issue)).build();

      final String formatted = formatter.format(reviewResult);

      assertThat(formatted).contains("### WARNING: Unused variable");
      assertThat(formatted).doesNotContain("**Suggestion:**");
    }

    @Test
    @DisplayName("should_format_issue_with_empty_suggestion")
    final void should_format_issue_with_empty_suggestion() {
      final ReviewResult.Issue issue =
          createIssue("WARNING", "Code smell", "Service.java", 100, "");

      final ReviewResult reviewResult = ReviewResult.builder().issues(List.of(issue)).build();

      final String formatted = formatter.format(reviewResult);

      assertThat(formatted).doesNotContain("**Suggestion:**");
    }

    private ReviewResult.Issue createIssue(
        final String severity,
        final String title,
        final String file,
        final int line,
        final String suggestion) {
      return ReviewResult.Issue.issueBuilder()
          .severity(severity)
          .title(title)
          .file(file)
          .startLine(line)
          .suggestion(suggestion)
          .build();
    }
  }

  @Nested
  @DisplayName("when formatting review result with notes")
  final class WithNotes {

    @Test
    @DisplayName("should_include_notes_section_with_count")
    final void should_include_notes_section_with_count() {
      final ReviewResult.Note note1 = createNote("Consider adding logging");
      final ReviewResult.Note note2 = createNote("Great use of patterns");

      final ReviewResult reviewResult =
          ReviewResult.builder().nonBlockingNotes(List.of(note1, note2)).build();

      final String formatted = formatter.format(reviewResult);

      assertThat(formatted).contains("## 📝 Suggestions & Notes (2)");
    }

    @Test
    @DisplayName("should_format_notes_as_list")
    final void should_format_notes_as_list() {
      final ReviewResult.Note note1 = createNote("Consider adding logging");
      final ReviewResult.Note note2 = createNote("Great use of patterns");

      final ReviewResult reviewResult =
          ReviewResult.builder().nonBlockingNotes(List.of(note1, note2)).build();

      final String formatted = formatter.format(reviewResult);

      assertThat(formatted).contains("- Consider adding logging");
      assertThat(formatted).contains("- Great use of patterns");
    }

    private ReviewResult.Note createNote(final String noteText) {
      return ReviewResult.Note.noteBuilder().note(noteText).build();
    }
  }

  @Nested
  @DisplayName("when formatting complete review result")
  final class CompleteReviewResult {

    @Test
    @DisplayName("should_include_all_sections_in_correct_order")
    final void should_include_all_sections_in_correct_order() {
      final ReviewResult.Issue issue =
          createIssue("CRITICAL", "Security flaw", "Auth.java", 25, "Fix ASAP");
      final ReviewResult.Note note = createNote("Good code structure");

      final ReviewResult reviewResult =
          ReviewResult.builder()
              .summary("Code review complete")
              .issues(List.of(issue))
              .nonBlockingNotes(List.of(note))
              .build();

      final String formatted = formatter.format(reviewResult);

      final int summaryIndex = formatted.indexOf("Code review complete");
      final int issuesIndex = formatted.indexOf("## 🔴 Issues Found");
      final int notesIndex = formatted.indexOf("## 📝 Suggestions & Notes");

      assertThat(summaryIndex).isLessThan(issuesIndex);
      assertThat(issuesIndex).isLessThan(notesIndex);
    }

    private ReviewResult.Issue createIssue(
        final String severity,
        final String title,
        final String file,
        final int line,
        final String suggestion) {
      return ReviewResult.Issue.issueBuilder()
          .severity(severity)
          .title(title)
          .file(file)
          .startLine(line)
          .suggestion(suggestion)
          .build();
    }

    private ReviewResult.Note createNote(final String noteText) {
      return ReviewResult.Note.noteBuilder().note(noteText).build();
    }
  }
}
