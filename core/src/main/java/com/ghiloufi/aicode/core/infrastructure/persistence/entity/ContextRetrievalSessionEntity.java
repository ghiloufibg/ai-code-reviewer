package com.ghiloufi.aicode.core.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "context_retrieval_sessions")
public class ContextRetrievalSessionEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "review_id", nullable = false)
  private UUID reviewId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "total_execution_time_ms", nullable = false)
  private Long totalExecutionTimeMs;

  @Column(name = "strategies_executed", nullable = false)
  private Integer strategiesExecuted;

  @Column(name = "strategies_succeeded", nullable = false)
  private Integer strategiesSucceeded;

  @Column(name = "strategies_failed", nullable = false)
  private Integer strategiesFailed;

  @Column(name = "diff_file_count", nullable = false)
  private Integer diffFileCount;

  @Column(name = "diff_line_count", nullable = false)
  private Integer diffLineCount;

  @Column(name = "total_matches", nullable = false)
  private Integer totalMatches;

  @Column(name = "high_confidence_matches", nullable = false)
  private Integer highConfidenceMatches;

  @Column(name = "context_enabled", nullable = false)
  private Boolean contextEnabled;

  @Column(name = "skipped_due_to_size", nullable = false)
  private Boolean skippedDueToSize;

  @Column(name = "prompt_text", nullable = false, columnDefinition = "TEXT")
  private String promptText;

  @Column(name = "prompt_size_bytes", nullable = false)
  private Integer promptSizeBytes;

  @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<ContextStrategyExecutionEntity> strategyExecutions = new ArrayList<>();

  protected ContextRetrievalSessionEntity() {}

  public ContextRetrievalSessionEntity(
      final UUID id,
      final UUID reviewId,
      final Instant createdAt,
      final Long totalExecutionTimeMs,
      final Integer strategiesExecuted,
      final Integer strategiesSucceeded,
      final Integer strategiesFailed,
      final Integer diffFileCount,
      final Integer diffLineCount,
      final Integer totalMatches,
      final Integer highConfidenceMatches,
      final Boolean contextEnabled,
      final Boolean skippedDueToSize,
      final String promptText,
      final Integer promptSizeBytes) {
    this.id = id;
    this.reviewId = reviewId;
    this.createdAt = createdAt;
    this.totalExecutionTimeMs = totalExecutionTimeMs;
    this.strategiesExecuted = strategiesExecuted;
    this.strategiesSucceeded = strategiesSucceeded;
    this.strategiesFailed = strategiesFailed;
    this.diffFileCount = diffFileCount;
    this.diffLineCount = diffLineCount;
    this.totalMatches = totalMatches;
    this.highConfidenceMatches = highConfidenceMatches;
    this.contextEnabled = contextEnabled;
    this.skippedDueToSize = skippedDueToSize;
    this.promptText = promptText;
    this.promptSizeBytes = promptSizeBytes;
  }

  public UUID getId() {
    return id;
  }

  public UUID getReviewId() {
    return reviewId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Long getTotalExecutionTimeMs() {
    return totalExecutionTimeMs;
  }

  public Integer getStrategiesExecuted() {
    return strategiesExecuted;
  }

  public Integer getStrategiesSucceeded() {
    return strategiesSucceeded;
  }

  public Integer getStrategiesFailed() {
    return strategiesFailed;
  }

  public Integer getDiffFileCount() {
    return diffFileCount;
  }

  public Integer getDiffLineCount() {
    return diffLineCount;
  }

  public Integer getTotalMatches() {
    return totalMatches;
  }

  public Integer getHighConfidenceMatches() {
    return highConfidenceMatches;
  }

  public Boolean getContextEnabled() {
    return contextEnabled;
  }

  public Boolean getSkippedDueToSize() {
    return skippedDueToSize;
  }

  public String getPromptText() {
    return promptText;
  }

  public Integer getPromptSizeBytes() {
    return promptSizeBytes;
  }

  public List<ContextStrategyExecutionEntity> getStrategyExecutions() {
    return strategyExecutions;
  }

  public void addStrategyExecution(final ContextStrategyExecutionEntity execution) {
    strategyExecutions.add(execution);
    execution.setSession(this);
  }

  public void setTotalExecutionTimeMs(final Long totalExecutionTimeMs) {
    this.totalExecutionTimeMs = totalExecutionTimeMs;
  }

  public void setStrategiesExecuted(final Integer strategiesExecuted) {
    this.strategiesExecuted = strategiesExecuted;
  }

  public void setStrategiesSucceeded(final Integer strategiesSucceeded) {
    this.strategiesSucceeded = strategiesSucceeded;
  }

  public void setStrategiesFailed(final Integer strategiesFailed) {
    this.strategiesFailed = strategiesFailed;
  }
}
