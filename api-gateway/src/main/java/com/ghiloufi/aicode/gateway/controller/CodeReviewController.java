package com.ghiloufi.aicode.gateway.controller;

import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import com.ghiloufi.aicode.core.domain.port.input.ReviewManagementUseCase;
import com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewIssueRepository;
import com.ghiloufi.aicode.gateway.formatter.SSEFormatter;
import jakarta.validation.constraints.Positive;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/reviews")
@Validated
@RequiredArgsConstructor
public class CodeReviewController {

  private final ReviewManagementUseCase reviewManagementUseCase;
  private final SSEFormatter sseFormatter;
  private final ReviewIssueRepository reviewIssueRepository;

  @GetMapping(
      value = "/{provider}/{repositoryId}/change-requests/{changeRequestId}/stream",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> streamReviewAnalysis(
      @PathVariable final String provider,
      @PathVariable final String repositoryId,
      @PathVariable @Positive final int changeRequestId) {

    final SourceProvider sourceProvider = SourceProvider.fromString(provider);
    final String decodedRepositoryId = URLDecoder.decode(repositoryId, StandardCharsets.UTF_8);

    log.info(
        "Starting code review stream: provider={}, repository={}, changeRequest={}",
        sourceProvider,
        decodedRepositoryId,
        changeRequestId);

    final RepositoryIdentifier repository =
        RepositoryIdentifier.create(sourceProvider, decodedRepositoryId);
    final ChangeRequestIdentifier changeRequest =
        ChangeRequestIdentifier.create(sourceProvider, changeRequestId);

    log.debug(
        "Created domain identifiers: repository={}, changeRequest={}",
        repository.getClass().getSimpleName(),
        changeRequest.getClass().getSimpleName());

    return reviewManagementUseCase
        .streamReview(repository, changeRequest)
        .map(sseFormatter::formatReviewChunk)
        .concatWith(Mono.just(sseFormatter.formatDone()))
        .doOnSubscribe(
            s ->
                log.info(
                    "Client subscribed to review stream: provider={}, repo={}, cr={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId))
        .doOnNext(
            sse ->
                log.debug(
                    "Streaming SSE chunk: {} bytes",
                    Optional.ofNullable(sse).map(String::length).orElse(0)))
        .doOnComplete(
            () ->
                log.info(
                    "Review stream completed: provider={}, repo={}, cr={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId))
        .doOnError(
            error ->
                log.error(
                    "Review stream error: provider={}, repo={}, cr={}, error={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId,
                    error.getMessage()))
        .onErrorResume(error -> Flux.just(sseFormatter.formatError(error)))
        .timeout(Duration.ofMinutes(10));
  }

  @GetMapping(
      value = "/{provider}/{repositoryId}/change-requests/{changeRequestId}/stream-and-publish",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> streamReviewAndPublish(
      @PathVariable final String provider,
      @PathVariable final String repositoryId,
      @PathVariable @Positive final int changeRequestId) {

    final SourceProvider sourceProvider = SourceProvider.fromString(provider);
    final String decodedRepositoryId = URLDecoder.decode(repositoryId, StandardCharsets.UTF_8);

    log.info(
        "Starting code review stream with auto-publish: provider={}, repository={}, changeRequest={}",
        sourceProvider,
        decodedRepositoryId,
        changeRequestId);

    final RepositoryIdentifier repository =
        RepositoryIdentifier.create(sourceProvider, decodedRepositoryId);
    final ChangeRequestIdentifier changeRequest =
        ChangeRequestIdentifier.create(sourceProvider, changeRequestId);

    log.debug(
        "Created domain identifiers for stream-and-publish: repository={}, changeRequest={}",
        repository.getClass().getSimpleName(),
        changeRequest.getClass().getSimpleName());

    return reviewManagementUseCase
        .streamAndPublishReview(repository, changeRequest)
        .map(sseFormatter::formatReviewChunk)
        .concatWith(Mono.just(sseFormatter.formatPublished()))
        .doOnSubscribe(
            s ->
                log.info(
                    "Client subscribed to review stream with auto-publish: provider={}, repo={}, cr={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId))
        .doOnNext(
            sse ->
                log.debug(
                    "Streaming SSE chunk: {} bytes",
                    Optional.ofNullable(sse).map(String::length).orElse(0)))
        .doOnComplete(
            () ->
                log.info(
                    "Review stream with auto-publish completed: provider={}, repo={}, cr={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId))
        .doOnError(
            error ->
                log.error(
                    "Review stream with auto-publish error: provider={}, repo={}, cr={}, error={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId,
                    error.getMessage()))
        .onErrorResume(error -> Flux.just(sseFormatter.formatError(error)))
        .timeout(Duration.ofMinutes(10));
  }

  @PostMapping(
      value = "/{provider}/{repositoryId}/change-requests/{changeRequestId}/review",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Map<String, Object>> publishReview(
      @PathVariable final String provider,
      @PathVariable final String repositoryId,
      @PathVariable @Positive final int changeRequestId,
      @RequestBody final ReviewResult reviewResult) {

    final SourceProvider sourceProvider = SourceProvider.fromString(provider);
    final String decodedRepositoryId = URLDecoder.decode(repositoryId, StandardCharsets.UTF_8);

    log.info(
        "Publishing review: provider={}, repository={}, changeRequest={}",
        sourceProvider,
        decodedRepositoryId,
        changeRequestId);

    final RepositoryIdentifier repository =
        RepositoryIdentifier.create(sourceProvider, decodedRepositoryId);
    final ChangeRequestIdentifier changeRequest =
        ChangeRequestIdentifier.create(sourceProvider, changeRequestId);

    log.debug(
        "Created domain identifiers for publish: repository={}, changeRequest={}",
        repository.getClass().getSimpleName(),
        changeRequest.getClass().getSimpleName());

    return reviewManagementUseCase
        .publishReview(repository, changeRequest, reviewResult)
        .then(
            Mono.fromSupplier(
                () ->
                    Map.<String, Object>of(
                        "status",
                        "success",
                        "message",
                        "Review published successfully",
                        "provider",
                        sourceProvider.name(),
                        "repository",
                        decodedRepositoryId,
                        "changeRequestId",
                        changeRequestId,
                        "timestamp",
                        System.currentTimeMillis())))
        .doOnSubscribe(
            s ->
                log.info(
                    "Client initiated review publish: provider={}, repo={}, cr={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId))
        .doOnSuccess(
            response ->
                log.info(
                    "Review published successfully: provider={}, repo={}, cr={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId))
        .doOnError(
            error ->
                log.error(
                    "Review publish error: provider={}, repo={}, cr={}, error={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId,
                    error.getMessage()))
        .onErrorResume(
            error ->
                Mono.just(
                    Map.<String, Object>of(
                        "status",
                        "error",
                        "message",
                        error.getMessage() != null
                            ? error.getMessage()
                            : "Failed to publish review",
                        "provider",
                        sourceProvider.name(),
                        "repository",
                        decodedRepositoryId,
                        "changeRequestId",
                        changeRequestId,
                        "timestamp",
                        System.currentTimeMillis())))
        .timeout(Duration.ofSeconds(30));
  }

  @GetMapping(value = "/{provider}/repositories", produces = MediaType.APPLICATION_JSON_VALUE)
  public Flux<RepositoryInfo> getAllRepositories(@PathVariable final String provider) {

    final SourceProvider sourceProvider = SourceProvider.fromString(provider);

    log.info("Fetching all repositories for provider: {}", sourceProvider);

    return reviewManagementUseCase
        .getAllRepositories(sourceProvider)
        .doOnSubscribe(
            s -> log.info("Client subscribed to repositories list: provider={}", sourceProvider))
        .doOnNext(repo -> log.debug("Streaming repository: {} - {}", repo.id(), repo.name()))
        .doOnComplete(() -> log.info("Repositories list completed: provider={}", sourceProvider))
        .doOnError(
            error ->
                log.error(
                    "Repositories list error: provider={}, error={}",
                    sourceProvider,
                    error.getMessage()))
        .timeout(Duration.ofSeconds(30));
  }

  @GetMapping(
      value = "/{provider}/{repositoryId}/change-requests",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Flux<MergeRequestSummary> getOpenChangeRequests(
      @PathVariable final String provider, @PathVariable final String repositoryId) {

    final SourceProvider sourceProvider = SourceProvider.fromString(provider);
    final String decodedRepositoryId = URLDecoder.decode(repositoryId, StandardCharsets.UTF_8);

    log.info(
        "Fetching open change requests: provider={}, repository={}",
        sourceProvider,
        decodedRepositoryId);

    final RepositoryIdentifier repository =
        RepositoryIdentifier.create(sourceProvider, decodedRepositoryId);

    log.debug("Created repository identifier: {}", repository.getClass().getSimpleName());

    return reviewManagementUseCase
        .getOpenChangeRequests(repository)
        .doOnSubscribe(
            s ->
                log.info(
                    "Client subscribed to change requests list: provider={}, repo={}",
                    sourceProvider,
                    repositoryId))
        .doOnNext(mr -> log.debug("Streaming change request: #{} - {}", mr.iid(), mr.title()))
        .doOnComplete(
            () ->
                log.info(
                    "Change requests list completed: provider={}, repo={}",
                    sourceProvider,
                    repositoryId))
        .doOnError(
            error ->
                log.error(
                    "Change requests list error: provider={}, repo={}, error={}",
                    sourceProvider,
                    repositoryId,
                    error.getMessage()))
        .timeout(Duration.ofSeconds(30));
  }

  @GetMapping(value = "/issues/{issueId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Map<String, Object>> getIssue(@PathVariable final UUID issueId) {

    log.info("Retrieving issue: {}", issueId);

    return Mono.fromCallable(() -> reviewIssueRepository.findByIdWithReview(issueId))
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .flatMap(
            optionalIssue ->
                optionalIssue
                    .map(
                        issue ->
                            Mono.just(
                                Map.<String, Object>ofEntries(
                                    Map.entry("id", issue.getId().toString()),
                                    Map.entry("filePath", issue.getFilePath()),
                                    Map.entry("startLine", issue.getStartLine()),
                                    Map.entry("severity", issue.getSeverity()),
                                    Map.entry("title", issue.getTitle()),
                                    Map.entry(
                                        "suggestion",
                                        issue.getSuggestion() != null ? issue.getSuggestion() : ""),
                                    Map.entry(
                                        "confidenceScore",
                                        issue.getConfidenceScore() != null
                                            ? issue.getConfidenceScore()
                                            : 0.0),
                                    Map.entry(
                                        "confidenceExplanation",
                                        issue.getConfidenceExplanation() != null
                                            ? issue.getConfidenceExplanation()
                                            : ""),
                                    Map.entry(
                                        "suggestedFix",
                                        issue.getSuggestedFix() != null
                                            ? issue.getSuggestedFix()
                                            : ""),
                                    Map.entry(
                                        "fixDiff",
                                        issue.getFixDiff() != null ? issue.getFixDiff() : ""))))
                    .orElseGet(
                        () ->
                            Mono.just(
                                Map.<String, Object>of(
                                    "status",
                                    "error",
                                    "message",
                                    "Issue not found: " + issueId,
                                    "timestamp",
                                    System.currentTimeMillis()))))
        .doOnSuccess(response -> log.info("Retrieved issue: {}", issueId))
        .doOnError(error -> log.error("Failed to retrieve issue: {}", issueId, error))
        .timeout(Duration.ofSeconds(10));
  }
}
