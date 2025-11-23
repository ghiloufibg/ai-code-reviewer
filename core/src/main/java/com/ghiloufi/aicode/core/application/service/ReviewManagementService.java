package com.ghiloufi.aicode.core.application.service;

import com.ghiloufi.aicode.core.application.service.context.ContextOrchestrator;
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
import com.ghiloufi.aicode.core.infrastructure.factory.SCMProviderFactory;
import com.ghiloufi.aicode.core.infrastructure.persistence.PostgresReviewRepository;
import com.ghiloufi.aicode.core.service.accumulator.ReviewChunkAccumulator;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SuppressWarnings("LoggingSimilarMessage")
@Service
@Slf4j
public class ReviewManagementService implements ReviewManagementUseCase {

  private final AIReviewStreamingService aiReviewStreamingService;
  private final SCMProviderFactory scmProviderFactory;
  private final ReviewChunkAccumulator chunkAccumulator;
  private final PostgresReviewRepository reviewRepository;
  private final ContextOrchestrator contextOrchestrator;

  public ReviewManagementService(
      final AIReviewStreamingService aiReviewStreamingService,
      final SCMProviderFactory scmProviderFactory,
      final ReviewChunkAccumulator chunkAccumulator,
      final PostgresReviewRepository reviewRepository,
      final ContextOrchestrator contextOrchestrator) {
    this.aiReviewStreamingService = aiReviewStreamingService;
    this.scmProviderFactory = scmProviderFactory;
    this.chunkAccumulator = chunkAccumulator;
    this.reviewRepository = reviewRepository;
    this.contextOrchestrator = contextOrchestrator;
  }

  @Override
  public Flux<ReviewChunk> streamReview(
      final RepositoryIdentifier repository, final ChangeRequestIdentifier changeRequest) {
    log.info(
        "Starting review for {}/{}", repository.getDisplayName(), changeRequest.getDisplayName());

    final ReviewConfiguration config = ReviewConfiguration.defaults();
    final SCMPort scmPort = scmProviderFactory.getProvider(repository.getProvider());

    return scmPort
        .getDiff(repository, changeRequest)
        .doOnSuccess(
            diff ->
                log.debug(
                    "Fetched diff: {} files, {} lines",
                    diff.getModifiedFileCount(),
                    diff.getTotalLineCount()))
        .flatMap(contextOrchestrator::retrieveEnrichedContext)
        .doOnSuccess(
            enrichedDiff ->
                log.debug(
                    "Context enrichment complete: {} context matches",
                    enrichedDiff.getContextMatchCount()))
        .flatMapMany(
            enrichedDiff -> aiReviewStreamingService.reviewCodeStreaming(enrichedDiff, config))
        .doOnNext(
            chunk ->
                log.debug("Streaming chunk: {} - {} chars", chunk.type(), chunk.content().length()))
        .doOnComplete(
            () ->
                log.info(
                    "Review completed for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName()))
        .doOnError(
            error ->
                log.error(
                    "Review failed for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName(),
                    error));
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
    final List<ReviewChunk> accumulatedChunks = new ArrayList<>();

    return scmPort
        .getDiff(repository, changeRequest)
        .doOnSuccess(
            diff ->
                log.debug(
                    "Fetched diff: {} files, {} lines",
                    diff.getModifiedFileCount(),
                    diff.getTotalLineCount()))
        .flatMap(contextOrchestrator::retrieveEnrichedContext)
        .doOnSuccess(
            enrichedDiff ->
                log.debug(
                    "Context enrichment complete: {} context matches",
                    enrichedDiff.getContextMatchCount()))
        .flatMapMany(
            enrichedDiff -> aiReviewStreamingService.reviewCodeStreaming(enrichedDiff, config))
        .doOnNext(
            chunk -> {
              accumulatedChunks.add(chunk);
              log.debug(
                  "Accumulated chunk: {} ({} chars, total: {})",
                  chunk.type(),
                  chunk.content().length(),
                  accumulatedChunks.size());
            })
        .doOnComplete(
            () -> {
              log.info("Streaming completed: {} chunks", accumulatedChunks.size());

              if (!accumulatedChunks.isEmpty()) {
                log.info(
                    "Publishing review for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName());

                final ReviewResult result =
                    chunkAccumulator.accumulateChunks(accumulatedChunks, config);
                final ReviewConfiguration llmMetadata = aiReviewStreamingService.getLlmMetadata();
                result.llmProvider = llmMetadata.llmProvider();
                result.llmModel = llmMetadata.llmModel();

                scmPort
                    .publishReview(repository, changeRequest, result)
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
                                error))
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
                              return reviewRepository.save(reviewId, result);
                            }))
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
                                error))
                    .subscribe();
              } else {
                log.warn(
                    "No chunks to publish for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName());
              }
            })
        .doOnError(
            error ->
                log.error(
                    "Streaming failed for {}/{}",
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
    reviewResult.llmProvider = llmMetadata.llmProvider();
    reviewResult.llmModel = llmMetadata.llmModel();

    return scmPort
        .publishReview(repository, changeRequest, reviewResult)
        .doOnSuccess(
            v ->
                log.info(
                    "Review published for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName()))
        .doOnError(
            error ->
                log.error(
                    "Failed to publish review for {}/{}",
                    repository.getDisplayName(),
                    changeRequest.getDisplayName(),
                    error))
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
                  return reviewRepository.save(reviewId, reviewResult);
                }))
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

  @Override
  public Flux<RepositoryInfo> getAllRepositories(final SourceProvider provider) {
    log.info("Fetching all repositories for provider: {}", provider);

    final SCMPort scmPort = scmProviderFactory.getProvider(provider);

    return scmPort
        .getAllRepositories()
        .doOnNext(repo -> log.debug("Found repository: {}", repo.fullName()))
        .doOnComplete(() -> log.info("Completed fetching repositories for: {}", provider))
        .doOnError(error -> log.error("Failed to fetch repositories for: {}", provider, error));
  }

}
