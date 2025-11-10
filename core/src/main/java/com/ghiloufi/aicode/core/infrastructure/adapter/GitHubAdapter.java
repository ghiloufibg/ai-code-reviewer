package com.ghiloufi.aicode.core.infrastructure.adapter;

import com.ghiloufi.aicode.core.domain.model.*;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.core.domain.service.GitHubDiffBuilder;
import com.ghiloufi.aicode.core.domain.service.ReviewResultFormatter;
import com.ghiloufi.aicode.core.domain.service.SCMIdentifierValidator;
import com.ghiloufi.aicode.core.service.diff.UnifiedDiffParser;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "scm.providers.github",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class GitHubAdapter implements SCMPort {

  private final GitHub gitHub;
  private final UnifiedDiffParser diffParser;
  private final ReviewResultFormatter reviewResultFormatter;
  private final SCMIdentifierValidator identifierValidator;
  private final GitHubDiffBuilder diffBuilder;

  public GitHubAdapter(
      @Value("${github.token}") final String token,
      final UnifiedDiffParser diffParser,
      final ReviewResultFormatter reviewResultFormatter,
      final SCMIdentifierValidator identifierValidator,
      final GitHubDiffBuilder diffBuilder)
      throws IOException {
    this.gitHub = new GitHubBuilder().withOAuthToken(token).build();
    this.diffParser = diffParser;
    this.reviewResultFormatter = reviewResultFormatter;
    this.identifierValidator = identifierValidator;
    this.diffBuilder = diffBuilder;
    log.info("GitHub adapter initialized");
  }

  @Override
  public Mono<DiffAnalysisBundle> getDiff(
      final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
    return Mono.fromCallable(
            () -> {
              final GitHubRepositoryId ghRepo = identifierValidator.validateGitHubRepository(repo);
              final PullRequestId prId =
                  identifierValidator.validateGitHubChangeRequest(changeRequest);

              log.debug("Fetching diff for {}/PR#{}", ghRepo.getDisplayName(), prId.number());

              final GHRepository ghRepository = gitHub.getRepository(ghRepo.getDisplayName());
              final GHPullRequest pullRequest = ghRepository.getPullRequest(prId.number());

              final List<GHPullRequestFileDetail> files = pullRequest.listFiles().toList();
              final String rawDiff = diffBuilder.buildRawDiff(files);

              final GitDiffDocument structuredDiff = diffParser.parse(rawDiff);

              log.debug("Parsed {} file modifications", structuredDiff.files.size());

              return new DiffAnalysisBundle(structuredDiff, rawDiff);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            error ->
                log.error(
                    "Failed to fetch diff for {}/PR#{}",
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
                final GitHubRepositoryId ghRepo =
                    identifierValidator.validateGitHubRepository(repo);
                final PullRequestId prId =
                    identifierValidator.validateGitHubChangeRequest(changeRequest);

                log.debug("Publishing review for {}/PR#{}", ghRepo.getDisplayName(), prId.number());

                final GHRepository ghRepository = gitHub.getRepository(ghRepo.getDisplayName());
                final GHPullRequest pullRequest = ghRepository.getPullRequest(prId.number());

                final String reviewBody = reviewResultFormatter.format(reviewResult);

                pullRequest.comment(reviewBody);

                log.info("Review published for {}/PR#{}", ghRepo.getDisplayName(), prId.number());
              } catch (final IOException e) {
                log.error(
                    "Failed to publish review for {}/PR#{}",
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
              final GitHubRepositoryId ghRepo = identifierValidator.validateGitHubRepository(repo);
              final PullRequestId prId =
                  identifierValidator.validateGitHubChangeRequest(changeRequest);

              log.debug("Checking PR status for {}/PR#{}", ghRepo.getDisplayName(), prId.number());

              final GHRepository ghRepository = gitHub.getRepository(ghRepo.getDisplayName());
              final GHPullRequest pullRequest = ghRepository.getPullRequest(prId.number());

              final boolean isOpen = pullRequest.getState() == GHIssueState.OPEN;
              log.debug(
                  "PR {}/#{} is {}",
                  ghRepo.getDisplayName(),
                  prId.number(),
                  isOpen ? "open" : "closed");

              return isOpen;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            error ->
                log.error(
                    "Failed to check PR status for {}/PR#{}",
                    repo.getDisplayName(),
                    changeRequest.getNumber(),
                    error));
  }

  @Override
  public Mono<RepositoryInfo> getRepository(final RepositoryIdentifier repo) {
    return Mono.fromCallable(
            () -> {
              final GitHubRepositoryId ghRepo = identifierValidator.validateGitHubRepository(repo);

              log.debug("Fetching repository: {}", ghRepo.getDisplayName());

              final GHRepository ghRepository = gitHub.getRepository(ghRepo.getDisplayName());

              final RepositoryInfo repoInfo =
                  new RepositoryInfo(
                      ghRepository.getFullName(),
                      ghRepository.getName(),
                      ghRepository.getOwner().getLogin(),
                      Optional.ofNullable(ghRepository.getDescription()).orElse(""),
                      Optional.ofNullable(ghRepository.getLanguage()).orElse(""),
                      ghRepository.hasIssues(),
                      true,
                      ghRepository.getDefaultBranch(),
                      ghRepository.getId(),
                      ghRepository.isPrivate(),
                      ghRepository.getHtmlUrl().toString());

              log.debug("Retrieved repository: {}", ghRepo.getDisplayName());
              return repoInfo;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            error -> log.error("Failed to fetch repository: {}", repo.getDisplayName(), error));
  }

  @Override
  public Flux<MergeRequestSummary> getOpenChangeRequests(final RepositoryIdentifier repo) {
    return Flux.error(
        new UnsupportedOperationException(
            "Listing open pull requests is not yet implemented for GitHub"));
  }

  @Override
  public Flux<RepositoryInfo> getAllRepositories() {
    return Flux.error(
        new UnsupportedOperationException(
            "Listing all repositories is not yet implemented for GitHub"));
  }

  @Override
  public SourceProvider getProviderType() {
    return SourceProvider.GITHUB;
  }
}
