package com.ghiloufi.aicode.core.infrastructure.persistence.repository;

import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ContextMatchEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContextMatchRepository extends JpaRepository<ContextMatchEntity, UUID> {

  List<ContextMatchEntity> findByStrategyExecutionId(UUID strategyExecutionId);
}
