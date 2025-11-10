package com.ghiloufi.aicode.core.domain.service;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import org.springframework.stereotype.Service;

@Service
public final class ReviewResultFormatter {

  private static final String HEADER = "## 🤖 AI Code Review\n\n";
  private static final String NO_ISSUES_MESSAGE = "✅ No issues found in this code review.\n";
  private static final String ISSUES_SECTION_TEMPLATE = "## 🔴 Issues Found (%d)\n\n";
  private static final String NOTES_SECTION_TEMPLATE = "## 📝 Suggestions & Notes (%d)\n\n";

  public final String format(final ReviewResult reviewResult) {
    final StringBuilder builder = new StringBuilder();
    builder.append(HEADER);

    appendSummary(builder, reviewResult);
    appendIssues(builder, reviewResult);
    appendNotes(builder, reviewResult);
    appendNoIssuesMessageIfNeeded(builder, reviewResult);

    return builder.toString();
  }

  private void appendSummary(final StringBuilder builder, final ReviewResult reviewResult) {
    if (hasSummary(reviewResult)) {
      builder.append(reviewResult.summary).append("\n\n");
    }
  }

  private void appendIssues(final StringBuilder builder, final ReviewResult reviewResult) {
    if (hasIssues(reviewResult)) {
      builder.append(String.format(ISSUES_SECTION_TEMPLATE, reviewResult.issues.size()));

      for (final ReviewResult.Issue issue : reviewResult.issues) {
        appendIssue(builder, issue);
      }
    }
  }

  private void appendIssue(final StringBuilder builder, final ReviewResult.Issue issue) {
    builder.append("### ").append(issue.severity).append(": ").append(issue.title).append("\n");
    builder.append("**File:** `").append(issue.file).append("`\n");
    builder.append("**Line:** ").append(issue.start_line).append("\n");

    if (hasSuggestion(issue)) {
      builder.append("**Suggestion:** ").append(issue.suggestion).append("\n");
    }

    builder.append("\n");
  }

  private void appendNotes(final StringBuilder builder, final ReviewResult reviewResult) {
    if (hasNotes(reviewResult)) {
      builder.append(String.format(NOTES_SECTION_TEMPLATE, reviewResult.non_blocking_notes.size()));

      for (final ReviewResult.Note note : reviewResult.non_blocking_notes) {
        builder.append("- ").append(note.note).append("\n");
      }
    }
  }

  private void appendNoIssuesMessageIfNeeded(
      final StringBuilder builder, final ReviewResult reviewResult) {
    if (isEmpty(reviewResult)) {
      builder.append(NO_ISSUES_MESSAGE);
    }
  }

  private boolean hasSummary(final ReviewResult reviewResult) {
    return reviewResult.summary != null && !reviewResult.summary.isBlank();
  }

  private boolean hasIssues(final ReviewResult reviewResult) {
    return !reviewResult.issues.isEmpty();
  }

  private boolean hasNotes(final ReviewResult reviewResult) {
    return !reviewResult.non_blocking_notes.isEmpty();
  }

  private boolean hasSuggestion(final ReviewResult.Issue issue) {
    return issue.suggestion != null && !issue.suggestion.isEmpty();
  }

  private boolean isEmpty(final ReviewResult reviewResult) {
    return !hasIssues(reviewResult) && !hasNotes(reviewResult) && !hasSummary(reviewResult);
  }
}
