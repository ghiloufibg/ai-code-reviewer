package com.ghiloufi.aicode.core.infrastructure.persistence.repository;

import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ContextStrategyExecutionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContextStrategyExecutionRepository
    extends JpaRepository<ContextStrategyExecutionEntity, UUID> {

  List<ContextStrategyExecutionEntity> findBySessionId(UUID sessionId);
}
