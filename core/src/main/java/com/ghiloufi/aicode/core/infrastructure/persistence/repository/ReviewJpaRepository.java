package com.ghiloufi.aicode.core.infrastructure.persistence.repository;

import com.ghiloufi.aicode.core.domain.model.ReviewState;
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

  @Query(
      "SELECT DISTINCT r FROM ReviewEntity r "
          + "LEFT JOIN FETCH r.issues "
          + "WHERE r.repositoryId = :repositoryId "
          + "AND r.changeRequestId = :changeRequestId "
          + "AND r.provider = :provider")
  Optional<ReviewEntity> findByRepositoryIdAndChangeRequestIdAndProviderWithIssues(
      @Param("repositoryId") String repositoryId,
      @Param("changeRequestId") String changeRequestId,
      @Param("provider") String provider);

  @Query(
      "SELECT DISTINCT r FROM ReviewEntity r "
          + "LEFT JOIN FETCH r.notes "
          + "WHERE r.repositoryId = :repositoryId "
          + "AND r.changeRequestId = :changeRequestId "
          + "AND r.provider = :provider")
  Optional<ReviewEntity> findByRepositoryIdAndChangeRequestIdAndProviderWithNotes(
      @Param("repositoryId") String repositoryId,
      @Param("changeRequestId") String changeRequestId,
      @Param("provider") String provider);

  List<ReviewEntity> findByStatusOrderByCreatedAtDesc(ReviewState status);

  List<ReviewEntity> findByProviderOrderByCreatedAtDesc(String provider);

  @Query("SELECT r FROM ReviewEntity r WHERE r.createdAt < :cutoffDate ORDER BY r.createdAt DESC")
  List<ReviewEntity> findOldReviews(@Param("cutoffDate") Instant cutoffDate);

  @Query("SELECT r FROM ReviewEntity r WHERE r.status IN :statuses ORDER BY r.createdAt DESC")
  List<ReviewEntity> findByStatusIn(@Param("statuses") List<ReviewState> statuses);

  long countByStatus(ReviewState status);

  long countByProvider(String provider);
}
