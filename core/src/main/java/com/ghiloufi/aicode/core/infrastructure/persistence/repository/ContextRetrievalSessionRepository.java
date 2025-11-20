package com.ghiloufi.aicode.core.infrastructure.persistence.repository;

import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ContextRetrievalSessionEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContextRetrievalSessionRepository
    extends JpaRepository<ContextRetrievalSessionEntity, UUID> {

  List<ContextRetrievalSessionEntity> findByReviewId(UUID reviewId);

  List<ContextRetrievalSessionEntity> findByCreatedAtBetween(Instant start, Instant end);

  @Query(
      "SELECT s FROM ContextRetrievalSessionEntity s WHERE s.totalMatches > 0 ORDER BY s.createdAt DESC")
  List<ContextRetrievalSessionEntity> findSessionsWithMatches();

  @Query(
      "SELECT AVG(s.totalExecutionTimeMs) FROM ContextRetrievalSessionEntity s WHERE s.createdAt >= :since")
  Double calculateAverageExecutionTime(Instant since);
}
