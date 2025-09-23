package com.ghiloufi.aicode.domain.entity;

import com.ghiloufi.aicode.domain.value.ReviewConfiguration;
import com.ghiloufi.aicode.domain.value.RepositoryInfo;
import com.ghiloufi.aicode.shared.exception.DomainException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root representing a code review process.
 *
 * <p>This entity encapsulates the entire lifecycle of a code review,
 * from initiation through completion, including all analysis results
 * and publication status.
 *
 * <p>Following DDD principles, this entity contains both data and
 * behavior relevant to the code review domain.
 */
public class CodeReview {

    private final UUID id;
    private final RepositoryInfo repositoryInfo;
    private final ReviewConfiguration configuration;
    private final OffsetDateTime createdAt;

    private ReviewStatus status;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private DiffAnalysis diffAnalysis;
    private final List<AnalysisResult> analysisResults;
    private ReviewResult finalResult;
    private String errorMessage;

    /**
     * Creates a new code review with initial state.
     */
    public CodeReview(RepositoryInfo repositoryInfo, ReviewConfiguration configuration) {
        if (repositoryInfo == null) {
            throw new DomainException("Repository information cannot be null");
        }
        if (configuration == null) {
            throw new DomainException("Review configuration cannot be null");
        }

        this.id = UUID.randomUUID();
        this.repositoryInfo = repositoryInfo;
        this.configuration = configuration;
        this.createdAt = OffsetDateTime.now();
        this.status = ReviewStatus.PENDING;
        this.analysisResults = new ArrayList<>();
    }

    /**
     * Starts the review process.
     */
    public void start() {
        if (status != ReviewStatus.PENDING) {
            throw new DomainException("Review can only be started from PENDING status");
        }

        this.status = ReviewStatus.IN_PROGRESS;
        this.startedAt = OffsetDateTime.now();
    }

    /**
     * Adds diff analysis to the review.
     */
    public void addDiffAnalysis(DiffAnalysis diffAnalysis) {
        if (status != ReviewStatus.IN_PROGRESS) {
            throw new DomainException("Can only add diff analysis to in-progress review");
        }
        if (diffAnalysis == null) {
            throw new DomainException("Diff analysis cannot be null");
        }

        this.diffAnalysis = diffAnalysis;
    }

    /**
     * Adds an analysis result to the review.
     */
    public void addAnalysisResult(AnalysisResult result) {
        if (status != ReviewStatus.IN_PROGRESS) {
            throw new DomainException("Can only add analysis results to in-progress review");
        }
        if (result == null) {
            throw new DomainException("Analysis result cannot be null");
        }

        this.analysisResults.add(result);
    }

    /**
     * Completes the review with final results.
     */
    public void complete(ReviewResult finalResult) {
        if (status != ReviewStatus.IN_PROGRESS) {
            throw new DomainException("Can only complete in-progress review");
        }
        if (finalResult == null) {
            throw new DomainException("Final result cannot be null");
        }

        this.finalResult = finalResult;
        this.status = ReviewStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now();
    }

    /**
     * Marks the review as failed with error message.
     */
    public void markAsFailed(String errorMessage) {
        if (status == ReviewStatus.COMPLETED) {
            throw new DomainException("Cannot mark completed review as failed");
        }

        this.status = ReviewStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = OffsetDateTime.now();
    }

    /**
     * Checks if the review is in a terminal state.
     */
    public boolean isTerminal() {
        return status == ReviewStatus.COMPLETED || status == ReviewStatus.FAILED;
    }

    /**
     * Checks if the review has analysis results.
     */
    public boolean hasAnalysisResults() {
        return !analysisResults.isEmpty();
    }

    // Getters
    public UUID getId() { return id; }
    public RepositoryInfo getRepositoryInfo() { return repositoryInfo; }
    public ReviewConfiguration getConfiguration() { return configuration; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public ReviewStatus getStatus() { return status; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public DiffAnalysis getDiffAnalysis() { return diffAnalysis; }
    public List<AnalysisResult> getAnalysisResults() { return new ArrayList<>(analysisResults); }
    public ReviewResult getFinalResult() { return finalResult; }
    public String getErrorMessage() { return errorMessage; }

    public enum ReviewStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}