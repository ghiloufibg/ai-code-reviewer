package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.config.SummaryCommentProperties;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SummaryCommentFormatterTest {

  private SummaryCommentFormatter createFormatter(
      final boolean includeStatistics, final boolean includeSeverityBreakdown) {
    final SummaryCommentProperties config =
        new SummaryCommentProperties(true, includeStatistics, includeSeverityBreakdown);
    return new SummaryCommentFormatter(config);
  }

  @Test
  void should_format_summary_with_header() {
    final SummaryCommentFormatter formatter = createFormatter(true, true);
    final ReviewResult result = createReviewWithSummary("Test summary");

    final String formatted = formatter.formatSummaryComment(result);

    assertThat(formatted).contains("## 📊 AI Code Review Summary");
    assertThat(formatted).contains("Test summary");
  }

  @Test
  void should_return_no_summary_message_when_summary_is_null() {
    final SummaryCommentFormatter formatter = createFormatter(true, true);
    final ReviewResult result = ReviewResult.builder().summary(null).build();

    final String formatted = formatter.formatSummaryComment(result);

    assertThat(formatted).contains("## 📊 AI Code Review Summary");
    assertThat(formatted).contains("No summary available for this review");
  }

  @Test
  void should_return_no_summary_message_when_summary_is_blank() {
    final SummaryCommentFormatter formatter = createFormatter(true, true);
    final ReviewResult result = ReviewResult.builder().summary("   ").build();

    final String formatted = formatter.formatSummaryComment(result);

    assertThat(formatted).contains("No summary available for this review");
  }

  @Test
  void should_include_statistics_when_enabled() {
    final SummaryCommentFormatter formatter = createFormatter(true, true);
    final ReviewResult result = createReviewWithIssuesAndNotes();

    final String formatted = formatter.formatSummaryComment(result);

    assertThat(formatted).contains("### 📈 Review Statistics");
    assertThat(formatted).contains("- **Issues Found**: 3");
    assertThat(formatted).contains("- **Suggestions**: 2");
    assertThat(formatted).contains("- **Files Analyzed**:");
  }

  @Test
  void should_exclude_statistics_when_disabled() {
    final SummaryCommentFormatter formatter = createFormatter(false, true);
    final ReviewResult result = createReviewWithIssuesAndNotes();

    final String formatted = formatter.formatSummaryComment(result);

    assertThat(formatted).doesNotContain("### 📈 Review Statistics");
    assertThat(formatted).doesNotContain("Issues Found");
  }

  @Test
  void should_include_severity_breakdown_when_enabled() {
    final SummaryCommentFormatter formatter = createFormatter(true, true);
    final ReviewResult result = createReviewWithSeverityVariety();

    final String formatted = formatter.formatSummaryComment(result);

    assertThat(formatted).contains("### ⚠️ Severity Breakdown");
    assertThat(formatted).contains("- **CRITICAL**: 1");
    assertThat(formatted).contains("- **HIGH**: 1");
    assertThat(formatted).contains("- **MEDIUM**: 1");
  }

  @Test
  void should_exclude_severity_breakdown_when_disabled() {
    final SummaryCommentFormatter formatter = createFormatter(true, false);
    final ReviewResult result = createReviewWithSeverityVariety();

    final String formatted = formatter.formatSummaryComment(result);

    assertThat(formatted).doesNotContain("### ⚠️ Severity Breakdown");
    assertThat(formatted).doesNotContain("CRITICAL");
  }

  @Test
  void should_not_include_severity_breakdown_when_no_issues() {
    final SummaryCommentFormatter formatter = createFormatter(true, true);
    final ReviewResult result = createReviewWithSummary("Summary only");

    final String formatted = formatter.formatSummaryComment(result);

    assertThat(formatted).doesNotContain("### ⚠️ Severity Breakdown");
  }

  @Test
  void should_include_footer_with_inline_comment_reference() {
    final SummaryCommentFormatter formatter = createFormatter(true, true);
    final ReviewResult result = createReviewWithSummary("Test summary");

    final String formatted = formatter.formatSummaryComment(result);

    assertThat(formatted).contains("---");
    assertThat(formatted)
        .contains("*Detailed findings are available in the inline comments below.*");
  }

  @Test
  void should_count_unique_files_correctly() {
    final SummaryCommentFormatter formatter = createFormatter(true, true);

    final ReviewResult.Issue issue1 =
        ReviewResult.Issue.issueBuilder().file("FileA.java").severity("HIGH").build();

    final ReviewResult.Issue issue2 =
        ReviewResult.Issue.issueBuilder().file("FileA.java").severity("MEDIUM").build();

    final ReviewResult.Issue issue3 =
        ReviewResult.Issue.issueBuilder().file("FileB.java").severity("LOW").build();

    final ReviewResult.Note note1 = ReviewResult.Note.noteBuilder().file("FileC.java").build();

    final ReviewResult result =
        ReviewResult.builder()
            .summary("Test")
            .issues(List.of(issue1, issue2, issue3))
            .nonBlockingNotes(List.of(note1))
            .build();

    final String formatted = formatter.formatSummaryComment(result);

    assertThat(formatted).contains("- **Files Analyzed**: 3");
  }

  private ReviewResult createReviewWithSummary(final String summary) {
    return ReviewResult.builder().summary(summary).build();
  }

  private ReviewResult createReviewWithIssuesAndNotes() {
    final ReviewResult.Issue issue1 =
        ReviewResult.Issue.issueBuilder().file("File1.java").severity("HIGH").build();

    final ReviewResult.Issue issue2 =
        ReviewResult.Issue.issueBuilder().file("File2.java").severity("MEDIUM").build();

    final ReviewResult.Issue issue3 =
        ReviewResult.Issue.issueBuilder().file("File3.java").severity("LOW").build();

    final ReviewResult.Note note1 = ReviewResult.Note.noteBuilder().file("File4.java").build();

    final ReviewResult.Note note2 = ReviewResult.Note.noteBuilder().file("File5.java").build();

    return ReviewResult.builder()
        .summary("Test summary")
        .issues(List.of(issue1, issue2, issue3))
        .nonBlockingNotes(List.of(note1, note2))
        .build();
  }

  private ReviewResult createReviewWithSeverityVariety() {
    final ReviewResult.Issue critical =
        ReviewResult.Issue.issueBuilder().file("Critical.java").severity("CRITICAL").build();

    final ReviewResult.Issue high =
        ReviewResult.Issue.issueBuilder().file("High.java").severity("HIGH").build();

    final ReviewResult.Issue medium =
        ReviewResult.Issue.issueBuilder().file("Medium.java").severity("MEDIUM").build();

    return ReviewResult.builder()
        .summary("Test summary with severity variety")
        .issues(List.of(critical, high, medium))
        .build();
  }
}
