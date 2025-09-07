package com.ghiloufi.aicode.core;

import com.ghiloufi.aicode.domain.FileDiff;
import com.ghiloufi.aicode.domain.Hunk;
import com.ghiloufi.aicode.domain.UnifiedDiff;
import jakarta.validation.constraints.NotBlank;

/**
 * Parser for unified diff format (git diff output). Converts unified diff strings into structured
 * domain objects.
 */
public class UnifiedDiffParser {

  /**
   * Parses a unified diff string into a structured UnifiedDiff object.
   *
   * @param diff the unified diff string to parse (must not be blank)
   * @return parsed UnifiedDiff containing files, hunks, and line changes
   */
  public UnifiedDiff parse(@NotBlank String diff) {

    UnifiedDiff unifiedDiff = new UnifiedDiff();

    String[] lines = diff.lines().toArray(String[]::new);

    FileDiff currentFile = null;
    Hunk currentHunk = null;

    for (String line : lines) {
      if (isOldFileHeader(line)) {
        currentFile = handleOldFileHeader(line);
      } else if (isNewFileHeader(line)) {
        currentFile = handleNewFileHeader(line, currentFile, unifiedDiff);
      } else if (isHunkHeader(line)) {
        currentHunk = handleHunkHeader(line, currentFile, unifiedDiff);
      } else if (isHunkContentLine(line, currentHunk)) {
        handleHunkContentLine(line, currentHunk);
      }
    }

    return unifiedDiff;
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
   * @param currentHunk the current hunk being processed
   * @return true if the line should be added to the current hunk
   */
  private boolean isHunkContentLine(String line, Hunk currentHunk) {
    return currentHunk != null
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
  private FileDiff handleOldFileHeader(String line) {
    FileDiff fileDiff = new FileDiff();
    fileDiff.oldPath = trimPrefix(line.substring(4), "a/");
    return fileDiff;
  }

  /**
   * Handles a new file header line, setting the newPath and adding to UnifiedDiff.
   *
   * @param line the new file header line
   * @param currentFile the current FileDiff (may be null)
   * @param unifiedDiff the UnifiedDiff to add the file to
   * @return the FileDiff with newPath set
   */
  private FileDiff handleNewFileHeader(String line, FileDiff currentFile, UnifiedDiff unifiedDiff) {
    if (currentFile == null) {
      currentFile = new FileDiff();
    }
    currentFile.newPath = trimPrefix(line.substring(4), "b/");
    unifiedDiff.files.add(currentFile);
    return currentFile;
  }

  /**
   * Handles a hunk header line, parsing line numbers and creating a new Hunk.
   *
   * @param line the hunk header line (format: @@ -oldStart,oldCount +newStart,newCount @@)
   * @param currentFile the current FileDiff (created if null)
   * @param unifiedDiff the UnifiedDiff to add file to if needed
   * @return newly created Hunk with parsed line numbers
   */
  private Hunk handleHunkHeader(String line, FileDiff currentFile, UnifiedDiff unifiedDiff) {
    String meta = line.substring(3).trim();
    String[] parts = meta.split(" ");
    String[] leftPart = parts[0].substring(1).split(",");
    String[] rightPart = parts[1].substring(1).split(",");

    Hunk hunk = new Hunk();
    hunk.oldStart = Integer.parseInt(leftPart[0]);
    hunk.oldCount = Integer.parseInt(leftPart.length > 1 ? leftPart[1] : "1");
    hunk.newStart = Integer.parseInt(rightPart[0]);
    hunk.newCount = Integer.parseInt(rightPart.length > 1 ? rightPart[1] : "1");

    if (currentFile == null) {
      currentFile = new FileDiff();
      unifiedDiff.files.add(currentFile);
    }
    currentFile.hunks.add(hunk);

    return hunk;
  }

  /**
   * Handles a hunk content line (addition, deletion, context, or continuation).
   *
   * @param line the content line to add to the hunk
   * @param currentHunk the hunk to add the line to
   */
  private void handleHunkContentLine(String line, Hunk currentHunk) {
    currentHunk.lines.add(line);
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
