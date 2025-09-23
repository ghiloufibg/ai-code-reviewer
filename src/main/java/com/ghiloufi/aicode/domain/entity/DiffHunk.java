package com.ghiloufi.aicode.domain.entity;

import com.ghiloufi.aicode.shared.exception.DomainException;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a single diff hunk (a contiguous block of changes).
 *
 * <p>A diff hunk contains the line numbers and the actual content
 * of the changes within that range.
 */
public class DiffHunk {

    private final int oldStart;
    private final int oldCount;
    private final int newStart;
    private final int newCount;
    private final List<String> lines;

    /**
     * Creates a new diff hunk.
     */
    public DiffHunk(int oldStart, int oldCount, int newStart, int newCount, List<String> lines) {
        if (oldStart < 0 || newStart < 0) {
            throw new DomainException("Start line numbers cannot be negative");
        }
        if (oldCount < 0 || newCount < 0) {
            throw new DomainException("Line counts cannot be negative");
        }
        if (lines == null) {
            throw new DomainException("Lines list cannot be null");
        }

        this.oldStart = oldStart;
        this.oldCount = oldCount;
        this.newStart = newStart;
        this.newCount = newCount;
        this.lines = List.copyOf(lines);
    }

    /**
     * Gets the total number of lines in this hunk.
     */
    public int getLineCount() {
        return lines.size();
    }

    /**
     * Counts the number of added lines (starting with +).
     */
    public int getAddedLines() {
        return (int) lines.stream()
                .filter(line -> line.startsWith("+") && !line.startsWith("+++"))
                .count();
    }

    /**
     * Counts the number of removed lines (starting with -).
     */
    public int getRemovedLines() {
        return (int) lines.stream()
                .filter(line -> line.startsWith("-") && !line.startsWith("---"))
                .count();
    }

    /**
     * Counts the number of context lines (starting with space or no prefix).
     */
    public int getContextLines() {
        return (int) lines.stream()
                .filter(line -> line.startsWith(" ") ||
                               (!line.startsWith("+") && !line.startsWith("-")))
                .count();
    }

    /**
     * Checks if this hunk contains only additions.
     */
    public boolean isAdditionOnly() {
        return getRemovedLines() == 0 && getAddedLines() > 0;
    }

    /**
     * Checks if this hunk contains only deletions.
     */
    public boolean isDeletionOnly() {
        return getAddedLines() == 0 && getRemovedLines() > 0;
    }

    /**
     * Gets the range of new line numbers affected by this hunk.
     */
    public LineRange getNewLineRange() {
        return new LineRange(newStart, newStart + newCount - 1);
    }

    /**
     * Gets the range of old line numbers affected by this hunk.
     */
    public LineRange getOldLineRange() {
        return new LineRange(oldStart, oldStart + oldCount - 1);
    }

    // Getters
    public int getOldStart() { return oldStart; }
    public int getOldCount() { return oldCount; }
    public int getNewStart() { return newStart; }
    public int getNewCount() { return newCount; }
    public List<String> getLines() { return lines; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiffHunk diffHunk)) return false;
        return oldStart == diffHunk.oldStart &&
               oldCount == diffHunk.oldCount &&
               newStart == diffHunk.newStart &&
               newCount == diffHunk.newCount &&
               Objects.equals(lines, diffHunk.lines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldStart, oldCount, newStart, newCount, lines);
    }

    /**
     * Represents a range of line numbers.
     */
    public record LineRange(int start, int end) {
        public LineRange {
            if (start < 0 || end < start) {
                throw new DomainException("Invalid line range: start=" + start + ", end=" + end);
            }
        }

        public boolean contains(int lineNumber) {
            return lineNumber >= start && lineNumber <= end;
        }

        public int size() {
            return end - start + 1;
        }
    }
}