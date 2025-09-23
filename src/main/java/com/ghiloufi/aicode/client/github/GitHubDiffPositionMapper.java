package com.ghiloufi.aicode.client.github;

import com.ghiloufi.aicode.domain.model.DiffHunkBlock;
import com.ghiloufi.aicode.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.domain.model.GitFileModification;

/**
 * Maps line numbers from unified diff files to GitHub API positions for inline PR comments.
 * GitHub's position system counts from the start of the diff, including hunk headers.
 */
public class GitHubDiffPositionMapper {

  private final GitDiffDocument gitDiffDocument;

  /**
   * Creates a new mapper for the given unified diff.
   *
   * @param gitDiffDocument the unified diff to map positions for
   */
  public GitHubDiffPositionMapper(GitDiffDocument gitDiffDocument) {
    this.gitDiffDocument = gitDiffDocument;
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

    for (GitFileModification file : gitDiffDocument.files) {
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
  private String getEffectiveFilePath(GitFileModification file) {
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
  private int skipFilePositions(GitFileModification file, int currentPosition) {
    int position = currentPosition;

    for (DiffHunkBlock diffHunkBlock : file.diffHunkBlocks) {
      position += 1 + diffHunkBlock.lines.size();
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
  private int findLinePositionInFile(
      GitFileModification file, int targetNewLine, int startPosition) {
    int currentPosition = startPosition;

    for (DiffHunkBlock diffHunkBlock : file.diffHunkBlocks) {
      currentPosition++;

      Integer foundPosition = searchLineInHunk(diffHunkBlock, targetNewLine, currentPosition);
      if (foundPosition != null) {
        return foundPosition;
      }

      currentPosition += diffHunkBlock.lines.size();
    }

    return -1;
  }

  /**
   * Searches for the target line within a specific hunk. Tracks line numbers in the new file
   * version and returns position when found.
   *
   * @param diffHunkBlock the hunk to search in
   * @param targetNewLine the target line number in the new version
   * @param hunkStartPosition the position at the start of this hunk's content
   * @return the position if found, null if not found in this hunk
   */
  private Integer searchLineInHunk(
      DiffHunkBlock diffHunkBlock, int targetNewLine, int hunkStartPosition) {
    int currentNewLine = diffHunkBlock.newStart - 1;
    int currentPosition = hunkStartPosition;

    for (String line : diffHunkBlock.lines) {
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
