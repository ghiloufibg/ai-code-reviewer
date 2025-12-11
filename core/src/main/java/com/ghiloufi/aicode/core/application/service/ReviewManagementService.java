package com.ghiloufi.aicode.core.application.service;

import com.ghiloufi.aicode.core.application.service.context.ContextOrchestrator;
import com.ghiloufi.aicode.core.config.SummaryCommentProperties;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import com.ghiloufi.aicode.core.domain.model.ReviewConfiguration;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.input.ReviewManagementUseCase;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.core.domain.service.SummaryCommentFormatter;
import com.ghiloufi.aicode.core.infrastructure.factory.SCMProviderFactory;
import com.ghiloufi.aicode.core.infrastructure.observability.ReviewMetrics;
import com.ghiloufi.aicode.core.infrastructure.persistence.PostgresReviewRepository;
import com.ghiloufi.aicode.core.infrastructure.resilience.Resilience;
import com.ghiloufi.aicode.core.service.accumulator.ReviewChunkAccumulator;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SuppressWarnings("LoggingSimilarMessage")
@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewManagementService implements ReviewManagementUseCase {

  private final AIReviewStreamingService aiReviewStreamingService;
  private final SCMProviderFactory scmProviderFactory;
  private final ReviewChunkAccumulator chunkAccumulator;
  private final PostgresReviewRepository reviewRepository;
  private final ContextOrchestrator contextOrchestrator;
  private final SummaryCommentProperties summaryCommentProperties;
  private final SummaryCommentFormatter summaryCommentFormatter;
  private final Resilience resilience;
  private final ReviewMetrics reviewMetrics;

  @Override
  public Flux<ReviewChunk> streamReview(
      final RepositoryIdentifier repository, final ChangeRequestIdentifier changeRequest) {
    log.info(
        "Starting review for {}/{}", repository.getDisplayName(), changeRequest.getDisplayName());

    final ReviewConfiguration config = ReviewConfiguration.defaults();
    final SCMPort scmPort = scmProviderFactory.getProvider(repository.getProvider());

    return scmPort
        .getDiff(repository, changeRequest)
        .transform(resilience.criticalMono("scm-get-diff"))
        .doOnSuccess(
            diff ->
                log.debug(
                    "Fetched diff: {} files, {} lines",
                    diff.getModifiedFileCount(),
                    diff.getTotalLineCount()))
        .flatMap(contextOrchestrator::retrieveEnrichedContext)
        .transform(resilience.criticalMono("context-enrichment"))
        .doOnSuccess(
            enrichedDiff ->
                log.debug(
                    "Context enrichment complete: {} context matches",
                    enrichedDiff.getContextMatchCount()))
        .flatMapMany(
            enrichedDiff ->
                aiReviewStreamingService
                    .reviewCodeStreaming(enrichedDiff, config)
                    .transform(resilience.criticalFlux("ai-streaming")))
        .doOnNext(
            chunk ->
                log.debug("Streaming chunk: {} - {} chars", chunk.type(), chunk.content().length()))
        .doOnComplete(
            () ->
                log.info(
                    "Review completed for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName()));
  }

  @Override
  public Flux<ReviewChunk> streamAndPublishReview(
      final RepositoryIdentifier repository, final ChangeRequestIdentifier changeRequest) {
    log.info(
        "Starting review with auto-publish for {}/{}",
        repository.getDisplayName(),
        changeRequest.getDisplayName());

    final ReviewConfiguration config = ReviewConfiguration.defaults();
    final SCMPort scmPort = scmProviderFactory.getProvider(repository.getProvider());
    final List<ReviewChunk> accumulatedChunks = Collections.synchronizedList(new ArrayList<>());
    final Timer.Sample timerSample = reviewMetrics.startReviewTimer();
    final AtomicReference<Integer> issueCountRef = new AtomicReference<>(0);

    reviewMetrics.recordReviewInitiated(
        repository.getDisplayName(), repository.getProvider().name());

    return scmPort
        .getDiff(repository, changeRequest)
        .transform(resilience.criticalMono("scm-get-diff"))
        .doOnSuccess(
            diff ->
                log.debug(
                    "Fetched diff: {} files, {} lines",
                    diff.getModifiedFileCount(),
                    diff.getTotalLineCount()))
        .flatMap(contextOrchestrator::retrieveEnrichedContext)
        .transform(resilience.criticalMono("context-enrichment"))
        .doOnSuccess(
            enrichedDiff ->
                log.debug(
                    "Context enrichment complete: {} context matches",
                    enrichedDiff.getContextMatchCount()))
        .flatMapMany(
            enrichedDiff ->
                aiReviewStreamingService
                    .reviewCodeStreaming(enrichedDiff, config)
                    .transform(resilience.criticalFlux("ai-streaming")))
        .doOnNext(
            chunk -> {
              accumulatedChunks.add(chunk);
              log.debug(
                  "Accumulated chunk: {} ({} chars, total: {})",
                  chunk.type(),
                  chunk.content().length(),
                  accumulatedChunks.size());
            })
        .transform(
            flux ->
                chainPublishOperations(
                    flux,
                    accumulatedChunks,
                    repository,
                    changeRequest,
                    scmPort,
                    config,
                    issueCountRef))
        .doOnComplete(
            () -> {
              reviewMetrics.stopReviewTimer(timerSample);
              reviewMetrics.recordReviewCompleted(
                  repository.getDisplayName(),
                  repository.getProvider().name(),
                  issueCountRef.get(),
                  0);
            })
        .doOnError(
            error -> {
              reviewMetrics.recordReviewFailed(
                  repository.getDisplayName(),
                  repository.getProvider().name(),
                  error.getClass().getSimpleName());
              log.error(
                  "Review failed for {}/{}",
                  repository.getDisplayName(),
                  changeRequest.getDisplayName(),
                  error);
            });
  }

  private Flux<ReviewChunk> chainPublishOperations(
      final Flux<ReviewChunk> sourceFlux,
      final List<ReviewChunk> accumulatedChunks,
      final RepositoryIdentifier repository,
      final ChangeRequestIdentifier changeRequest,
      final SCMPort scmPort,
      final ReviewConfiguration config,
      final AtomicReference<Integer> issueCountRef) {
    return sourceFlux.concatWith(
        Mono.defer(
                () -> {
                  log.info("Streaming completed: {} chunks", accumulatedChunks.size());

                  if (accumulatedChunks.isEmpty()) {
                    log.warn(
                        "No chunks to publish for {}/{}",
                        repository.getDisplayName(),
                        changeRequest.getDisplayName());
                    return Mono.empty();
                  }

                  log.info(
                      "Publishing review for {}/{}",
                      repository.getDisplayName(),
                      changeRequest.getDisplayName());

                  final ReviewConfiguration llmMetadata = aiReviewStreamingService.getLlmMetadata();
                  final ReviewResult result =
                      chunkAccumulator
                          .accumulateChunks(accumulatedChunks, config)
                          .withLlmMetadata(llmMetadata.llmProvider(), llmMetadata.llmModel());

                  issueCountRef.set(result.getIssues().size());

                  return publishAndSaveReview(repository, changeRequest, result, scmPort);
                })
            .then(Mono.empty()));
  }

  private Mono<Void> publishAndSaveReview(
      final RepositoryIdentifier repository,
      final ChangeRequestIdentifier changeRequest,
      final ReviewResult result,
      final SCMPort scmPort) {
    final String reviewId =
        repository.getDisplayName()
            + "_"
            + changeRequest.getNumber()
            + "_"
            + repository.getProvider().name().toLowerCase();
    final ReviewResult filteredResult = filterHighConfidenceIssues(result);

    return publishSummaryComment(repository, changeRequest, result)
        .then(
            scmPort
                .publishReview(repository, changeRequest, filteredResult)
                .transform(resilience.criticalMono("scm-publish-review"))
                .doOnSuccess(
                    v ->
                        log.info(
                            "Inline comments published successfully for {}/{}",
                            repository.getDisplayName(),
                            changeRequest.getDisplayName())))
        .then(
            Mono.defer(
                    () -> {
                      log.debug("Saving review to database: {}", reviewId);
                      return reviewRepository.save(reviewId, result);
                    })
                .transform(resilience.bestEffortMono("db-save-review")))
        .doOnSuccess(
            v ->
                log.info(
                    "Review saved to database for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName()))
        .doOnError(
            error ->
                log.error(
                    "Failed to publish/save review for {}/{}",
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
  public Mono<Void> publishReview(
      final RepositoryIdentifier repository,
      final ChangeRequestIdentifier changeRequest,
      final ReviewResult reviewResult) {
    log.info(
        "Publishing review for {}/{}", repository.getDisplayName(), changeRequest.getDisplayName());

    final SCMPort scmPort = scmProviderFactory.getProvider(repository.getProvider());
    final ReviewConfiguration llmMetadata = aiReviewStreamingService.getLlmMetadata();
    final ReviewResult enrichedResult =
        reviewResult.withLlmMetadata(llmMetadata.llmProvider(), llmMetadata.llmModel());
    final ReviewResult filteredResult = filterHighConfidenceIssues(enrichedResult);

    return publishSummaryComment(repository, changeRequest, enrichedResult)
        .then(
            scmPort
                .publishReview(repository, changeRequest, filteredResult)
                .transform(resilience.criticalMono("scm-publish-review"))
                .doOnSuccess(
                    v ->
                        log.info(
                            "Inline comments published for {}/{}",
                            repository.getDisplayName(),
                            changeRequest.getDisplayName()))
                .doOnError(
                    error ->
                        log.error(
                            "Failed to publish inline comments for {}/{}",
                            repository.getDisplayName(),
                            changeRequest.getDisplayName(),
                            error)))
        .then(
            Mono.defer(
                    () -> {
                      final String reviewId =
                          repository.getDisplayName()
                              + "_"
                              + changeRequest.getNumber()
                              + "_"
                              + repository.getProvider().name().toLowerCase();
                      log.debug("Saving review to database: {}", reviewId);
                      return reviewRepository.save(reviewId, enrichedResult);
                    })
                .transform(resilience.bestEffortMono("db-save-review")))
        .doOnSuccess(
            v ->
                log.info(
                    "Review saved to database for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName()))
        .doOnError(
            error ->
                log.error(
                    "Failed to save review to database for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName(),
                    error));
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

  @Override
  public Flux<RepositoryInfo> getAllRepositories(final SourceProvider provider) {
    log.info("Fetching all repositories for provider: {}", provider);

    final SCMPort scmPort = scmProviderFactory.getProvider(provider);

    return scmPort
        .getAllRepositories()
        .transform(resilience.criticalFlux("scm-get-merge-requests"))
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
        "Publishing async review result: provider={}, repository={}, changeRequest={}",
        provider,
        repositoryId,
        changeRequestId);

    final RepositoryIdentifier repository = RepositoryIdentifier.create(provider, repositoryId);
    final ChangeRequestIdentifier changeRequest =
        ChangeRequestIdentifier.create(provider, changeRequestId);
    final SCMPort scmPort = scmProviderFactory.getProvider(provider);
    final ReviewResult filteredResult = filterHighConfidenceIssues(reviewResult);

    return publishSummaryComment(repository, changeRequest, reviewResult)
        .then(
            scmPort
                .publishReview(repository, changeRequest, filteredResult)
                .transform(resilience.criticalMono("scm-publish-review"))
                .doOnSuccess(
                    v ->
                        log.info(
                            "Async review published successfully for {}/{}",
                            repository.getDisplayName(),
                            changeRequest.getDisplayName())))
        .then(
            Mono.defer(
                    () -> {
                      final String reviewId =
                          repository.getDisplayName()
                              + "_"
                              + changeRequest.getNumber()
                              + "_"
                              + provider.name().toLowerCase();
                      log.debug("Saving async review to database: {}", reviewId);
                      return reviewRepository.save(reviewId, reviewResult);
                    })
                .transform(resilience.bestEffortMono("db-save-review")))
        .doOnSuccess(
            v ->
                log.info(
                    "Async review saved to database for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName()))
        .doOnError(
            error ->
                log.error(
                    "Failed to publish/save async review for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName(),
                    error));
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
