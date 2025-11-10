package com.ghiloufi.aicode.core.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DiffHunkBlock {

  public int oldStart;
  public int oldCount;
  public int newStart;
  public int newCount;
  public List<String> lines = new ArrayList<>();

  public DiffHunkBlock() {}

  public DiffHunkBlock(
      final int oldStart, final int oldCount, final int newStart, final int newCount) {
    this.oldStart = oldStart;
    this.oldCount = oldCount;
    this.newStart = newStart;
    this.newCount = newCount;
  }

  public int getAddedLinesCount() {
    if (lines == null) {
      return 0;
    }

    return (int) lines.stream().filter(line -> line.startsWith("+")).count();
  }

  public int getDeletedLinesCount() {
    if (lines == null) {
      return 0;
    }

    return (int) lines.stream().filter(line -> line.startsWith("-")).count();
  }

  public int getContextLinesCount() {
    if (lines == null) {
      return 0;
    }

    return (int) lines.stream().filter(line -> line.startsWith(" ")).count();
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
