package com.ghiloufi.aicode.application.usecase.review;

import com.ghiloufi.aicode.application.query.ReviewStatusQuery;
import com.ghiloufi.aicode.domain.entity.CodeReview;
import com.ghiloufi.aicode.domain.repository.ReviewRepository;
import com.ghiloufi.aicode.shared.annotation.UseCase;
import com.ghiloufi.aicode.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Use case for getting review status.
 */
@UseCase
@RequiredArgsConstructor
public class GetReviewStatusUseCase {

    private final ReviewRepository reviewRepository;

    /**
     * Gets the status of a review.
     */
    public Mono<CodeReview.ReviewStatus> execute(ReviewStatusQuery query) {
        return reviewRepository.findById(query.reviewId())
            .map(CodeReview::getStatus)
            .switchIfEmpty(Mono.error(new ValidationException("Review not found: " + query.reviewId())));
    }
}