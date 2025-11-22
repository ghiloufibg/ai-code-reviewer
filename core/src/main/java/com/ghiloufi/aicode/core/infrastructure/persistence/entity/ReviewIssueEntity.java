package com.ghiloufi.aicode.core.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "review_issues")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIssueEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "review_id", nullable = false)
  private ReviewEntity review;

  @Column(name = "file_path", nullable = false)
  private String filePath;

  @Column(name = "start_line", nullable = false)
  private Integer startLine;

  @Column(name = "severity", nullable = false)
  private String severity;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "suggestion", columnDefinition = "TEXT")
  private String suggestion;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "inline_comment_posted", nullable = false)
  @Builder.Default
  private boolean inlineCommentPosted = false;

  @Column(name = "scm_comment_id")
  private String scmCommentId;

  @Column(name = "fallback_reason")
  private String fallbackReason;

  @Column(name = "position_metadata", columnDefinition = "TEXT")
  private String positionMetadata;

  @Column(name = "confidence_score", precision = 3, scale = 2)
  private BigDecimal confidenceScore;

  @Column(name = "confidence_explanation", columnDefinition = "TEXT")
  private String confidenceExplanation;

  @Column(name = "suggested_fix", columnDefinition = "TEXT")
  private String suggestedFix;

  @Column(name = "fix_diff", columnDefinition = "TEXT")
  private String fixDiff;

  @Column(name = "fix_applied", nullable = false)
  @Builder.Default
  private boolean fixApplied = false;

  @Column(name = "applied_at")
  private Instant appliedAt;

  @Column(name = "applied_commit_sha", length = 40)
  private String appliedCommitSha;

  @PrePersist
  protected void onCreate() {
    createdAt = Instant.now();
  }

  public boolean isHighConfidence() {
    return confidenceScore != null && confidenceScore.compareTo(new BigDecimal("0.70")) >= 0;
  }

  public boolean hasFixSuggestion() {
    return suggestedFix != null && !suggestedFix.isBlank();
  }

  public boolean canApplyFix() {
    return hasFixSuggestion() && !fixApplied && isHighConfidence();
  }
}
