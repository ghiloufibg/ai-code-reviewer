package com.ghiloufi.aicode.application.port.output;

import com.ghiloufi.aicode.domain.entity.CodeReview;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Port for review persistence operations.
 *
 * <p>This port defines the contract for storing and retrieving code review entities, abstracting
 * the persistence implementation.
 */
public interface ReviewPersistencePort {

  /**
   * Saves a code review entity.
   *
   * @param review the review to save
   * @return the saved review with updated metadata
   */
  Mono<CodeReview> save(CodeReview review);

  /**
   * Finds a review by its ID.
   *
   * @param id the review ID
   * @return the review if found
   */
  Mono<CodeReview> findById(UUID id);

  /**
   * Updates an existing review.
   *
   * @param review the review to update
   * @return the updated review
   */
  Mono<CodeReview> update(CodeReview review);
}
