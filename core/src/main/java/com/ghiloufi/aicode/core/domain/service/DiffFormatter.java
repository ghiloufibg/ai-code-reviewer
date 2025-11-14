package com.ghiloufi.aicode.core.domain.service;

import com.ghiloufi.aicode.core.domain.model.DiffHunkBlock;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DiffFormatter {

  private static final String FILE_SEPARATOR = "=".repeat(80);
  private static final String HUNK_SEPARATOR = "─".repeat(64);

  public String formatDiff(final GitDiffDocument diff) {
    Objects.requireNonNull(diff, "GitDiffDocument cannot be null");

    if (diff.isEmpty()) {
      return "No changes in diff";
    }

    final StringBuilder result = new StringBuilder();

    for (final GitFileModification file : diff.files) {
      appendFileSeparator(result);
      appendFileHeader(result, file);
      appendFileSeparator(result);
      result.append("\n");

      int hunkIndex = 1;
      for (final DiffHunkBlock hunk : file.diffHunkBlocks) {
        appendHunkHeader(result, hunkIndex, hunk);
        appendHunkSeparator(result);
        result.append("\n");
        appendHunkLines(result, hunk);
        result.append("\n");
        hunkIndex++;
      }
    }

    return result.toString();
  }

  private void appendFileSeparator(final StringBuilder result) {
    result.append(FILE_SEPARATOR).append("\n");
  }

  private void appendFileHeader(final StringBuilder result, final GitFileModification file) {
    result.append("FILE: ").append(file.getEffectivePath());

    if (file.isNewFile()) {
      result.append(" (NEW FILE)");
    } else if (file.isDeleted()) {
      result.append(" (DELETED)");
    } else if (file.isRenamed()) {
      result.append(" (RENAMED FROM ").append(file.oldPath).append(")");
    } else {
      result.append(" (MODIFIED)");
    }

    result.append("\n");
  }

  private void appendHunkHeader(
      final StringBuilder result, final int hunkIndex, final DiffHunkBlock hunk) {
    final int startLine = hunk.newStart;
    final int endLine = hunk.newStart + hunk.newCount - 1;
    result.append("\nHunk ").append(hunkIndex).append(": Lines ").append(startLine);
    if (endLine > startLine) {
      result.append("-").append(endLine);
    }
    result.append("\n");
  }

  private void appendHunkSeparator(final StringBuilder result) {
    result.append(HUNK_SEPARATOR).append("\n");
  }

  private void appendHunkLines(final StringBuilder result, final DiffHunkBlock hunk) {
    int currentLineNumber = hunk.newStart;

    for (final String line : hunk.lines) {
      if (line.startsWith("+")) {
        result
            .append(String.format("%-4d", currentLineNumber))
            .append(" │ + ")
            .append(line.substring(1))
            .append("\n");
        currentLineNumber++;
      } else if (line.startsWith("-")) {
        result.append("     │ - ").append(line.substring(1)).append("\n");
      } else {
        final String content = line.startsWith(" ") ? line.substring(1) : line;
        result
            .append(String.format("%-4d", currentLineNumber))
            .append(" │   ")
            .append(content)
            .append("\n");
        currentLineNumber++;
      }
    }
  }
}
