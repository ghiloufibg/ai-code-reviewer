package com.ghiloufi.aicode.core;

import com.ghiloufi.aicode.domain.FileDiff;
import com.ghiloufi.aicode.domain.Hunk;
import com.ghiloufi.aicode.domain.UnifiedDiff;

/**
 * Maps line numbers from unified diff files to GitHub API positions for inline PR comments.
 * GitHub's position system counts from the start of the diff, including hunk headers.
 */
public class GitHubDiffPositionMapper {

  private final UnifiedDiff unifiedDiff;

  /**
   * Creates a new mapper for the given unified diff.
   *
   * @param unifiedDiff the unified diff to map positions for
   */
  public GitHubDiffPositionMapper(UnifiedDiff unifiedDiff) {
    this.unifiedDiff = unifiedDiff;
  }

  /**
   * Finds the GitHub API position for a specific line number in a file. The position represents the
   * line's location within the entire diff, counting hunk headers and all diff lines.
   *
   * @param path the file path to search for
   * @param newLine the target line number in the new version of the file
   * @return the GitHub API position (1-based), or -1 if the line is not found
   */
  public int positionFor(String path, int newLine) {
    int currentPosition = 0;

    for (FileDiff file : unifiedDiff.files) {
      String filePath = getEffectiveFilePath(file);

      if (!filePath.equals(path)) {
        currentPosition = skipFilePositions(file, currentPosition);
        continue;
      }

      return findLinePositionInFile(file, newLine, currentPosition);
    }

    return -1;
  }

  /**
   * Determines the effective file path to use for comparison. Prefers newPath if available,
   * otherwise falls back to oldPath.
   *
   * @param file the file diff to get the path from
   * @return the effective file path for comparison
   */
  private String getEffectiveFilePath(FileDiff file) {
    return file.newPath != null ? file.newPath : file.oldPath;
  }

  /**
   * Skips all positions for a file that doesn't match the target path. Counts hunk headers and all
   * lines in each hunk.
   *
   * @param file the file to skip
   * @param currentPosition the current position counter
   * @return the updated position after skipping this file
   */
  private int skipFilePositions(FileDiff file, int currentPosition) {
    int position = currentPosition;

    for (Hunk hunk : file.hunks) {
      position += 1 + hunk.lines.size();
    }

    return position;
  }

  /**
   * Searches for the target line within a specific file and returns its position. Tracks the
   * current line number in the new file version while counting positions.
   *
   * @param file the file to search in
   * @param targetNewLine the target line number in the new version
   * @param startPosition the starting position counter
   * @return the GitHub API position if found, -1 if not found
   */
  private int findLinePositionInFile(FileDiff file, int targetNewLine, int startPosition) {
    int currentPosition = startPosition;

    for (Hunk hunk : file.hunks) {
      currentPosition++;

      Integer foundPosition = searchLineInHunk(hunk, targetNewLine, currentPosition);
      if (foundPosition != null) {
        return foundPosition;
      }

      currentPosition += hunk.lines.size();
    }

    return -1;
  }

  /**
   * Searches for the target line within a specific hunk. Tracks line numbers in the new file
   * version and returns position when found.
   *
   * @param hunk the hunk to search in
   * @param targetNewLine the target line number in the new version
   * @param hunkStartPosition the position at the start of this hunk's content
   * @return the position if found, null if not found in this hunk
   */
  private Integer searchLineInHunk(Hunk hunk, int targetNewLine, int hunkStartPosition) {
    int currentNewLine = hunk.newStart - 1;
    int currentPosition = hunkStartPosition;

    for (String line : hunk.lines) {
      currentPosition++;

      if (isNewOrContextLine(line)) {
        currentNewLine++;
        if (currentNewLine == targetNewLine) {
          return currentPosition;
        }
      }
    }

    return null;
  }

  /**
   * Checks if a diff line represents content in the new file version. This includes added lines (+)
   * and context lines (space).
   *
   * @param line the diff line to check
   * @return true if the line exists in the new file version
   */
  private boolean isNewOrContextLine(String line) {
    return line.startsWith("+") || line.startsWith(" ");
  }
}
