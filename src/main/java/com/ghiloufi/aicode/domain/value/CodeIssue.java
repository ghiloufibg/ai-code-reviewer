package com.ghiloufi.aicode.domain.value;

import com.ghiloufi.aicode.shared.exception.DomainException;
import java.util.List;
import java.util.Objects;

/**
 * Value object representing a code issue found during analysis.
 *
 * <p>Immutable object containing all details about a specific
 * issue including location, severity, and suggested fixes.
 */
public record CodeIssue(
    String file,
    int startLine,
    int endLine,
    Severity severity,
    String title,
    String description,
    String rationale,
    String suggestion,
    List<String> references,
    String ruleId,
    String tool
) {

    public CodeIssue {
        if (file == null || file.trim().isEmpty()) {
            throw new DomainException("File cannot be null or empty");
        }
        if (startLine <= 0) {
            throw new DomainException("Start line must be positive");
        }
        if (endLine < startLine) {
            throw new DomainException("End line cannot be before start line");
        }
        if (severity == null) {
            throw new DomainException("Severity cannot be null");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new DomainException("Title cannot be null or empty");
        }

        file = file.trim();
        title = title.trim();
        description = description != null ? description.trim() : "";
        rationale = rationale != null ? rationale.trim() : "";
        suggestion = suggestion != null ? suggestion.trim() : "";
        references = references != null ? List.copyOf(references) : List.of();
        ruleId = ruleId != null ? ruleId.trim() : "";
        tool = tool != null ? tool.trim() : "";
    }

    /**
     * Checks if this is a single-line issue.
     */
    public boolean isSingleLine() {
        return startLine == endLine;
    }

    /**
     * Gets the number of lines affected by this issue.
     */
    public int getAffectedLineCount() {
        return endLine - startLine + 1;
    }

    /**
     * Checks if this issue has a suggested fix.
     */
    public boolean hasSuggestion() {
        return suggestion != null && !suggestion.isEmpty();
    }

    /**
     * Checks if this issue has references.
     */
    public boolean hasReferences() {
        return references != null && !references.isEmpty();
    }

    /**
     * Gets a short display text for this issue.
     */
    public String getDisplayText() {
        String line = isSingleLine() ?
            "line " + startLine :
            "lines " + startLine + "-" + endLine;
        return String.format("[%s] %s at %s (%s)",
                           severity.name(), title, line, file);
    }

    /**
     * Creates a simple error issue.
     */
    public static CodeIssue error(String file, int line, String title, String description) {
        return new CodeIssue(file, line, line, Severity.ERROR, title, description,
                           null, null, null, null, null);
    }

    /**
     * Creates a simple warning issue.
     */
    public static CodeIssue warning(String file, int line, String title, String description) {
        return new CodeIssue(file, line, line, Severity.WARNING, title, description,
                           null, null, null, null, null);
    }

    /**
     * Creates a simple info issue.
     */
    public static CodeIssue info(String file, int line, String title, String description) {
        return new CodeIssue(file, line, line, Severity.INFO, title, description,
                           null, null, null, null, null);
    }

    public enum Severity {
        ERROR(3, "Error"),
        WARNING(2, "Warning"),
        INFO(1, "Info");

        private final int priority;
        private final String displayName;

        Severity(int priority, String displayName) {
            this.priority = priority;
            this.displayName = displayName;
        }

        public int getPriority() { return priority; }
        public String getDisplayName() { return displayName; }

        public boolean isHigherThan(Severity other) {
            return this.priority > other.priority;
        }
    }
}