package com.ghiloufi.aicode.core.application.service;

import com.ghiloufi.aicode.core.config.SummaryCommentProperties;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.input.ReviewManagementUseCase;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.core.domain.service.SummaryCommentFormatter;
import com.ghiloufi.aicode.core.infrastructure.factory.SCMProviderFactory;
import com.ghiloufi.aicode.core.infrastructure.observability.ReviewMetrics;
import com.ghiloufi.aicode.core.infrastructure.persistence.PostgresReviewRepository;
import com.ghiloufi.aicode.core.infrastructure.resilience.Resilience;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewManagementService implements ReviewManagementUseCase {

  private final SCMProviderFactory scmProviderFactory;
  private final PostgresReviewRepository reviewRepository;
  private final SummaryCommentProperties summaryCommentProperties;
  private final SummaryCommentFormatter summaryCommentFormatter;
  private final Resilience resilience;
  private final ReviewMetrics reviewMetrics;

  @Override
  public Mono<Void> publishReview(
      final RepositoryIdentifier repository,
      final ChangeRequestIdentifier changeRequest,
      final ReviewResult reviewResult) {
    log.info(
        "Publishing pre-computed review for {}/{}",
        repository.getDisplayName(),
        changeRequest.getDisplayName());

    final SCMPort scmPort = scmProviderFactory.getProvider(repository.getProvider());
    final ReviewResult filteredResult = filterHighConfidenceIssues(reviewResult);

    return publishSummaryComment(repository, changeRequest, reviewResult)
        .then(
            scmPort
                .publishReview(repository, changeRequest, filteredResult)
                .transform(resilience.criticalMono("scm-publish-review")))
        .then(saveToDatabase(repository, changeRequest, reviewResult))
        .doOnSuccess(
            v ->
                log.info(
                    "Review published successfully for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName()))
        .doOnError(
            error ->
                log.error(
                    "Failed to publish review for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName(),
                    error));
  }

  @Override
  public Flux<MergeRequestSummary> getOpenChangeRequests(final RepositoryIdentifier repository) {
    log.info("Fetching open change requests for: {}", repository.getDisplayName());

    final SCMPort scmPort = scmProviderFactory.getProvider(repository.getProvider());

    return scmPort
        .getOpenChangeRequests(repository)
        .transform(resilience.criticalFlux("scm-get-merge-requests"))
        .doOnNext(mr -> log.debug("Found open change request: #{} - {}", mr.iid(), mr.title()))
        .doOnComplete(
            () ->
                log.info("Completed fetching change requests for: {}", repository.getDisplayName()))
        .doOnError(
            error ->
                log.error(
                    "Failed to fetch change requests for: {}", repository.getDisplayName(), error));
  }

  @Override
  public Flux<RepositoryInfo> getAllRepositories(final SourceProvider provider) {
    log.info("Fetching all repositories for provider: {}", provider);

    final SCMPort scmPort = scmProviderFactory.getProvider(provider);

    return scmPort
        .getAllRepositories()
        .transform(resilience.criticalFlux("scm-get-repositories"))
        .doOnNext(repo -> log.debug("Found repository: {}", repo.fullName()))
        .doOnComplete(() -> log.info("Completed fetching repositories for: {}", provider))
        .doOnError(error -> log.error("Failed to fetch repositories for: {}", provider, error));
  }

  @Override
  public Mono<Void> publishReviewFromAsync(
      final SourceProvider provider,
      final String repositoryId,
      final int changeRequestId,
      final ReviewResult reviewResult) {
    log.info(
        "Publishing async review: provider={}, repo={}, cr={}",
        provider,
        repositoryId,
        changeRequestId);

    final RepositoryIdentifier repository = RepositoryIdentifier.create(provider, repositoryId);
    final ChangeRequestIdentifier changeRequest =
        ChangeRequestIdentifier.create(provider, changeRequestId);

    return publishReview(repository, changeRequest, reviewResult);
  }

  private Mono<Void> publishSummaryComment(
      final RepositoryIdentifier repository,
      final ChangeRequestIdentifier changeRequest,
      final ReviewResult reviewResult) {
    if (!summaryCommentProperties.isEnabled()) {
      log.debug("Summary comment feature is disabled, skipping");
      return Mono.empty();
    }

    log.info(
        "Publishing summary comment for {}/{}",
        repository.getDisplayName(),
        changeRequest.getDisplayName());

    final String summaryComment = summaryCommentFormatter.formatSummaryComment(reviewResult);
    final SCMPort scmPort = scmProviderFactory.getProvider(repository.getProvider());

    return scmPort
        .publishSummaryComment(repository, changeRequest, summaryComment)
        .transform(resilience.bestEffortMono("scm-publish-summary"))
        .doOnSuccess(
            v ->
                log.info(
                    "Summary comment published for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName()));
  }

  private Mono<Void> saveToDatabase(
      final RepositoryIdentifier repository,
      final ChangeRequestIdentifier changeRequest,
      final ReviewResult reviewResult) {
    final String reviewId =
        repository.getDisplayName()
            + "_"
            + changeRequest.getNumber()
            + "_"
            + repository.getProvider().name().toLowerCase();

    return Mono.defer(() -> reviewRepository.save(reviewId, reviewResult))
        .transform(resilience.bestEffortMono("db-save-review"))
        .doOnSuccess(v -> log.debug("Saved review to database: {}", reviewId));
  }

  private ReviewResult filterHighConfidenceIssues(final ReviewResult reviewResult) {
    final List<ReviewResult.Issue> highConfidenceIssues =
        reviewResult.getIssues().stream().filter(ReviewResult.Issue::isHighConfidence).toList();

    final int filteredCount = reviewResult.getIssues().size() - highConfidenceIssues.size();
    if (filteredCount > 0) {
      log.info(
          "Filtered {} low-confidence issues from SCM publishing (kept {} high-confidence)",
          filteredCount,
          highConfidenceIssues.size());
      reviewMetrics.recordLowConfidenceFiltered(filteredCount);
    }

    return reviewResult.withIssues(highConfidenceIssues);
  }
}
