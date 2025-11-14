package com.ghiloufi.aicode.core.domain.service;

import com.ghiloufi.aicode.core.domain.model.DiffHunkBlock;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DiffLineValidator {

  public record ValidationResult(
      List<ReviewResult.Issue> validIssues,
      List<ReviewResult.Issue> invalidIssues,
      List<ReviewResult.Note> validNotes,
      List<ReviewResult.Note> invalidNotes) {}

  public ValidationResult validate(final GitDiffDocument diff, final ReviewResult result) {
    Objects.requireNonNull(diff, "GitDiffDocument cannot be null");
    Objects.requireNonNull(result, "ReviewResult cannot be null");

    final List<ReviewResult.Issue> validIssues = new ArrayList<>();
    final List<ReviewResult.Issue> invalidIssues = new ArrayList<>();
    final List<ReviewResult.Note> validNotes = new ArrayList<>();
    final List<ReviewResult.Note> invalidNotes = new ArrayList<>();

    for (final ReviewResult.Issue issue : result.issues) {
      if (isLineInDiff(diff, issue.file, issue.start_line)) {
        validIssues.add(issue);
      } else {
        invalidIssues.add(issue);
      }
    }

    for (final ReviewResult.Note note : result.non_blocking_notes) {
      if (isLineInDiff(diff, note.file, note.line)) {
        validNotes.add(note);
      } else {
        invalidNotes.add(note);
      }
    }

    return new ValidationResult(validIssues, invalidIssues, validNotes, invalidNotes);
  }

  public boolean isLineInDiff(
      final GitDiffDocument diff, final String filePath, final int lineNumber) {
    if (diff == null || filePath == null || filePath.isBlank() || lineNumber <= 0) {
      return false;
    }

    final GitFileModification file = findFileByPath(diff, filePath);
    if (file == null) {
      return false;
    }

    if (file.isDeleted()) {
      return false;
    }

    for (final DiffHunkBlock hunk : file.diffHunkBlocks) {
      final int hunkStartLine = hunk.newStart;
      final int hunkEndLine = hunk.newStart + hunk.newCount - 1;

      if (lineNumber >= hunkStartLine && lineNumber <= hunkEndLine) {
        return true;
      }
    }

    return false;
  }

  private GitFileModification findFileByPath(final GitDiffDocument diff, final String filePath) {
    for (final GitFileModification file : diff.files) {
      if (matchesFilePath(file, filePath)) {
        return file;
      }
    }
    return null;
  }

  private boolean matchesFilePath(final GitFileModification file, final String filePath) {
    return Objects.equals(file.newPath, filePath) || Objects.equals(file.oldPath, filePath);
  }
}
