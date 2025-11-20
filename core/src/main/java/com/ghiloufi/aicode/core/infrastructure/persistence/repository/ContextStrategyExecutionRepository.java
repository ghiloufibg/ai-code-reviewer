package com.ghiloufi.aicode.core.infrastructure.persistence.repository;

import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ContextStrategyExecutionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContextStrategyExecutionRepository
    extends JpaRepository<ContextStrategyExecutionEntity, UUID> {

  List<ContextStrategyExecutionEntity> findBySessionId(UUID sessionId);

  List<ContextStrategyExecutionEntity> findByStrategyName(String strategyName);

  @Query(
      "SELECT e FROM ContextStrategyExecutionEntity e WHERE e.status = 'ERROR' ORDER BY e.startedAt DESC")
  List<ContextStrategyExecutionEntity> findFailedExecutions();

  @Query(
      "SELECT e.strategyName, AVG(e.executionTimeMs) FROM ContextStrategyExecutionEntity e "
          + "WHERE e.status = 'SUCCESS' GROUP BY e.strategyName")
  List<Object[]> calculateAverageExecutionTimeByStrategy();
}
