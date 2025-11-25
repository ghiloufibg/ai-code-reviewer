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

              final PrMetadata prMetadata = extractPrMetadata(pullRequest, files.size());

              return new DiffAnalysisBundle(repo, structuredDiff, rawDiff, prMetadata);
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
  public Mono<Void> publishSummaryComment(
      final RepositoryIdentifier repo,
      final ChangeRequestIdentifier changeRequest,
      final String summaryComment) {
    log.warn("Summary comment feature not yet implemented for GitHub");
    return Mono.empty();
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

  @Override
  public Mono<CommitResult> applyFix(
      final RepositoryIdentifier repo,
      final String branchName,
      final String filePath,
      final String fixDiff,
      final String commitMessage) {
    return Mono.error(
        new UnsupportedOperationException("Fix application is not yet implemented for GitHub"));
  }

  @Override
  public Mono<Boolean> hasWriteAccess(final RepositoryIdentifier repo) {
    return Mono.error(
        new UnsupportedOperationException("Write access check is not yet implemented for GitHub"));
  }

  @Override
  public Mono<List<String>> listRepositoryFiles() {
    return Mono.just(List.of());
  }

  @Override
  public reactor.core.publisher.Flux<com.ghiloufi.aicode.core.domain.model.CommitInfo>
      getCommitsFor(
          final RepositoryIdentifier repo,
          final String filePath,
          final java.time.LocalDate since,
          final int maxResults) {
    return reactor.core.publisher.Flux.error(
        new UnsupportedOperationException(
            "Git history queries are not yet implemented for GitHub"));
  }

  @Override
  public reactor.core.publisher.Flux<com.ghiloufi.aicode.core.domain.model.CommitInfo>
      getCommitsSince(
          final RepositoryIdentifier repo, final java.time.LocalDate since, final int maxResults) {
    return reactor.core.publisher.Flux.error(
        new UnsupportedOperationException(
            "Git history queries are not yet implemented for GitHub"));
  }

  @Override
  public Mono<String> getFileContent(final RepositoryIdentifier repo, final String filePath) {
    return Mono.error(
        new UnsupportedOperationException("File content retrieval not yet implemented for GitHub"));
  }

  @Override
  public Mono<PrMetadata> getPullRequestMetadata(
      final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
    return Mono.fromCallable(
            () -> {
              final GitHubRepositoryId ghRepo = identifierValidator.validateGitHubRepository(repo);
              final PullRequestId prId =
                  identifierValidator.validateGitHubChangeRequest(changeRequest);

              final GHRepository ghRepository = gitHub.getRepository(ghRepo.getDisplayName());
              final GHPullRequest pullRequest = ghRepository.getPullRequest(prId.number());

              return extractPrMetadata(pullRequest, pullRequest.getChangedFiles());
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  private PrMetadata extractPrMetadata(final GHPullRequest pullRequest, final int changedFiles) {
    try {
      final String author =
          pullRequest.getUser() != null ? pullRequest.getUser().getLogin() : null;
      final String baseBranch =
          pullRequest.getBase() != null ? pullRequest.getBase().getRef() : null;
      final String headBranch =
          pullRequest.getHead() != null ? pullRequest.getHead().getRef() : null;

      final List<String> labels =
          pullRequest.getLabels().stream().map(label -> label.getName()).toList();

      final List<CommitInfo> commits =
          pullRequest.listCommits().toList().stream()
              .limit(10)
              .map(
                  commit ->
                      new CommitInfo(
                          commit.getSha(),
                          commit.getCommit().getMessage(),
                          commit.getCommit().getAuthor() != null
                              ? commit.getCommit().getAuthor().getName()
                              : null,
                          commit.getCommit().getAuthor() != null
                                  && commit.getCommit().getAuthor().getDate() != null
                              ? commit.getCommit().getAuthor().getDate().toInstant()
                              : null,
                          List.of()))
              .toList();

      return new PrMetadata(
          pullRequest.getTitle(),
          pullRequest.getBody(),
          author,
          baseBranch,
          headBranch,
          labels,
          commits,
          changedFiles);
    } catch (final IOException e) {
      log.warn("Failed to extract full PR metadata: {}", e.getMessage());
      return new PrMetadata(
          pullRequest.getTitle(),
          pullRequest.getBody(),
          null,
          null,
          null,
          List.of(),
          List.of(),
          changedFiles);
    }
  }
}
