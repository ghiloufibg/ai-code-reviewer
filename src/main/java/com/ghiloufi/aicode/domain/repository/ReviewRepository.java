package com.ghiloufi.aicode.domain.repository;

import com.ghiloufi.aicode.domain.entity.CodeReview;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository interface for CodeReview entities.
 *
 * <p>Defines the contract for persisting and retrieving code reviews without exposing
 * infrastructure details to the domain layer.
 */
public interface ReviewRepository {

  /** Saves a code review. */
  Mono<CodeReview> save(CodeReview review);

  /** Finds a code review by its ID. */
  Mono<CodeReview> findById(UUID id);

  /** Finds all reviews for a repository. */
  Flux<CodeReview> findByRepository(String repository);

  /** Finds reviews by status. */
  Flux<CodeReview> findByStatus(CodeReview.ReviewStatus status);

  /** Deletes a code review. */
  Mono<Void> delete(UUID id);

  /** Checks if a review exists. */
  Mono<Boolean> existsById(UUID id);
}
