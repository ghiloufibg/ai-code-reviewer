package com.ghiloufi.aicode.application.port.input;

import com.ghiloufi.aicode.application.command.StartReviewCommand;
import com.ghiloufi.aicode.application.query.ReviewStatusQuery;
import com.ghiloufi.aicode.application.query.ReviewResultsQuery;
import com.ghiloufi.aicode.domain.entity.CodeReview;
import com.ghiloufi.aicode.domain.entity.ReviewResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Input port for review management operations.
 *
 * <p>Defines the contract for managing code reviews from
 * external interfaces (REST, messaging, etc.).
 */
public interface ReviewManagementPort {

    /**
     * Starts a new code review.
     */
    Mono<CodeReview> startReview(StartReviewCommand command);

    /**
     * Gets the status of a review.
     */
    Mono<CodeReview.ReviewStatus> getReviewStatus(ReviewStatusQuery query);

    /**
     * Gets the results of a completed review.
     */
    Mono<ReviewResult> getReviewResults(ReviewResultsQuery query);

    /**
     * Cancels an in-progress review.
     */
    Mono<Void> cancelReview(UUID reviewId);

    /**
     * Lists reviews with filtering.
     */
    Flux<CodeReview> listReviews(String repository, CodeReview.ReviewStatus status, int page, int size);
}