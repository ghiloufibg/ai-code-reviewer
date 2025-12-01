package com.ghiloufi.aicode.gateway.controller;

import com.ghiloufi.aicode.core.application.service.FixApplicationService;
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
  private final Optional<FixApplicationService> fixApplicationService;
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
                                        issue.getFixDiff() != null ? issue.getFixDiff() : ""),
                                    Map.entry("canApplyFix", issue.canApplyFix()),
                                    Map.entry("fixApplied", issue.isFixApplied()),
                                    Map.entry(
                                        "appliedAt",
                                        issue.getAppliedAt() != null
                                            ? issue.getAppliedAt().toEpochMilli()
                                            : null),
                                    Map.entry(
                                        "appliedCommitSha",
                                        issue.getAppliedCommitSha() != null
                                            ? issue.getAppliedCommitSha()
                                            : ""))))
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

  @PostMapping(
      value =
          "/{provider}/{repositoryId}/change-requests/{changeRequestId}/issues/{issueId}/apply-fix",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Map<String, Object>> applyFixByIssueId(
      @PathVariable final String provider,
      @PathVariable final String repositoryId,
      @PathVariable @Positive final int changeRequestId,
      @PathVariable final UUID issueId) {

    final SourceProvider sourceProvider = SourceProvider.fromString(provider);
    final String decodedRepositoryId = URLDecoder.decode(repositoryId, StandardCharsets.UTF_8);

    log.info(
        "Applying AI-generated fix: provider={}, repository={}, changeRequest={}, issueId={}",
        sourceProvider,
        decodedRepositoryId,
        changeRequestId,
        issueId);

    final RepositoryIdentifier repository =
        RepositoryIdentifier.create(sourceProvider, decodedRepositoryId);
    final ChangeRequestIdentifier changeRequest =
        ChangeRequestIdentifier.create(sourceProvider, changeRequestId);

    log.debug(
        "Created domain identifiers for fix application: repository={}, changeRequest={}",
        repository.getClass().getSimpleName(),
        changeRequest.getClass().getSimpleName());

    return fixApplicationService
        .map(service -> service.applyFixByIssueId(repository, changeRequest, issueId))
        .orElseGet(
            () ->
                Mono.error(
                    new UnsupportedOperationException(
                        "Fix application feature is disabled. Enable it in configuration: features.fix-application.enabled=true")))
        .map(
            commitResult ->
                Map.<String, Object>of(
                    "status",
                    "success",
                    "message",
                    "Fix applied successfully",
                    "commitSha",
                    commitResult.commitSha(),
                    "commitUrl",
                    commitResult.commitUrl(),
                    "branchName",
                    commitResult.branchName(),
                    "filesModified",
                    commitResult.filesModified(),
                    "timestamp",
                    commitResult.createdAt().toEpochMilli()))
        .doOnSubscribe(
            s ->
                log.info(
                    "Client initiated fix application: provider={}, repo={}, cr={}, issueId={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId,
                    issueId))
        .doOnSuccess(
            response ->
                log.info(
                    "Fix applied successfully: provider={}, repo={}, cr={}, issueId={}, commit={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId,
                    issueId,
                    response.get("commitSha")))
        .doOnError(
            error ->
                log.error(
                    "Fix application error: provider={}, repo={}, cr={}, issueId={}, error={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId,
                    issueId,
                    error.getMessage()))
        .onErrorResume(
            error ->
                Mono.just(
                    Map.<String, Object>of(
                        "status",
                        "error",
                        "message",
                        error.getMessage() != null ? error.getMessage() : "Failed to apply fix",
                        "provider",
                        sourceProvider.name(),
                        "repository",
                        decodedRepositoryId,
                        "changeRequestId",
                        changeRequestId,
                        "issueId",
                        issueId.toString(),
                        "timestamp",
                        System.currentTimeMillis())))
        .timeout(Duration.ofSeconds(30));
  }

  @Deprecated(forRemoval = true)
  @PostMapping(
      value = "/{provider}/{repositoryId}/change-requests/{changeRequestId}/apply-fix",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Map<String, Object>> applyFixDeprecated(
      @PathVariable final String provider,
      @PathVariable final String repositoryId,
      @PathVariable @Positive final int changeRequestId,
      @RequestBody @jakarta.validation.Valid final FixApplicationRequest request) {

    final SourceProvider sourceProvider = SourceProvider.fromString(provider);
    final String decodedRepositoryId = URLDecoder.decode(repositoryId, StandardCharsets.UTF_8);

    log.warn(
        "DEPRECATED: Using deprecated apply-fix endpoint. Please migrate to /issues/{{issueId}}/apply-fix");
    log.info(
        "Applying fix: provider={}, repository={}, changeRequest={}, file={}",
        sourceProvider,
        decodedRepositoryId,
        changeRequestId,
        request.filePath());

    final RepositoryIdentifier repository =
        RepositoryIdentifier.create(sourceProvider, decodedRepositoryId);
    final ChangeRequestIdentifier changeRequest =
        ChangeRequestIdentifier.create(sourceProvider, changeRequestId);

    return fixApplicationService
        .map(
            service ->
                service.applyFix(
                    repository,
                    changeRequest,
                    request.filePath(),
                    request.fixDiff(),
                    request.issueTitle()))
        .orElseGet(
            () ->
                Mono.error(
                    new UnsupportedOperationException(
                        "Fix application feature is disabled. Enable it in configuration: features.fix-application.enabled=true")))
        .map(
            commitResult ->
                Map.<String, Object>of(
                    "status",
                    "success",
                    "message",
                    "Fix applied successfully",
                    "commitSha",
                    commitResult.commitSha(),
                    "commitUrl",
                    commitResult.commitUrl(),
                    "branchName",
                    commitResult.branchName(),
                    "filesModified",
                    commitResult.filesModified(),
                    "timestamp",
                    commitResult.createdAt().toEpochMilli()))
        .doOnSuccess(
            response ->
                log.info(
                    "Fix applied successfully: provider={}, repo={}, cr={}, file={}, commit={}",
                    sourceProvider,
                    repositoryId,
                    changeRequestId,
                    request.filePath(),
                    response.get("commitSha")))
        .onErrorResume(
            error ->
                Mono.just(
                    Map.<String, Object>of(
                        "status",
                        "error",
                        "message",
                        error.getMessage() != null ? error.getMessage() : "Failed to apply fix",
                        "provider",
                        sourceProvider.name(),
                        "repository",
                        decodedRepositoryId,
                        "changeRequestId",
                        changeRequestId,
                        "filePath",
                        request.filePath(),
                        "timestamp",
                        System.currentTimeMillis())))
        .timeout(Duration.ofSeconds(30));
  }

  public record FixApplicationRequest(
      @jakarta.validation.constraints.NotBlank(message = "File path is required") String filePath,
      @jakarta.validation.constraints.NotBlank(message = "Fix diff is required") String fixDiff,
      @jakarta.validation.constraints.NotBlank(message = "Issue title is required")
          String issueTitle) {}
}
