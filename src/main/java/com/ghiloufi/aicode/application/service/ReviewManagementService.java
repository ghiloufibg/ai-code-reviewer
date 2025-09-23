package com.ghiloufi.aicode.application.service;

import com.ghiloufi.aicode.application.command.StartReviewCommand;
import com.ghiloufi.aicode.application.port.input.ReviewManagementPort;
import com.ghiloufi.aicode.application.query.ReviewResultsQuery;
import com.ghiloufi.aicode.application.query.ReviewStatusQuery;
import com.ghiloufi.aicode.application.usecase.review.GetReviewResultsUseCase;
import com.ghiloufi.aicode.application.usecase.review.GetReviewStatusUseCase;
import com.ghiloufi.aicode.application.usecase.review.StartReviewUseCase;
import com.ghiloufi.aicode.domain.entity.CodeReview;
import com.ghiloufi.aicode.domain.entity.ReviewResult;
import com.ghiloufi.aicode.domain.repository.ReviewRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Application service implementing review management operations.
 *
 * <p>This service coordinates use cases and provides the implementation of the ReviewManagementPort
 * interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewManagementService implements ReviewManagementPort {

  private final StartReviewUseCase startReviewUseCase;
  private final GetReviewStatusUseCase getReviewStatusUseCase;
  private final GetReviewResultsUseCase getReviewResultsUseCase;
  private final ReviewRepository reviewRepository;

  @Override
  public Mono<CodeReview> startReview(StartReviewCommand command) {
    log.info("Starting review for repository: {}", command.repositoryInfo().repository());
    return startReviewUseCase.execute(command);
  }

  @Override
  public Mono<CodeReview.ReviewStatus> getReviewStatus(ReviewStatusQuery query) {
    return getReviewStatusUseCase.execute(query);
  }

  @Override
  public Mono<ReviewResult> getReviewResults(ReviewResultsQuery query) {
    return getReviewResultsUseCase.execute(query);
  }

  @Override
  public Mono<Void> cancelReview(UUID reviewId) {
    return reviewRepository
        .findById(reviewId)
        .filter(review -> !review.isTerminal())
        .doOnNext(review -> review.markAsFailed("Review cancelled by user"))
        .flatMap(reviewRepository::save)
        .then()
        .doOnSuccess(unused -> log.info("Review cancelled: {}", reviewId));
  }

  @Override
  public Flux<CodeReview> listReviews(
      String repository, CodeReview.ReviewStatus status, int page, int size) {
    // Simple implementation - in production you'd want proper pagination
    if (status != null) {
      return reviewRepository
          .findByStatus(status)
          .filter(
              review ->
                  repository == null || repository.equals(review.getRepositoryInfo().repository()))
          .skip((long) page * size)
          .take(size);
    } else if (repository != null) {
      return reviewRepository.findByRepository(repository).skip((long) page * size).take(size);
    } else {
      // This would need a findAll method in repository
      return Flux.empty();
    }
  }
}
