package com.ghiloufi.aicode.service.analysis.model;

import com.ghiloufi.aicode.api.model.AnalysisStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;
import lombok.Data;

/**
 * Internal entity representing an analysis in progress. Used for in-memory storage and progress
 * tracking.
 */
@Data
@Builder
public class AnalysisEntity {

  private String id;
  private AnalysisStatus.StatusEnum status;
  private LocalDateTime createdAt;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;

  // Progress tracking
  private AtomicInteger progress; // 0-100
  private String currentStep;
  private Integer totalSteps;
  private Integer stepsCompleted;

  // File information
  private List<String> uploadedFiles;
  private Integer filesCount;
  private Long totalSize;

  // Analysis configuration
  private String model;
  private Boolean staticAnalysis;
  private Boolean securityScan;
  private Boolean performanceAnalysis;

  // Error handling
  private String errorMessage;
  private String estimatedTimeRemaining;

  // LLM analysis results
  private com.ghiloufi.aicode.api.model.AnalysisResults llmResults;

  /** Update progress atomically and safely */
  public void updateProgress(int newProgress, String step) {
    this.progress.set(Math.max(0, Math.min(100, newProgress)));
    this.currentStep = step;
    if (newProgress >= 100) {
      this.status = AnalysisStatus.StatusEnum.COMPLETED;
      this.completedAt = LocalDateTime.now();
    }
  }

  /** Mark analysis as failed with error message */
  public void markAsFailed(String errorMessage) {
    this.status = AnalysisStatus.StatusEnum.FAILED;
    this.errorMessage = errorMessage;
    this.completedAt = LocalDateTime.now();
  }

  /** Mark analysis as cancelled */
  public void markAsCancelled() {
    this.status = AnalysisStatus.StatusEnum.CANCELLED;
    this.completedAt = LocalDateTime.now();
  }

  /** Get current progress value safely */
  public int getCurrentProgress() {
    return this.progress.get();
  }

  /** Check if analysis is in terminal state */
  public boolean isTerminal() {
    return status == AnalysisStatus.StatusEnum.COMPLETED
        || status == AnalysisStatus.StatusEnum.FAILED
        || status == AnalysisStatus.StatusEnum.CANCELLED;
  }

  /** Check if analysis can be cancelled */
  public boolean canBeCancelled() {
    return status == AnalysisStatus.StatusEnum.PENDING
        || status == AnalysisStatus.StatusEnum.IN_PROGRESS;
  }
}
