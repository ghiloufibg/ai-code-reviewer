package com.ghiloufi.aicode.core.domain.service;

import com.ghiloufi.aicode.core.config.SummaryCommentProperties;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public final class SummaryCommentFormatter {

  private static final String HEADER = "## 📊 AI Code Review Summary\n\n";
  private static final String NO_SUMMARY_MESSAGE = "No summary available for this review.\n";
  private static final String FOOTER =
      "\n---\n\n*Detailed findings are available in the inline comments below.*\n";

  private final SummaryCommentProperties config;

  public SummaryCommentFormatter(final SummaryCommentProperties config) {
    this.config = config;
  }

  public String formatSummaryComment(final ReviewResult reviewResult) {
    return formatSummaryComment(reviewResult, reviewResult.getFilesAnalyzed());
  }

  public String formatSummaryComment(final ReviewResult reviewResult, final int totalFilesInDiff) {
    final StringBuilder comment = new StringBuilder();

    comment.append(HEADER);

    final String summary = reviewResult.getSummary();
    if (summary == null || summary.isBlank()) {
      comment.append(NO_SUMMARY_MESSAGE);
    } else {
      comment.append(summary).append("\n\n");
    }

    if (config.isIncludeStatistics()) {
      appendStatistics(comment, reviewResult, totalFilesInDiff);
    }

    if (config.isIncludeSeverityBreakdown() && !reviewResult.getIssues().isEmpty()) {
      appendSeverityBreakdown(comment, reviewResult);
    }

    comment.append(FOOTER);

    return comment.toString();
  }

  private void appendStatistics(
      final StringBuilder comment, final ReviewResult reviewResult, final int totalFilesInDiff) {
    final int issueCount = reviewResult.getIssues().size();
    final int suggestionCount = reviewResult.getNonBlockingNotes().size();
    final int fileCount =
        totalFilesInDiff > 0 ? totalFilesInDiff : countUniqueFilesFromIssues(reviewResult);

    comment.append("### 📈 Review Statistics\n\n");
    comment.append("- **Issues Found**: ").append(issueCount).append("\n");
    comment.append("- **Suggestions**: ").append(suggestionCount).append("\n");
    comment.append("- **Files Analyzed**: ").append(fileCount).append("\n\n");
  }

  private void appendSeverityBreakdown(
      final StringBuilder comment, final ReviewResult reviewResult) {
    final Map<String, Long> severityCounts =
        reviewResult.getIssues().stream()
            .collect(Collectors.groupingBy(issue -> issue.getSeverity(), Collectors.counting()));

    comment.append("### ⚠️ Severity Breakdown\n\n");
    severityCounts.forEach(
        (severity, count) -> {
          comment.append("- **").append(severity).append("**: ").append(count).append("\n");
        });
    comment.append("\n");
  }

  private int countUniqueFilesFromIssues(final ReviewResult reviewResult) {
    final Set<String> uniqueFiles = new HashSet<>();
    reviewResult.getIssues().forEach(issue -> uniqueFiles.add(issue.getFile()));
    reviewResult.getNonBlockingNotes().forEach(note -> uniqueFiles.add(note.getFile()));
    return uniqueFiles.size();
  }
}
