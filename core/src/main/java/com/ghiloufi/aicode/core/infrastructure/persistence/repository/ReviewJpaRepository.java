package com.ghiloufi.aicode.core.infrastructure.persistence.repository;

import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, UUID> {

  Optional<ReviewEntity> findByRepositoryIdAndChangeRequestIdAndProvider(
      String repositoryId, String changeRequestId, String provider);

  @Query("SELECT r FROM ReviewEntity r WHERE r.createdAt < :cutoffDate ORDER BY r.createdAt DESC")
  List<ReviewEntity> findOldReviews(@Param("cutoffDate") Instant cutoffDate);
}
