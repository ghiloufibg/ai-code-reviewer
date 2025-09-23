package com.ghiloufi.aicode.domain.entity;

import com.ghiloufi.aicode.shared.exception.DomainException;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a modification to a single file.
 *
 * <p>Contains the file path and all diff hunks that show
 * the specific changes made to that file.
 */
public class FileModification {

    private final String filePath;
    private final String oldPath;
    private final List<DiffHunk> diffHunks;
    private final ModificationType type;

    /**
     * Creates a new file modification.
     */
    public FileModification(String filePath, String oldPath, List<DiffHunk> diffHunks, ModificationType type) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new DomainException("File path cannot be null or empty");
        }
        if (diffHunks == null) {
            throw new DomainException("Diff hunks list cannot be null");
        }
        if (type == null) {
            throw new DomainException("Modification type cannot be null");
        }

        this.filePath = filePath;
        this.oldPath = oldPath;
        this.diffHunks = List.copyOf(diffHunks);
        this.type = type;
    }

    /**
     * Calculates the total number of lines in all hunks.
     */
    public int getLineCount() {
        return diffHunks.stream()
                .mapToInt(DiffHunk::getLineCount)
                .sum();
    }

    /**
     * Checks if this file was renamed.
     */
    public boolean isRenamed() {
        return oldPath != null && !Objects.equals(oldPath, filePath);
    }

    /**
     * Checks if this file was newly created.
     */
    public boolean isNewFile() {
        return type == ModificationType.ADDED;
    }

    /**
     * Checks if this file was deleted.
     */
    public boolean isDeleted() {
        return type == ModificationType.DELETED;
    }

    /**
     * Gets the number of added lines.
     */
    public int getAddedLines() {
        return diffHunks.stream()
                .mapToInt(DiffHunk::getAddedLines)
                .sum();
    }

    /**
     * Gets the number of removed lines.
     */
    public int getRemovedLines() {
        return diffHunks.stream()
                .mapToInt(DiffHunk::getRemovedLines)
                .sum();
    }

    // Getters
    public String getFilePath() { return filePath; }
    public String getOldPath() { return oldPath; }
    public List<DiffHunk> getDiffHunks() { return diffHunks; }
    public ModificationType getType() { return type; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileModification that)) return false;
        return Objects.equals(filePath, that.filePath) &&
               Objects.equals(oldPath, that.oldPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, oldPath);
    }

    public enum ModificationType {
        ADDED,
        MODIFIED,
        DELETED,
        RENAMED
    }
}