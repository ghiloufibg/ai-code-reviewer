package com.ghiloufi.aicode.core.infrastructure.persistence.repository;

import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ContextMatchEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.MatchReasonEnum;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContextMatchRepository extends JpaRepository<ContextMatchEntity, UUID> {

  List<ContextMatchEntity> findByStrategyExecutionId(UUID strategyExecutionId);

  List<ContextMatchEntity> findByMatchReason(MatchReasonEnum reason);

  List<ContextMatchEntity> findByIsHighConfidenceTrue();

  List<ContextMatchEntity> findByIncludedInPromptTrue();

  @Query(
      "SELECT m.matchReason, COUNT(m) FROM ContextMatchEntity m GROUP BY m.matchReason ORDER BY COUNT(m) DESC")
  List<Object[]> countMatchesByReason();

  @Query("SELECT AVG(m.confidence) FROM ContextMatchEntity m WHERE m.includedInPrompt = true")
  Double calculateAverageConfidenceForIncludedMatches();
}
