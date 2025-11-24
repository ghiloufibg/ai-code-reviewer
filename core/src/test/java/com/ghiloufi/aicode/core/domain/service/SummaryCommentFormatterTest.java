package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.config.SummaryCommentProperties;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
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
    final ReviewResult result = new ReviewResult();
    result.summary = null;

    final String formatted = formatter.formatSummaryComment(result);

    assertThat(formatted).contains("## 📊 AI Code Review Summary");
    assertThat(formatted).contains("No summary available for this review");
  }

  @Test
  void should_return_no_summary_message_when_summary_is_blank() {
    final SummaryCommentFormatter formatter = createFormatter(true, true);
    final ReviewResult result = new ReviewResult();
    result.summary = "   ";

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
    final ReviewResult result = new ReviewResult();
    result.summary = "Test";

    final ReviewResult.Issue issue1 = new ReviewResult.Issue();
    issue1.file = "FileA.java";
    issue1.severity = "HIGH";

    final ReviewResult.Issue issue2 = new ReviewResult.Issue();
    issue2.file = "FileA.java";
    issue2.severity = "MEDIUM";

    final ReviewResult.Issue issue3 = new ReviewResult.Issue();
    issue3.file = "FileB.java";
    issue3.severity = "LOW";

    final ReviewResult.Note note1 = new ReviewResult.Note();
    note1.file = "FileC.java";

    result.issues.add(issue1);
    result.issues.add(issue2);
    result.issues.add(issue3);
    result.non_blocking_notes.add(note1);

    final String formatted = formatter.formatSummaryComment(result);

    assertThat(formatted).contains("- **Files Analyzed**: 3");
  }

  private ReviewResult createReviewWithSummary(final String summary) {
    final ReviewResult result = new ReviewResult();
    result.summary = summary;
    return result;
  }

  private ReviewResult createReviewWithIssuesAndNotes() {
    final ReviewResult result = new ReviewResult();
    result.summary = "Test summary";

    final ReviewResult.Issue issue1 = new ReviewResult.Issue();
    issue1.file = "File1.java";
    issue1.severity = "HIGH";

    final ReviewResult.Issue issue2 = new ReviewResult.Issue();
    issue2.file = "File2.java";
    issue2.severity = "MEDIUM";

    final ReviewResult.Issue issue3 = new ReviewResult.Issue();
    issue3.file = "File3.java";
    issue3.severity = "LOW";

    final ReviewResult.Note note1 = new ReviewResult.Note();
    note1.file = "File4.java";

    final ReviewResult.Note note2 = new ReviewResult.Note();
    note2.file = "File5.java";

    result.issues.add(issue1);
    result.issues.add(issue2);
    result.issues.add(issue3);
    result.non_blocking_notes.add(note1);
    result.non_blocking_notes.add(note2);

    return result;
  }

  private ReviewResult createReviewWithSeverityVariety() {
    final ReviewResult result = new ReviewResult();
    result.summary = "Test summary with severity variety";

    final ReviewResult.Issue critical = new ReviewResult.Issue();
    critical.file = "Critical.java";
    critical.severity = "CRITICAL";

    final ReviewResult.Issue high = new ReviewResult.Issue();
    high.file = "High.java";
    high.severity = "HIGH";

    final ReviewResult.Issue medium = new ReviewResult.Issue();
    medium.file = "Medium.java";
    medium.severity = "MEDIUM";

    result.issues.add(critical);
    result.issues.add(high);
    result.issues.add(medium);

    return result;
  }
}
