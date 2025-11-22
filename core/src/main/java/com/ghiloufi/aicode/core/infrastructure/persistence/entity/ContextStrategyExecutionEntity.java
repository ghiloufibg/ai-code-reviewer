package com.ghiloufi.aicode.core.infrastructure.persistence.entity;

import com.ghiloufi.aicode.core.domain.model.ExecutionStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "context_strategy_executions")
public class ContextStrategyExecutionEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Setter
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private ContextRetrievalSessionEntity session;

  @Column(name = "strategy_name", nullable = false, length = 100)
  private String strategyName;

  @Column(name = "execution_order", nullable = false)
  private Integer executionOrder;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private ExecutionStatus status;

  @Column(name = "execution_time_ms", nullable = false)
  private Long executionTimeMs;

  @Column(name = "total_candidates")
  private Integer totalCandidates;

  @Column(name = "matches_found")
  private Integer matchesFound;

  @Column(name = "high_confidence_count")
  private Integer highConfidenceCount;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @OneToMany(mappedBy = "strategyExecution", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<ContextMatchEntity> matches = new ArrayList<>();

  @OneToMany(mappedBy = "strategyExecution", cascade = CascadeType.ALL, orphanRemoval = true)
  private final List<ContextReasonDistributionEntity> reasonDistribution = new ArrayList<>();

  protected ContextStrategyExecutionEntity() {}

  public ContextStrategyExecutionEntity(
      final UUID id,
      final String strategyName,
      final Integer executionOrder,
      final ExecutionStatus status,
      final Long executionTimeMs,
      final Integer totalCandidates,
      final Integer matchesFound,
      final Integer highConfidenceCount,
      final String errorMessage,
      final Instant startedAt,
      final Instant completedAt) {
    this.id = id;
    this.strategyName = strategyName;
    this.executionOrder = executionOrder;
    this.status = status;
    this.executionTimeMs = executionTimeMs;
    this.totalCandidates = totalCandidates;
    this.matchesFound = matchesFound;
    this.highConfidenceCount = highConfidenceCount;
    this.errorMessage = errorMessage;
    this.startedAt = startedAt;
    this.completedAt = completedAt;
  }

  public void addMatch(final ContextMatchEntity match) {
    matches.add(match);
    match.setStrategyExecution(this);
  }

  public void addReasonDistribution(final ContextReasonDistributionEntity distribution) {
    reasonDistribution.add(distribution);
    distribution.setStrategyExecution(this);
  }
}
