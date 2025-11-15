package com.ghiloufi.aicode.core.infrastructure.persistence.repository;

import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewIssueEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewIssueRepository extends JpaRepository<ReviewIssueEntity, UUID> {

  @Query(
      "SELECT i FROM ReviewIssueEntity i "
          + "LEFT JOIN FETCH i.review r "
          + "WHERE i.id = :issueId")
  Optional<ReviewIssueEntity> findByIdWithReview(@Param("issueId") UUID issueId);

  @Query(
      "SELECT i FROM ReviewIssueEntity i "
          + "WHERE i.id = :issueId "
          + "AND i.fixApplied = false "
          + "AND i.suggestedFix IS NOT NULL "
          + "AND i.fixDiff IS NOT NULL "
          + "AND i.confidenceScore >= 0.70")
  Optional<ReviewIssueEntity> findApplicableFixById(@Param("issueId") UUID issueId);
}
