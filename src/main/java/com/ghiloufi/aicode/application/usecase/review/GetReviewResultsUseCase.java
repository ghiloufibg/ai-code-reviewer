package com.ghiloufi.aicode.application.usecase.review;

import com.ghiloufi.aicode.application.query.ReviewResultsQuery;
import com.ghiloufi.aicode.domain.entity.ReviewResult;
import com.ghiloufi.aicode.domain.repository.ReviewRepository;
import com.ghiloufi.aicode.shared.annotation.UseCase;
import com.ghiloufi.aicode.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Use case for getting review results.
 */
@UseCase
@RequiredArgsConstructor
public class GetReviewResultsUseCase {

    private final ReviewRepository reviewRepository;

    /**
     * Gets the results of a completed review.
     */
    public Mono<ReviewResult> execute(ReviewResultsQuery query) {
        return reviewRepository.findById(query.reviewId())
            .filter(review -> review.getFinalResult() != null)
            .map(review -> review.getFinalResult())
            .switchIfEmpty(Mono.error(new ValidationException("Review results not available: " + query.reviewId())));
    }
}