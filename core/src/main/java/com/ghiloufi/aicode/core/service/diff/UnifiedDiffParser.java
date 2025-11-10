package com.ghiloufi.aicode.core.service.diff;

import com.ghiloufi.aicode.core.domain.model.DiffHunkBlock;
import com.ghiloufi.aicode.core.domain.model.GitDiffDocument;
import com.ghiloufi.aicode.core.domain.model.GitFileModification;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UnifiedDiffParser {

  public GitDiffDocument parse(@NotBlank final String diff) {
    log.debug("Parsing unified diff, length: {} chars", diff.length());

    final GitDiffDocument gitDiffDocument = new GitDiffDocument();
    final String[] lines = diff.lines().toArray(String[]::new);

    GitFileModification currentFile = null;
    DiffHunkBlock currentDiffHunkBlock = null;

    for (final String line : lines) {
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

    log.debug(
        "Parsed diff: {} files, {} total hunks",
        gitDiffDocument.files.size(),
        gitDiffDocument.files.stream().mapToInt(f -> f.diffHunkBlocks.size()).sum());

    return gitDiffDocument;
  }

  private boolean isOldFileHeader(final String line) {
    return line.startsWith("--- ");
  }

  private boolean isNewFileHeader(final String line) {
    return line.startsWith("+++ ");
  }

  private boolean isHunkHeader(final String line) {
    return line.startsWith("@@ ");
  }

  private boolean isHunkContentLine(final String line, final DiffHunkBlock currentDiffHunkBlock) {
    return currentDiffHunkBlock != null
        && (line.startsWith("+")
            || line.startsWith("-")
            || line.startsWith(" ")
            || line.startsWith("\\"));
  }

  private GitFileModification handleOldFileHeader(final String line) {
    final GitFileModification gitFileModification = new GitFileModification();
    gitFileModification.oldPath = trimPrefix(line.substring(4), "a/");
    return gitFileModification;
  }

  private GitFileModification handleNewFileHeader(
      final String line, GitFileModification currentFile, final GitDiffDocument gitDiffDocument) {
    if (currentFile == null) {
      currentFile = new GitFileModification();
    }
    currentFile.newPath = trimPrefix(line.substring(4), "b/");
    gitDiffDocument.files.add(currentFile);
    return currentFile;
  }

  private DiffHunkBlock handleHunkHeader(
      final String line, GitFileModification currentFile, final GitDiffDocument gitDiffDocument) {
    final String meta = line.substring(3).trim();
    final String[] parts = meta.split(" ");
    final String[] leftPart = parts[0].substring(1).split(",");
    final String[] rightPart = parts[1].substring(1).split(",");

    final DiffHunkBlock diffHunkBlock = new DiffHunkBlock();
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

  private void handleHunkContentLine(final String line, final DiffHunkBlock currentDiffHunkBlock) {
    currentDiffHunkBlock.lines.add(line);
  }

  private String trimPrefix(final String string, final String prefix) {
    return string.startsWith(prefix) ? string.substring(prefix.length()) : string;
  }
}
