package com.ghiloufi.aicode.core.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "context_matches")
public class ContextMatchEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Setter
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "strategy_execution_id", nullable = false)
  private ContextStrategyExecutionEntity strategyExecution;

  @Column(name = "file_path", nullable = false, length = 500)
  private String filePath;

  @Enumerated(EnumType.STRING)
  @Column(name = "match_reason", nullable = false, length = 50)
  private MatchReasonEnum matchReason;

  @Column(name = "confidence", nullable = false, precision = 3, scale = 2)
  private BigDecimal confidence;

  @Column(name = "evidence", nullable = false, columnDefinition = "TEXT")
  private String evidence;

  @Column(name = "is_high_confidence", nullable = false)
  private Boolean isHighConfidence;

  @Column(name = "included_in_prompt", nullable = false)
  private Boolean includedInPrompt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ContextMatchEntity() {}

  public ContextMatchEntity(
      final UUID id,
      final String filePath,
      final MatchReasonEnum matchReason,
      final BigDecimal confidence,
      final String evidence,
      final Boolean isHighConfidence,
      final Boolean includedInPrompt,
      final Instant createdAt) {
    this.id = id;
    this.filePath = filePath;
    this.matchReason = matchReason;
    this.confidence = confidence;
    this.evidence = evidence;
    this.isHighConfidence = isHighConfidence;
    this.includedInPrompt = includedInPrompt;
    this.createdAt = createdAt;
  }
}
