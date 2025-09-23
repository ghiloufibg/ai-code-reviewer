package com.ghiloufi.aicode.domain.entity;

import com.ghiloufi.aicode.domain.value.CodeIssue;
import com.ghiloufi.aicode.shared.exception.DomainException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing the final consolidated result of a code review.
 *
 * <p>This entity aggregates all analysis results into a final
 * comprehensive review with merged issues and an overall summary.
 */
public class ReviewResult {

    private final String summary;
    private final List<CodeIssue> issues;
    private final List<String> nonBlockingNotes;
    private final ReviewQuality quality;
    private final OffsetDateTime createdAt;

    /**
     * Creates a new review result.
     */
    public ReviewResult(String summary, List<CodeIssue> issues, List<String> nonBlockingNotes) {
        if (summary == null || summary.trim().isEmpty()) {
            throw new DomainException("Summary cannot be null or empty");
        }

        this.summary = summary.trim();
        this.issues = issues != null ? List.copyOf(issues) : new ArrayList<>();
        this.nonBlockingNotes = nonBlockingNotes != null ? List.copyOf(nonBlockingNotes) : new ArrayList<>();
        this.quality = calculateQuality();
        this.createdAt = OffsetDateTime.now();
    }

    /**
     * Calculates the overall quality of the review.
     */
    private ReviewQuality calculateQuality() {
        long errorCount = getIssuesBySeverity(CodeIssue.Severity.ERROR).size();
        long warningCount = getIssuesBySeverity(CodeIssue.Severity.WARNING).size();

        if (errorCount > 0) {
            return ReviewQuality.POOR;
        } else if (warningCount > 5) {
            return ReviewQuality.FAIR;
        } else if (warningCount > 0) {
            return ReviewQuality.GOOD;
        } else {
            return ReviewQuality.EXCELLENT;
        }
    }

    /**
     * Gets the total number of issues.
     */
    public int getIssueCount() {
        return issues.size();
    }

    /**
     * Gets issues by severity level.
     */
    public List<CodeIssue> getIssuesBySeverity(CodeIssue.Severity severity) {
        return issues.stream()
                .filter(issue -> issue.severity() == severity)
                .toList();
    }

    /**
     * Checks if the review has any blocking issues.
     */
    public boolean hasBlockingIssues() {
        return issues.stream()
                .anyMatch(issue -> issue.severity() == CodeIssue.Severity.ERROR);
    }

    /**
     * Gets a summary of issues by severity.
     */
    public String getIssueSummary() {
        long errors = getIssuesBySeverity(CodeIssue.Severity.ERROR).size();
        long warnings = getIssuesBySeverity(CodeIssue.Severity.WARNING).size();
        long infos = getIssuesBySeverity(CodeIssue.Severity.INFO).size();

        return String.format("Found %d issue(s): %d error(s), %d warning(s), %d info",
                           getIssueCount(), errors, warnings, infos);
    }

    /**
     * Checks if the review passed (no blocking issues).
     */
    public boolean isPassed() {
        return !hasBlockingIssues();
    }

    // Getters
    public String getSummary() { return summary; }
    public List<CodeIssue> getIssues() { return issues; }
    public List<String> getNonBlockingNotes() { return nonBlockingNotes; }
    public ReviewQuality getQuality() { return quality; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReviewResult that)) return false;
        return Objects.equals(summary, that.summary) &&
               Objects.equals(issues, that.issues) &&
               Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summary, issues, createdAt);
    }

    public enum ReviewQuality {
        EXCELLENT(4, "Excellent"),
        GOOD(3, "Good"),
        FAIR(2, "Fair"),
        POOR(1, "Poor");

        private final int score;
        private final String displayName;

        ReviewQuality(int score, String displayName) {
            this.score = score;
            this.displayName = displayName;
        }

        public int getScore() { return score; }
        public String getDisplayName() { return displayName; }
    }
}