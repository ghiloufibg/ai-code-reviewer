package com.ghiloufi.aicode.core;

import com.ghiloufi.aicode.domain.DiffHunkBlock;
import com.ghiloufi.aicode.domain.GitDiffDocument;
import com.ghiloufi.aicode.domain.GitFileModification;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Component;

/**
 * Parser for unified diff format (git diff output). Converts unified diff strings into structured
 * domain objects.
 */
@Component
public class UnifiedDiffParser {

  /**
   * Parses a unified diff string into a structured UnifiedDiff object.
   *
   * @param diff the unified diff string to parse (must not be blank)
   * @return parsed UnifiedDiff containing files, hunks, and line changes
   */
  public GitDiffDocument parse(@NotBlank String diff) {

    GitDiffDocument gitDiffDocument = new GitDiffDocument();

    String[] lines = diff.lines().toArray(String[]::new);

    GitFileModification currentFile = null;
    DiffHunkBlock currentDiffHunkBlock = null;

    for (String line : lines) {
      if (isOldFileHeader(line)) {
        currentFile = handleOldFileHeader(line);
      } else if (isNewFileHeader(line)) {
        currentFile = handleNewFileHeader(line, currentFile, gitDiffDocument);
      } else if (isHunkHeader(line)) {
        currentDiffHunkBlock = handleHunkHeader(line, currentFile, gitDiffDocument);
      } else if (isHunkContentLine(line, currentDiffHunkBlock)) {
        handleHunkContentLine(line, currentDiffHunkBlock);
      }
    }

    return gitDiffDocument;
  }

  /**
   * Checks if the line is an old file header (starts with "--- ").
   *
   * @param line the line to check
   * @return true if the line is an old file header
   */
  private boolean isOldFileHeader(String line) {
    return line.startsWith("--- ");
  }

  /**
   * Checks if the line is a new file header (starts with "+++ ").
   *
   * @param line the line to check
   * @return true if the line is a new file header
   */
  private boolean isNewFileHeader(String line) {
    return line.startsWith("+++ ");
  }

  /**
   * Checks if the line is a hunk header (starts with "@@ ").
   *
   * @param line the line to check
   * @return true if the line is a hunk header
   */
  private boolean isHunkHeader(String line) {
    return line.startsWith("@@ ");
  }

  /**
   * Checks if the line is part of hunk content (additions, deletions, context, or continuation).
   *
   * @param line the line to check
   * @param currentDiffHunkBlock the current hunk being processed
   * @return true if the line should be added to the current hunk
   */
  private boolean isHunkContentLine(String line, DiffHunkBlock currentDiffHunkBlock) {
    return currentDiffHunkBlock != null
        && (line.startsWith("+")
            || line.startsWith("-")
            || line.startsWith(" ")
            || line.startsWith("\\"));
  }

  /**
   * Handles an old file header line, creating a new FileDiff.
   *
   * @param line the old file header line
   * @return newly created FileDiff with oldPath set
   */
  private GitFileModification handleOldFileHeader(String line) {
    GitFileModification gitFileModification = new GitFileModification();
    gitFileModification.oldPath = trimPrefix(line.substring(4), "a/");
    return gitFileModification;
  }

  /**
   * Handles a new file header line, setting the newPath and adding to UnifiedDiff.
   *
   * @param line the new file header line
   * @param currentFile the current FileDiff (may be null)
   * @param gitDiffDocument the UnifiedDiff to add the file to
   * @return the FileDiff with newPath set
   */
  private GitFileModification handleNewFileHeader(
      String line, GitFileModification currentFile, GitDiffDocument gitDiffDocument) {
    if (currentFile == null) {
      currentFile = new GitFileModification();
    }
    currentFile.newPath = trimPrefix(line.substring(4), "b/");
    gitDiffDocument.files.add(currentFile);
    return currentFile;
  }

  /**
   * Handles a hunk header line, parsing line numbers and creating a new Hunk.
   *
   * @param line the hunk header line (format: @@ -oldStart,oldCount +newStart,newCount @@)
   * @param currentFile the current FileDiff (created if null)
   * @param gitDiffDocument the UnifiedDiff to add file to if needed
   * @return newly created Hunk with parsed line numbers
   */
  private DiffHunkBlock handleHunkHeader(
      String line, GitFileModification currentFile, GitDiffDocument gitDiffDocument) {
    String meta = line.substring(3).trim();
    String[] parts = meta.split(" ");
    String[] leftPart = parts[0].substring(1).split(",");
    String[] rightPart = parts[1].substring(1).split(",");

    DiffHunkBlock diffHunkBlock = new DiffHunkBlock();
    diffHunkBlock.oldStart = Integer.parseInt(leftPart[0]);
    diffHunkBlock.oldCount = Integer.parseInt(leftPart.length > 1 ? leftPart[1] : "1");
    diffHunkBlock.newStart = Integer.parseInt(rightPart[0]);
    diffHunkBlock.newCount = Integer.parseInt(rightPart.length > 1 ? rightPart[1] : "1");

    if (currentFile == null) {
      currentFile = new GitFileModification();
      gitDiffDocument.files.add(currentFile);
    }
    currentFile.diffHunkBlocks.add(diffHunkBlock);

    return diffHunkBlock;
  }

  /**
   * Handles a hunk content line (addition, deletion, context, or continuation).
   *
   * @param line the content line to add to the hunk
   * @param currentDiffHunkBlock the hunk to add the line to
   */
  private void handleHunkContentLine(String line, DiffHunkBlock currentDiffHunkBlock) {
    currentDiffHunkBlock.lines.add(line);
  }

  /**
   * Trims a specified prefix from the beginning of a string if present.
   *
   * @param string the string to trim
   * @param prefix the prefix to remove
   * @return the string with prefix removed, or original string if prefix not found
   */
  private String trimPrefix(String string, String prefix) {
    return string.startsWith(prefix) ? string.substring(prefix.length()) : string;
  }
}
