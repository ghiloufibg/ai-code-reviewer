package com.ghiloufi.aicode.domain.service;

import com.ghiloufi.aicode.domain.entity.CodeReview;
import com.ghiloufi.aicode.domain.entity.ReviewResult;
import com.ghiloufi.aicode.domain.value.CodeIssue;
import com.ghiloufi.aicode.shared.annotation.DomainService;
import java.util.List;

/**
 * Domain service for applying review policies and business rules.
 *
 * <p>Contains logic for determining review outcomes based on
 * configured policies and quality thresholds.
 */
@DomainService
public class ReviewPolicyService {

    /**
     * Evaluates if a review passes based on configured policies.
     */
    public boolean shouldPassReview(ReviewResult result, CodeReview review) {
        // No blocking issues (errors)
        if (result.hasBlockingIssues()) {
            return false;
        }

        // Check if warning count exceeds threshold
        long warningCount = result.getIssuesBySeverity(CodeIssue.Severity.WARNING).size();
        return warningCount <= getMaxWarningsThreshold(review);
    }

    /**
     * Gets the maximum warnings threshold for a review.
     */
    private int getMaxWarningsThreshold(CodeReview review) {
        // Could be configurable per repository or team
        return 10;
    }

    /**
     * Determines if a review requires human approval.
     */
    public boolean requiresHumanApproval(ReviewResult result, CodeReview review) {
        // Require human approval for high-impact changes
        if (result.hasBlockingIssues()) {
            return true;
        }

        // Require approval for large diff changes
        if (review.getDiffAnalysis() != null &&
            review.getDiffAnalysis().getTotalLineCount() > 500) {
            return true;
        }

        return false;
    }

    /**
     * Calculates priority level for a review.
     */
    public ReviewPriority calculatePriority(CodeReview review) {
        if (review.getDiffAnalysis() == null) {
            return ReviewPriority.LOW;
        }

        int lineCount = review.getDiffAnalysis().getTotalLineCount();
        List<String> modifiedFiles = review.getDiffAnalysis().getModifiedFiles();

        // High priority for large changes or critical files
        if (lineCount > 1000 || containsCriticalFiles(modifiedFiles)) {
            return ReviewPriority.HIGH;
        }

        // Medium priority for moderate changes
        if (lineCount > 200) {
            return ReviewPriority.MEDIUM;
        }

        return ReviewPriority.LOW;
    }

    /**
     * Checks if the modified files contain critical files.
     */
    private boolean containsCriticalFiles(List<String> files) {
        return files.stream().anyMatch(file ->
            file.contains("security") ||
            file.contains("authentication") ||
            file.contains("authorization") ||
            file.endsWith("Config.java") ||
            file.contains("Application.java")
        );
    }

    public enum ReviewPriority {
        HIGH(3),
        MEDIUM(2),
        LOW(1);

        private final int level;

        ReviewPriority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }
}