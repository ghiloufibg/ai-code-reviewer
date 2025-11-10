package com.ghiloufi.aicode.core.infrastructure.adapter;

import com.ghiloufi.aicode.core.domain.model.*;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.core.domain.service.GitLabDiffBuilder;
import com.ghiloufi.aicode.core.domain.service.GitLabMergeRequestMapper;
import com.ghiloufi.aicode.core.domain.service.GitLabProjectMapper;
import com.ghiloufi.aicode.core.domain.service.ReviewResultFormatter;
import com.ghiloufi.aicode.core.domain.service.SCMIdentifierValidator;
import com.ghiloufi.aicode.core.service.diff.UnifiedDiffParser;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Diff;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "scm.providers.gitlab", name = "enabled", havingValue = "true")
public class GitLabAdapter implements SCMPort {

  private final GitLabApi gitLabApi;
  private final UnifiedDiffParser diffParser;
  private final ReviewResultFormatter reviewResultFormatter;
  private final SCMIdentifierValidator identifierValidator;
  private final GitLabDiffBuilder diffBuilder;
  private final GitLabMergeRequestMapper mergeRequestMapper;
  private final GitLabProjectMapper projectMapper;

  public GitLabAdapter(
      @Value("${scm.providers.gitlab.api-url}") final String apiUrl,
      @Value("${scm.providers.gitlab.token}") final String token,
      final UnifiedDiffParser diffParser,
      final ReviewResultFormatter reviewResultFormatter,
      final SCMIdentifierValidator identifierValidator,
      final GitLabDiffBuilder diffBuilder,
      final GitLabMergeRequestMapper mergeRequestMapper,
      final GitLabProjectMapper projectMapper) {
    this.gitLabApi = new GitLabApi(apiUrl, token);
    this.diffParser = diffParser;
    this.reviewResultFormatter = reviewResultFormatter;
    this.identifierValidator = identifierValidator;
    this.diffBuilder = diffBuilder;
    this.mergeRequestMapper = mergeRequestMapper;
    this.projectMapper = projectMapper;
    log.info("GitLab adapter initialized for: {}", apiUrl);
  }

