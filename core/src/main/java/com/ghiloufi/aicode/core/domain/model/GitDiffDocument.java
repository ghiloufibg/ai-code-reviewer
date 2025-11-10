package com.ghiloufi.aicode.core.domain.model;

import java.util.*;

public class GitDiffDocument {

  public List<GitFileModification> files = new ArrayList<>();

  public GitDiffDocument() {}

  public GitDiffDocument(List<GitFileModification> files) {
    Objects.requireNonNull(files, "La liste des fichiers ne peut pas être null");
    this.files.addAll(files);
  }

  public boolean isEmpty() {
    return files.isEmpty() || files.stream().allMatch(file -> file.diffHunkBlocks.isEmpty());
  }

  public int getFileCount() {
    return files.size();
  }

  public int getTotalHunkCount() {
    return files.stream().mapToInt(file -> file.diffHunkBlocks.size()).sum();
  }

  public int getTotalLineCount() {
    return files.stream()
        .flatMap(file -> file.diffHunkBlocks.stream())
        .mapToInt(hunk -> hunk.lines.size())
        .sum();
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "GitDiffDocument[vide]";
    }

    return String.format(
        "GitDiffDocument[%d fichier(s), %d hunk(s), %d ligne(s)]",
        getFileCount(), getTotalHunkCount(), getTotalLineCount());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    GitDiffDocument other = (GitDiffDocument) obj;
    return Objects.equals(files, other.files);
  }

  @Override
  public int hashCode() {
    return Objects.hash(files);
  }
}
