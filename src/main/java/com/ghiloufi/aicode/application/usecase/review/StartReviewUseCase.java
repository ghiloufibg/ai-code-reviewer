package com.ghiloufi.aicode.application.usecase.review;

import com.ghiloufi.aicode.application.command.StartReviewCommand;
import com.ghiloufi.aicode.application.port.output.AIAnalysisPort;
import com.ghiloufi.aicode.application.port.output.DiffCollectionPort;
import com.ghiloufi.aicode.application.port.output.ReviewPublishingPort;
import com.ghiloufi.aicode.application.port.output.StaticAnalysisPort;
import com.ghiloufi.aicode.domain.entity.AnalysisResult;
import com.ghiloufi.aicode.domain.entity.CodeReview;
import com.ghiloufi.aicode.domain.entity.ReviewResult;
import com.ghiloufi.aicode.domain.repository.ReviewRepository;
import com.ghiloufi.aicode.domain.service.ReviewPolicyService;
import com.ghiloufi.aicode.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Use case for starting a new code review.
 *
 * <p>Orchestrates the entire review process from diff collection through analysis and result
 * publication.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class StartReviewUseCase {

  private final ReviewRepository reviewRepository;
  private final DiffCollectionPort diffCollectionPort;
  private final StaticAnalysisPort staticAnalysisPort;
  private final AIAnalysisPort aiAnalysisPort;
  private final ReviewPublishingPort reviewPublishingPort;
  private final ReviewPolicyService reviewPolicyService;

  /** Executes the review start use case. */
  public Mono<CodeReview> execute(StartReviewCommand command) {
    log.info("Starting review for repository: {}", command.repositoryInfo().getDisplayName());

    // Create and save initial review
    CodeReview review = new CodeReview(command.repositoryInfo(), command.configuration());
    review.start();

    return reviewRepository
        .save(review)
        .flatMap(this::collectDiffAndAnalyze)
        .onErrorResume(error -> handleReviewError(review, error));
  }

  /** Collects diff and performs analysis. */
  private Mono<CodeReview> collectDiffAndAnalyze(CodeReview review) {
    return diffCollectionPort
        .collectDiff(review.getRepositoryInfo())
        .doOnNext(
            diffAnalysis -> {
              review.addDiffAnalysis(diffAnalysis);
              log.info(
                  "Collected diff: {} lines across {} files",
                  diffAnalysis.getTotalLineCount(),
                  diffAnalysis.getFileModifications().size());
            })
        .flatMap(diffAnalysis -> performAnalysis(review, diffAnalysis))
        .flatMap(updatedReview -> reviewRepository.save(updatedReview))
        .subscribeOn(Schedulers.boundedElastic());
  }

  /** Performs static and AI analysis. */
  private Mono<CodeReview> performAnalysis(
      CodeReview review, com.ghiloufi.aicode.domain.entity.DiffAnalysis diffAnalysis) {
    // Run static analysis if enabled
    Mono<Void> staticAnalysis = Mono.empty();
    if (review.getConfiguration().enableStaticAnalysis() && staticAnalysisPort.isAvailable()) {
      staticAnalysis =
          staticAnalysisPort
              .analyze(diffAnalysis)
              .doOnNext(
                  result -> {
                    review.addAnalysisResult(result);
                    log.debug("Static analysis completed: {} issues found", result.getIssueCount());
                  })
              .then();
    }

    // Run AI analysis if enabled
    Mono<Void> aiAnalysis = Mono.empty();
    if (review.getConfiguration().enableLlmAnalysis() && aiAnalysisPort.isAvailable()) {
      aiAnalysis =
          aiAnalysisPort
              .analyze(diffAnalysis, review.getConfiguration())
              .doOnNext(
                  result -> {
                    review.addAnalysisResult(result);
                    log.debug("AI analysis completed: {} issues found", result.getIssueCount());
                  })
              .then();
    }

    // Wait for all analyses to complete, then finalize
    return Mono.when(staticAnalysis, aiAnalysis)
        .then(Mono.fromCallable(() -> finalizeReview(review)))
        .flatMap(finalResult -> publishReviewIfNeeded(review, finalResult));
  }

  /** Finalizes the review by merging all analysis results. */
  private ReviewResult finalizeReview(CodeReview review) {
    // Merge all analysis results into final result
    // This is simplified - in reality, you'd implement sophisticated merging logic
    var allIssues =
        review.getAnalysisResults().stream()
            .flatMap(result -> result.getIssues().stream())
            .toList();

    var allNotes =
        review.getAnalysisResults().stream().flatMap(result -> result.getNotes().stream()).toList();

    String summary = generateSummary(review);
    ReviewResult finalResult = new ReviewResult(summary, allIssues, allNotes);

    review.complete(finalResult);
    log.info(
        "Review completed: {} issues found, quality: {}",
        finalResult.getIssueCount(),
        finalResult.getQuality());

    return finalResult;
  }

  /** Generates a summary of the review. */
  private String generateSummary(CodeReview review) {
    int totalIssues =
        review.getAnalysisResults().stream().mapToInt(AnalysisResult::getIssueCount).sum();

    return String.format(
        "Code review completed for %s. Found %d issue(s) across %d analysis tool(s).",
        review.getRepositoryInfo().getDisplayName(),
        totalIssues,
        review.getAnalysisResults().size());
  }

  /** Publishes review results if supported. */
  private Mono<CodeReview> publishReviewIfNeeded(CodeReview review, ReviewResult result) {
    if (reviewPublishingPort.supports(review)) {
      return reviewPublishingPort
          .publishReview(review, result)
          .then(reviewRepository.save(review))
          .doOnSuccess(r -> log.info("Review results published for {}", r.getId()));
    }
    return reviewRepository.save(review);
  }

  /** Handles review errors. */
  private Mono<CodeReview> handleReviewError(CodeReview review, Throwable error) {
    log.error("Review failed for {}: {}", review.getId(), error.getMessage(), error);
    review.markAsFailed(error.getMessage());
    return reviewRepository.save(review);
  }
}
