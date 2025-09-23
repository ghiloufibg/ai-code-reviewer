package com.ghiloufi.aicode.domain.entity;

import com.ghiloufi.aicode.domain.value.CodeIssue;
import com.ghiloufi.aicode.shared.exception.DomainException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing the result of a specific analysis (LLM, static analysis, etc.).
 *
 * <p>This entity contains the analysis type, summary, and all issues
 * found during that specific analysis phase.
 */
public class AnalysisResult {

    private final AnalysisType type;
    private final String summary;
    private final List<CodeIssue> issues;
    private final List<String> notes;
    private final OffsetDateTime analyzedAt;
    private final String toolVersion;

    /**
     * Creates a new analysis result.
     */
    public AnalysisResult(AnalysisType type, String summary, List<CodeIssue> issues, List<String> notes, String toolVersion) {
        if (type == null) {
            throw new DomainException("Analysis type cannot be null");
        }
        if (summary == null) {
            throw new DomainException("Summary cannot be null");
        }

        this.type = type;
        this.summary = summary;
        this.issues = issues != null ? List.copyOf(issues) : new ArrayList<>();
        this.notes = notes != null ? List.copyOf(notes) : new ArrayList<>();
        this.analyzedAt = OffsetDateTime.now();
        this.toolVersion = toolVersion;
    }

    /**
     * Gets the number of issues found.
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
     * Gets the count of critical issues.
     */
    public long getCriticalIssueCount() {
        return issues.stream()
                .filter(issue -> issue.severity() == CodeIssue.Severity.ERROR)
                .count();
    }

    /**
     * Checks if this analysis found any critical issues.
     */
    public boolean hasCriticalIssues() {
        return getCriticalIssueCount() > 0;
    }

    /**
     * Checks if this analysis found any issues at all.
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * Gets a summary of issue counts by severity.
     */
    public String getIssueSummary() {
        long errors = getIssuesBySeverity(CodeIssue.Severity.ERROR).size();
        long warnings = getIssuesBySeverity(CodeIssue.Severity.WARNING).size();
        long infos = getIssuesBySeverity(CodeIssue.Severity.INFO).size();

        return String.format("Errors: %d, Warnings: %d, Info: %d", errors, warnings, infos);
    }

    // Getters
    public AnalysisType getType() { return type; }
    public String getSummary() { return summary; }
    public List<CodeIssue> getIssues() { return issues; }
    public List<String> getNotes() { return notes; }
    public OffsetDateTime getAnalyzedAt() { return analyzedAt; }
    public String getToolVersion() { return toolVersion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnalysisResult that)) return false;
        return type == that.type &&
               Objects.equals(analyzedAt, that.analyzedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, analyzedAt);
    }

    public enum AnalysisType {
        LLM_ANALYSIS("LLM Analysis"),
        STATIC_ANALYSIS_CHECKSTYLE("Checkstyle"),
        STATIC_ANALYSIS_PMD("PMD"),
        STATIC_ANALYSIS_SPOTBUGS("SpotBugs"),
        STATIC_ANALYSIS_SEMGREP("Semgrep"),
        SECURITY_ANALYSIS("Security Analysis");

        private final String displayName;

        AnalysisType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}