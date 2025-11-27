package com.ghiloufi.aicode.core.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DiffHunkBlock {

  public int oldStart;
  public int oldCount;
  public int newStart;
  public int newCount;
  public List<String> lines = new ArrayList<>(50);

  public DiffHunkBlock() {}

  public DiffHunkBlock(
      final int oldStart, final int oldCount, final int newStart, final int newCount) {
    this.oldStart = oldStart;
    this.oldCount = oldCount;
    this.newStart = newStart;
    this.newCount = newCount;
  }

  public record LineStats(int added, int deleted, int context) {}

  public LineStats getLineStats() {
    if (lines == null) {
      return new LineStats(0, 0, 0);
    }
    int added = 0;
    int deleted = 0;
    int context = 0;
    for (final String line : lines) {
      if (line.startsWith("+")) {
        added++;
      } else if (line.startsWith("-")) {
        deleted++;
      } else if (line.startsWith(" ")) {
        context++;
      }
    }
    return new LineStats(added, deleted, context);
  }

  public int getAddedLinesCount() {
    return getLineStats().added();
  }

  public int getDeletedLinesCount() {
    return getLineStats().deleted();
  }

  public int getContextLinesCount() {
    return getLineStats().context();
  }

  public boolean isEmpty() {
    return lines == null || lines.isEmpty();
  }

  public String generateHeader() {
    return String.format("@@ -%d,%d +%d,%d @@", oldStart, oldCount, newStart, newCount);
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "DiffHunkBlock[vide]";
    }

    return String.format(
        "DiffHunkBlock[%s, +%d/-%d lignes, %d contexte]",
        generateHeader(), getAddedLinesCount(), getDeletedLinesCount(), getContextLinesCount());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    final DiffHunkBlock other = (DiffHunkBlock) obj;
    return oldStart == other.oldStart
        && oldCount == other.oldCount
        && newStart == other.newStart
        && newCount == other.newCount
        && Objects.equals(lines, other.lines);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oldStart, oldCount, newStart, newCount, lines);
  }
}
