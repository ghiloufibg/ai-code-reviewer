package com.ghiloufi.aicode.core.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GitFileModification {

  public String oldPath;
  public String newPath;
  public List<DiffHunkBlock> diffHunkBlocks = new ArrayList<>(10);

  public GitFileModification() {}

  public GitFileModification(String oldPath, String newPath) {
    this.oldPath = oldPath;
    this.newPath = newPath;
  }

  public boolean isRenamed() {
    return !Objects.equals(oldPath, newPath) && !isNewFile() && !isDeleted();
  }

  public boolean isNewFile() {
    return oldPath == null || oldPath.isEmpty() || "/dev/null".equals(oldPath);
  }

  public boolean isDeleted() {
    return newPath == null || newPath.isEmpty() || "/dev/null".equals(newPath);
  }

  public int getTotalLineCount() {
    if (diffHunkBlocks == null) {
      return 0;
    }

    return diffHunkBlocks.stream()
        .mapToInt(hunk -> Optional.ofNullable(hunk.lines).map(List::size).orElse(0))
        .sum();
  }

  public int getHunkCount() {
    return Optional.ofNullable(diffHunkBlocks).map(List::size).orElse(0);
  }

  public String getEffectivePath() {
    return Optional.ofNullable(newPath).orElse(oldPath);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("GitFileModification[");

    if (isNewFile()) {
      sb.append("NEW: ").append(newPath);
    } else if (isDeleted()) {
      sb.append("DELETED: ").append(oldPath);
    } else if (isRenamed()) {
      sb.append("RENAMED: ").append(oldPath).append(" -> ").append(newPath);
    } else {
      sb.append("MODIFIED: ").append(getEffectivePath());
    }

    sb.append(", ").append(getHunkCount()).append(" hunk(s)");
    sb.append(", ").append(getTotalLineCount()).append(" ligne(s)");
    sb.append("]");

    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    GitFileModification other = (GitFileModification) obj;
    return Objects.equals(oldPath, other.oldPath)
        && Objects.equals(newPath, other.newPath)
        && Objects.equals(diffHunkBlocks, other.diffHunkBlocks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oldPath, newPath, diffHunkBlocks);
  }
}
