package com.ghiloufi.aicode.domain.entity;

import com.ghiloufi.aicode.shared.exception.DomainException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing the analysis of code differences.
 *
 * <p>This entity encapsulates the diff content and its structured representation, providing domain
 * behavior for diff analysis operations.
 */
public class DiffAnalysis {

  private final String rawDiff;
  private final List<FileModification> fileModifications;
  private final int totalLineCount;
  private final OffsetDateTime analyzedAt;

  /** Creates a new diff analysis. */
  public DiffAnalysis(String rawDiff, List<FileModification> fileModifications) {
    if (rawDiff == null || rawDiff.trim().isEmpty()) {
      throw new DomainException("Raw diff cannot be null or empty");
    }
    if (fileModifications == null) {
      throw new DomainException("File modifications list cannot be null");
    }

    this.rawDiff = rawDiff;
    this.fileModifications = List.copyOf(fileModifications);
    this.totalLineCount = calculateTotalLineCount();
    this.analyzedAt = OffsetDateTime.now();
  }

  /** Calculates the total number of lines in the diff. */
  private int calculateTotalLineCount() {
    return fileModifications.stream().mapToInt(FileModification::getLineCount).sum();
  }

  /** Checks if the diff is empty (no modifications). */
  public boolean isEmpty() {
    return fileModifications.isEmpty();
  }

  /** Checks if the diff exceeds the specified line limit. */
  public boolean exceedsLineLimit(int limit) {
    return totalLineCount > limit;
  }

  /** Gets files modified in this diff. */
  public List<String> getModifiedFiles() {
    return fileModifications.stream().map(FileModification::getFilePath).toList();
  }

  /** Finds file modification by path. */
  public FileModification getFileModification(String filePath) {
    return fileModifications.stream()
        .filter(fm -> Objects.equals(fm.getFilePath(), filePath))
        .findFirst()
        .orElse(null);
  }

  // Getters
  public String getRawDiff() {
    return rawDiff;
  }

  public List<FileModification> getFileModifications() {
    return fileModifications;
  }

  public int getTotalLineCount() {
    return totalLineCount;
  }

  public OffsetDateTime getAnalyzedAt() {
    return analyzedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DiffAnalysis that)) return false;
    return Objects.equals(rawDiff, that.rawDiff);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawDiff);
  }
}
