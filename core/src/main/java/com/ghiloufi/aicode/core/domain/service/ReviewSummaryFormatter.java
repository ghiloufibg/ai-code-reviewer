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
    if (result.issues.isEmpty()) {
      summary.append("✅ Issues Found: 0\n");
    } else {
      summary.append(String.format("⚠️  Issues Found: %d\n", result.issues.size()));
      for (int i = 0; i < Math.min(MAX_ISSUES_TO_DISPLAY, result.issues.size()); i++) {
        final ReviewResult.Issue issue = result.issues.get(i);
        summary.append(
            String.format(
                "   %d. [%s] %s (%s:%d)\n",
                i + 1, issue.severity, issue.title, issue.file, issue.start_line));
      }
      if (result.issues.size() > MAX_ISSUES_TO_DISPLAY) {
        summary.append(
            String.format(
                "   ... and %d more issues\n", result.issues.size() - MAX_ISSUES_TO_DISPLAY));
      }
    }
  }

  private void appendNotesSection(final StringBuilder summary, final ReviewResult result) {
    summary.append(
        String.format("\n📝 Non-Blocking Notes: %d\n", result.non_blocking_notes.size()));
    for (int i = 0; i < Math.min(MAX_NOTES_TO_DISPLAY, result.non_blocking_notes.size()); i++) {
      final ReviewResult.Note note = result.non_blocking_notes.get(i);
      final String notePreview =
          note.note.length() > NOTE_PREVIEW_LENGTH
              ? note.note.substring(0, NOTE_PREVIEW_LENGTH) + "..."
              : note.note;
      summary.append(
          String.format("   %d. [%s:%d] %s\n", i + 1, note.file, note.line, notePreview));
    }
    if (result.non_blocking_notes.size() > MAX_NOTES_TO_DISPLAY) {
      summary.append(
          String.format(
              "   ... and %d more notes\n",
              result.non_blocking_notes.size() - MAX_NOTES_TO_DISPLAY));
    }
  }

  private void appendSummarySection(final StringBuilder summary, final ReviewResult result) {
    summary.append(String.format("\n📄 Summary: %d characters\n", result.summary.length()));
    final String summaryPreview =
        result.summary.length() > SUMMARY_PREVIEW_LENGTH
            ? result.summary.substring(0, SUMMARY_PREVIEW_LENGTH) + "..."
            : result.summary;
    summary.append("   ").append(summaryPreview.replace("\n", "\n   ")).append("\n");
  }
}
