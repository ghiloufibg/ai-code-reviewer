package com.ghiloufi.aicode.core.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(
    name = "context_reason_distribution",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"strategy_execution_id", "match_reason"})
    })
public class ContextReasonDistributionEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Setter
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "strategy_execution_id", nullable = false)
  private ContextStrategyExecutionEntity strategyExecution;

  @Enumerated(EnumType.STRING)
  @Column(name = "match_reason", nullable = false, length = 50)
  private MatchReasonEnum matchReason;

  @Column(name = "count", nullable = false)
  private Integer count;

  protected ContextReasonDistributionEntity() {}

  public ContextReasonDistributionEntity(
      final UUID id, final MatchReasonEnum matchReason, final Integer count) {
    this.id = id;
    this.matchReason = matchReason;
    this.count = count;
  }
}