  @Override
  public Mono<DiffAnalysisBundle> getDiff(
      final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
    return Mono.fromCallable(
            () -> {
              final GitLabRepositoryId gitLabRepo =
                  identifierValidator.validateGitLabRepository(repo);
              final MergeRequestId mrId =
                  identifierValidator.validateGitLabChangeRequest(changeRequest);

              log.debug("Fetching diff for {}/MR!{}", gitLabRepo.projectId(), mrId.iid());

              final Object projectIdOrPath = gitLabRepo.projectId();
              final MergeRequest mergeRequestWithChanges =
                  gitLabApi
                      .getMergeRequestApi()
                      .getMergeRequestChanges(projectIdOrPath, (long) mrId.iid());

              final List<Diff> diffs = mergeRequestWithChanges.getChanges();
              log.debug(
                  "Fetched {} diffs for {}/MR!{}",
                  diffs.size(),
                  gitLabRepo.projectId(),
                  mrId.iid());

              final String rawDiff = diffBuilder.buildRawDiff(diffs);
              final GitDiffDocument structuredDiff = diffParser.parse(rawDiff);

              log.debug("Parsed {} file modifications", structuredDiff.files.size());

              return new DiffAnalysisBundle(structuredDiff, rawDiff);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            error ->
                log.error(
                    "Failed to fetch diff for {}/MR!{}",
                    repo.getDisplayName(),
                    changeRequest.getNumber(),
                    error));
  }

  @Override
  public Mono<Void> publishReview(
      final RepositoryIdentifier repo,
      final ChangeRequestIdentifier changeRequest,
      final ReviewResult reviewResult) {
    return Mono.<Void>fromRunnable(
            () -> {
              try {
                final GitLabRepositoryId gitLabRepo =
                    identifierValidator.validateGitLabRepository(repo);
                final MergeRequestId mrId =
                    identifierValidator.validateGitLabChangeRequest(changeRequest);

                log.debug("Publishing review for {}/MR!{}", gitLabRepo.projectId(), mrId.iid());

                final Object projectIdOrPath = gitLabRepo.projectId();
                final String reviewBody = reviewResultFormatter.format(reviewResult);

                log.debug(
                    "Publishing review: issues={}, notes={}",
                    reviewResult.issues.size(),
                    reviewResult.non_blocking_notes.size());

                gitLabApi
                    .getNotesApi()
                    .createMergeRequestNote(
                        projectIdOrPath, (long) mrId.iid(), reviewBody, null, null);

                log.info(
                    "Review published for {}/MR!{}",
                    repo.getDisplayName(),
                    changeRequest.getNumber());
              } catch (final GitLabApiException e) {
                log.error(
                    "Failed to publish review for {}/MR!{}",
                    repo.getDisplayName(),
                    changeRequest.getNumber(),
                    e);
                throw new RuntimeException("Failed to publish review", e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  @Override
  public Mono<Boolean> isChangeRequestOpen(
      final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
    return Mono.fromCallable(
            () -> {
              final GitLabRepositoryId gitLabRepo =
                  identifierValidator.validateGitLabRepository(repo);
              final MergeRequestId mrId =
                  identifierValidator.validateGitLabChangeRequest(changeRequest);

              log.debug("Checking MR status for {}/MR!{}", gitLabRepo.projectId(), mrId.iid());

              final Object projectIdOrPath = gitLabRepo.toNumericId();
              final MergeRequest mergeRequest =
                  gitLabApi
                      .getMergeRequestApi()
                      .getMergeRequest(projectIdOrPath, (long) mrId.iid());

              final boolean isOpen = "opened".equals(mergeRequest.getState());
              log.debug(
                  "MR {}/!{} is {}",
                  repo.getDisplayName(),
                  changeRequest.getNumber(),
                  isOpen ? "open" : "closed");

              return isOpen;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            error ->
                log.error(
                    "Failed to check MR status for {}/MR!{}",
                    repo.getDisplayName(),
                    changeRequest.getNumber(),
                    error));
  }

  @Override
  public Mono<RepositoryInfo> getRepository(final RepositoryIdentifier repo) {
    return Mono.fromCallable(
            () -> {
              final GitLabRepositoryId gitLabRepo =
                  identifierValidator.validateGitLabRepository(repo);

              log.debug("Fetching repository: {}", gitLabRepo.projectId());

              final Object projectIdOrPath = gitLabRepo.toNumericId();
              final Project project = gitLabApi.getProjectApi().getProject(projectIdOrPath);

              final RepositoryInfo repoInfo = projectMapper.toRepositoryInfo(project);

              log.debug("Retrieved repository: {}", gitLabRepo.projectId());
              return repoInfo;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            error -> log.error("Failed to fetch repository: {}", repo.getDisplayName(), error));
  }

  @Override
  public Flux<MergeRequestSummary> getOpenChangeRequests(final RepositoryIdentifier repo) {
    return Flux.defer(
            () -> {
              final GitLabRepositoryId gitLabRepo;
              try {
                gitLabRepo = identifierValidator.validateGitLabRepository(repo);
              } catch (final IllegalArgumentException e) {
                return Flux.error(e);
              }

              log.debug("Fetching open merge requests for: {}", gitLabRepo.projectId());

              try {
                final Object projectIdOrPath =
                    gitLabRepo.isNumericId() ? gitLabRepo.toNumericId() : gitLabRepo.projectId();
                final List<MergeRequest> mergeRequests =
                    gitLabApi
                        .getMergeRequestApi()
                        .getMergeRequests(
                            projectIdOrPath, org.gitlab4j.api.Constants.MergeRequestState.OPENED);

                log.debug(
                    "Fetched {} open merge requests for: {}",
                    mergeRequests.size(),
                    gitLabRepo.projectId());

                final List<MergeRequestSummary> summaries =
                    mergeRequests.stream().map(mergeRequestMapper::toMergeRequestSummary).toList();

                return Flux.fromIterable(summaries);
              } catch (final GitLabApiException e) {
                log.error("Failed to fetch open merge requests for: {}", gitLabRepo.projectId(), e);
                return Flux.error(new RuntimeException("Failed to fetch open merge requests", e));
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            error ->
                log.error(
                    "Failed to fetch open change requests for: {}", repo.getDisplayName(), error));
  }

  @Override
  public Flux<RepositoryInfo> getAllRepositories() {
    return Flux.defer(
            () -> {
              log.debug("Fetching all repositories");

              try {
                final List<Project> projects = gitLabApi.getProjectApi().getProjects();

                log.debug("Fetched {} repositories", projects.size());

                final List<RepositoryInfo> repositories =
                    projects.stream().map(projectMapper::toRepositoryInfo).toList();

                return Flux.fromIterable(repositories);
              } catch (final Exception e) {
                log.error("Failed to fetch repositories", e);
                return Flux.error(new RuntimeException("Failed to fetch repositories", e));
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnNext(repo -> log.debug("Streaming repository: {}", repo.name()))
        .doOnComplete(() -> log.info("Completed fetching repositories"))
        .doOnError(error -> log.error("Failed to fetch repositories", error));
  }

  @Override
  public SourceProvider getProviderType() {
    return SourceProvider.GITLAB;
  }
}
