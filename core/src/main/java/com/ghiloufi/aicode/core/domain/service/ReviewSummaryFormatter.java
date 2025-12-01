package com.ghiloufi.aicode.core.domain.service;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import org.springframework.stereotype.Service;

@Service
public final class ReviewSummaryFormatter {

  private static final int MAX_ISSUES_TO_DISPLAY = 3;
  private static final int MAX_NOTES_TO_DISPLAY = 5;
  private static final int NOTE_PREVIEW_LENGTH = 80;
  private static final int SUMMARY_PREVIEW_LENGTH = 200;
  private static final int SEPARATOR_LINE_LENGTH = 80;

  public String formatSummary(final ReviewResult result) {
    final StringBuilder summary = new StringBuilder();

    summary.append("\n").append("=".repeat(SEPARATOR_LINE_LENGTH)).append("\n");
    summary.append("📊 ACCUMULATED REVIEW SUMMARY\n");
    summary.append("=".repeat(SEPARATOR_LINE_LENGTH)).append("\n");

    appendIssuesSection(summary, result);
    appendNotesSection(summary, result);
    appendSummarySection(summary, result);

    summary.append("=".repeat(SEPARATOR_LINE_LENGTH)).append("\n");

    return summary.toString();
  }

  private void appendIssuesSection(final StringBuilder summary, final ReviewResult result) {
    if (result.getIssues().isEmpty()) {
      summary.append("✅ Issues Found: 0\n");
    } else {
      summary.append(String.format("⚠️  Issues Found: %d\n", result.getIssues().size()));
      for (int i = 0; i < Math.min(MAX_ISSUES_TO_DISPLAY, result.getIssues().size()); i++) {
        final ReviewResult.Issue issue = result.getIssues().get(i);
        summary.append(
            String.format(
                "   %d. [%s] %s (%s:%d)\n",
                i + 1,
                issue.getSeverity(),
                issue.getTitle(),
                issue.getFile(),
                issue.getStartLine()));
      }
      if (result.getIssues().size() > MAX_ISSUES_TO_DISPLAY) {
        summary.append(
            String.format(
                "   ... and %d more issues\n", result.getIssues().size() - MAX_ISSUES_TO_DISPLAY));
      }
    }
  }

  private void appendNotesSection(final StringBuilder summary, final ReviewResult result) {
    summary.append(
        String.format("\n📝 Non-Blocking Notes: %d\n", result.getNonBlockingNotes().size()));
    for (int i = 0; i < Math.min(MAX_NOTES_TO_DISPLAY, result.getNonBlockingNotes().size()); i++) {
      final ReviewResult.Note note = result.getNonBlockingNotes().get(i);
      final String notePreview =
          note.getNote().length() > NOTE_PREVIEW_LENGTH
              ? note.getNote().substring(0, NOTE_PREVIEW_LENGTH) + "..."
              : note.getNote();
      summary.append(
          String.format("   %d. [%s:%d] %s\n", i + 1, note.getFile(), note.getLine(), notePreview));
    }
    if (result.getNonBlockingNotes().size() > MAX_NOTES_TO_DISPLAY) {
      summary.append(
          String.format(
              "   ... and %d more notes\n",
              result.getNonBlockingNotes().size() - MAX_NOTES_TO_DISPLAY));
    }
  }

  private void appendSummarySection(final StringBuilder summary, final ReviewResult result) {
    final String resultSummary = result.getSummary() != null ? result.getSummary() : "";
    summary.append(String.format("\n📄 Summary: %d characters\n", resultSummary.length()));
    final String summaryPreview =
        resultSummary.length() > SUMMARY_PREVIEW_LENGTH
            ? resultSummary.substring(0, SUMMARY_PREVIEW_LENGTH) + "..."
            : resultSummary;
    summary.append("   ").append(summaryPreview.replace("\n", "\n   ")).append("\n");
  }
}
