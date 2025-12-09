package com.ghiloufi.aicode.core.infrastructure.adapter;

import com.ghiloufi.aicode.core.domain.model.*;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.core.domain.service.CommentPlacementRouter;
import com.ghiloufi.aicode.core.domain.service.DiffLineValidator;
import com.ghiloufi.aicode.core.domain.service.GitLabDiffBuilder;
import com.ghiloufi.aicode.core.domain.service.GitLabMergeRequestMapper;
import com.ghiloufi.aicode.core.domain.service.GitLabProjectMapper;
import com.ghiloufi.aicode.core.domain.service.SCMIdentifierValidator;
import com.ghiloufi.aicode.core.exception.SCMException;
import com.ghiloufi.aicode.core.service.diff.UnifiedDiffParser;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.DiscussionsApi;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Diff;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Position;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.RepositoryFile;
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
  private final SCMIdentifierValidator identifierValidator;
  private final GitLabDiffBuilder diffBuilder;
  private final GitLabMergeRequestMapper mergeRequestMapper;
  private final GitLabProjectMapper projectMapper;
  private final DiffLineValidator diffLineValidator;
  private final CommentPlacementRouter commentPlacementRouter;

  public GitLabAdapter(
      @Value("${scm.providers.gitlab.api-url}") final String apiUrl,
      @Value("${scm.providers.gitlab.token}") final String token,
      final UnifiedDiffParser diffParser,
      final SCMIdentifierValidator identifierValidator,
      final GitLabDiffBuilder diffBuilder,
      final GitLabMergeRequestMapper mergeRequestMapper,
      final GitLabProjectMapper projectMapper,
      final DiffLineValidator diffLineValidator,
      final CommentPlacementRouter commentPlacementRouter) {
    this.gitLabApi = new GitLabApi(apiUrl, token);
    this.diffParser = diffParser;
    this.identifierValidator = identifierValidator;
    this.diffBuilder = diffBuilder;
    this.mergeRequestMapper = mergeRequestMapper;
    this.projectMapper = projectMapper;
    this.diffLineValidator = diffLineValidator;
    this.commentPlacementRouter = commentPlacementRouter;
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
                  "Fetched {} diffs for {}/MR!{} - Title: {}",
                  diffs.size(),
                  gitLabRepo.projectId(),
                  mrId.iid(),
                  mergeRequestWithChanges.getTitle());

              final String rawDiff = diffBuilder.buildRawDiff(diffs);
              final GitDiffDocument structuredDiff = diffParser.parse(rawDiff);

              log.debug("Parsed {} file modifications", structuredDiff.files.size());

              final PrMetadata prMetadata =
                  extractPrMetadata(mergeRequestWithChanges, diffs.size());

              return new DiffAnalysisBundle(repo, structuredDiff, rawDiff, prMetadata);
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
    return Mono.fromCallable(
            () -> {
              final GitLabRepositoryId gitLabRepo =
                  identifierValidator.validateGitLabRepository(repo);
              final MergeRequestId mrId =
                  identifierValidator.validateGitLabChangeRequest(changeRequest);

              log.debug("Publishing review for {}/MR!{}", gitLabRepo.projectId(), mrId.iid());

              final Object projectIdOrPath = gitLabRepo.projectId();
              final MergeRequest mergeRequest =
                  gitLabApi
                      .getMergeRequestApi()
                      .getMergeRequestChanges(projectIdOrPath, (long) mrId.iid());

              final List<Diff> diffs = mergeRequest.getChanges();
              final String rawDiff = diffBuilder.buildRawDiff(diffs);
              final GitDiffDocument structuredDiff = diffParser.parse(rawDiff);

              final DiffLineValidator.ValidationResult validationResult =
                  diffLineValidator.validate(structuredDiff, reviewResult);

              final CommentPlacementRouter.SplitResult splitResult =
                  commentPlacementRouter.split(validationResult);

              log.debug(
                  "Validation: {} valid issues, {} invalid issues",
                  splitResult.validForInline().getIssues().size(),
                  splitResult.invalidForFallback().getIssues().size());

              return publishWithInlineComments(
                  projectIdOrPath, mrId.iid(), mergeRequest, splitResult);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .then()
        .doOnSuccess(
            unused ->
                log.info(
                    "Review published for {}/MR!{}",
                    repo.getDisplayName(),
                    changeRequest.getNumber()))
        .doOnError(
            error ->
                log.error(
                    "Failed to publish review for {}/MR!{}",
                    repo.getDisplayName(),
                    changeRequest.getNumber(),
                    error));
  }

  @Override
  public Mono<Void> publishSummaryComment(
      final RepositoryIdentifier repo,
      final ChangeRequestIdentifier changeRequest,
      final String summaryComment) {
    return Mono.fromCallable(
            () -> {
              final GitLabRepositoryId gitLabRepo =
                  identifierValidator.validateGitLabRepository(repo);
              final MergeRequestId mrId =
                  identifierValidator.validateGitLabChangeRequest(changeRequest);

              log.debug(
                  "Publishing summary comment for {}/MR!{}", gitLabRepo.projectId(), mrId.iid());

              final Object projectIdOrPath = gitLabRepo.projectId();
              gitLabApi
                  .getNotesApi()
                  .createMergeRequestNote(
                      projectIdOrPath, (long) mrId.iid(), summaryComment, null, null);

              log.debug(
                  "Summary comment published for {}/MR!{}", gitLabRepo.projectId(), mrId.iid());

              return null;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .then()
        .doOnSuccess(
            unused ->
                log.info(
                    "Summary comment published for {}/MR!{}",
                    repo.getDisplayName(),
                    changeRequest.getNumber()))
        .doOnError(
            error ->
                log.error(
                    "Failed to publish summary comment for {}/MR!{}",
                    repo.getDisplayName(),
                    changeRequest.getNumber(),
                    error));
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

  private PublishResult publishWithInlineComments(
      final Object projectIdOrPath,
      final long mergeRequestIid,
      final MergeRequest mergeRequest,
      final CommentPlacementRouter.SplitResult splitResult) {

    final DiscussionsApi discussionsApi = gitLabApi.getDiscussionsApi();
    final List<String> discussionIds = new ArrayList<>();
    final List<PublishError> errors = new ArrayList<>();

    int inlineCommentsCreated = 0;

    for (final ReviewResult.Issue issue : splitResult.validForInline().getIssues()) {
      try {
        final String commentBody = formatInlineComment(issue);
        final Position position =
            createPosition(mergeRequest, issue.getFile(), issue.getStartLine());

        final Discussion discussion =
            discussionsApi.createMergeRequestDiscussion(
                projectIdOrPath, mergeRequestIid, commentBody, null, null, position);

        discussionIds.add(discussion.getId());
        inlineCommentsCreated++;

        log.debug(
            "Created inline comment on {}:{} (discussion {})",
            issue.getFile(),
            issue.getStartLine(),
            discussion.getId());

      } catch (final GitLabApiException e) {
        log.error(
            "Failed to create inline comment for {}:{}", issue.getFile(), issue.getStartLine(), e);
        errors.add(
            new PublishError(
                issue.getFile(),
                issue.getStartLine(),
                "Failed to create inline comment: " + e.getMessage()));
      }
    }

    for (final ReviewResult.Note note : splitResult.validForInline().getNonBlockingNotes()) {
      try {
        final String commentBody = formatInlineNote(note);
        final Position position = createPosition(mergeRequest, note.getFile(), note.getLine());

        final Discussion discussion =
            discussionsApi.createMergeRequestDiscussion(
                projectIdOrPath, mergeRequestIid, commentBody, null, null, position);

        discussionIds.add(discussion.getId());
        inlineCommentsCreated++;

        log.debug(
            "Created inline note on {}:{} (discussion {})",
            note.getFile(),
            note.getLine(),
            discussion.getId());

      } catch (final GitLabApiException e) {
        log.error("Failed to create inline note for {}:{}", note.getFile(), note.getLine(), e);
        errors.add(
            new PublishError(
                note.getFile(), note.getLine(), "Failed to create inline note: " + e.getMessage()));
      }
    }

    if (!splitResult.invalidForFallback().getIssues().isEmpty()
        || !splitResult.invalidForFallback().getNonBlockingNotes().isEmpty()) {
      try {
        final String fallbackBody = formatFallbackComment(splitResult);
        gitLabApi
            .getNotesApi()
            .createMergeRequestNote(projectIdOrPath, mergeRequestIid, fallbackBody, null, null);

        log.debug(
            "Published fallback comment with {} invalid issues",
            splitResult.invalidForFallback().getIssues().size());

      } catch (final GitLabApiException e) {
        log.error("Failed to publish fallback comment", e);
        errors.add(
            new PublishError(null, 0, "Failed to publish fallback comment: " + e.getMessage()));
      }
    }

    log.info(
        "Published review: {} inline comments, {} fallback items",
        inlineCommentsCreated,
        splitResult.invalidForFallback().getIssues().size());

    return new PublishResult(
        inlineCommentsCreated,
        splitResult.invalidForFallback().getIssues().size(),
        discussionIds,
        errors);
  }

  private Position createPosition(
      final MergeRequest mergeRequest, final String filePath, final int lineNumber) {

    final Position position = new Position();
    position.setPositionType(Position.PositionType.TEXT);
    position.setBaseSha(mergeRequest.getDiffRefs().getBaseSha());
    position.setHeadSha(mergeRequest.getDiffRefs().getHeadSha());
    position.setStartSha(mergeRequest.getDiffRefs().getStartSha());
    position.setNewPath(filePath);
    position.setOldPath(filePath);
    position.setNewLine(lineNumber);

    return position;
  }

  private String formatInlineComment(final ReviewResult.Issue issue) {
    final String label = "issue";

    final String blockingStatus =
        switch (issue.getSeverity()) {
          case "critical" -> "(blocking)";
          case "major" -> "(blocking)";
          case "minor" -> "(non-blocking)";
          case "info" -> "(non-blocking)";
          default -> "(non-blocking)";
        };

    final String severityDecoration =
        switch (issue.getSeverity()) {
          case "critical" -> "critical";
          case "major" -> "major";
          case "minor" -> "minor";
          case "info" -> "info";
          default -> issue.getSeverity();
        };

    final StringBuilder comment = new StringBuilder();
    comment.append(
        String.format(
            "%s %s, %s: %s\n\n", label, blockingStatus, severityDecoration, issue.getTitle()));

    if (issue.getSuggestion() != null && !issue.getSuggestion().isBlank()) {
      comment.append(String.format("**Recommendation:** %s\n\n", issue.getSuggestion()));
    }

    if (issue.isHighConfidence() && issue.getConfidenceScore() != null) {
      comment.append(String.format("**Confidence: %.0f%%**\n\n", issue.getConfidenceScore() * 100));
    }

    return comment.toString();
  }

  private String formatInlineNote(final ReviewResult.Note note) {
    return "note (non-blocking): Code observation\n\n" + note.getNote();
  }

  private String formatFallbackComment(final CommentPlacementRouter.SplitResult splitResult) {
    final StringBuilder body = new StringBuilder();
    body.append("## Additional Review Findings\n\n");
    body.append("The following issues were found in code areas outside the current diff:\n\n");

    for (final ReviewResult.Issue issue : splitResult.invalidForFallback().getIssues()) {
      final String blockingStatus =
          switch (issue.getSeverity()) {
            case "critical" -> "(blocking)";
            case "major" -> "(blocking)";
            case "minor" -> "(non-blocking)";
            case "info" -> "(non-blocking)";
            default -> "(non-blocking)";
          };

      final String severityLabel =
          switch (issue.getSeverity()) {
            case "critical" -> "CRITICAL";
            case "major" -> "MAJOR";
            case "minor" -> "MINOR";
            case "info" -> "INFO";
            default -> issue.getSeverity().toUpperCase();
          };

      body.append("---\n\n");
      body.append(String.format("**Issue:** %s\n", issue.getTitle()));
      body.append(String.format("**Severity:** %s %s\n", severityLabel, blockingStatus));
      body.append(
          String.format("**Location:** `%s:%d`\n\n", issue.getFile(), issue.getStartLine()));

      if (issue.getSuggestion() != null && !issue.getSuggestion().isBlank()) {
        body.append(String.format("**Recommendation:** %s\n\n", issue.getSuggestion()));
      }
    }

    if (!splitResult.invalidForFallback().getNonBlockingNotes().isEmpty()) {
      body.append("---\n\n");
      body.append("## Additional Notes\n\n");
      for (final ReviewResult.Note note : splitResult.invalidForFallback().getNonBlockingNotes()) {
        body.append("---\n\n");
        body.append("**Note:** Code observation\n");
        body.append(String.format("**Location:** `%s:%d`\n\n", note.getFile(), note.getLine()));
        body.append(String.format("%s\n\n", note.getNote()));
      }
    }

    return body.toString();
  }

  @Override
  public Mono<List<String>> listRepositoryFiles() {
    return Mono.just(List.of());
  }

  @Override
  public Flux<CommitInfo> getCommitsFor(
      final RepositoryIdentifier repo,
      final String filePath,
      final LocalDate since,
      final int maxResults) {
    return Flux.defer(
            () -> {
              try {
                final GitLabRepositoryId gitLabRepo =
                    identifierValidator.validateGitLabRepository(repo);

                log.debug(
                    "Fetching commits for file {} in project {} since {}",
                    filePath,
                    gitLabRepo.projectId(),
                    since);

                final Object projectIdOrPath = gitLabRepo.projectId();
                final Date sinceDate =
                    Date.from(since.atStartOfDay(ZoneId.systemDefault()).toInstant());

                final List<org.gitlab4j.api.models.Commit> commits =
                    gitLabApi
                        .getCommitsApi()
                        .getCommits(projectIdOrPath, null, sinceDate, null, filePath);

                log.debug("Found {} commits for file {}", commits.size(), filePath);

                return Flux.fromIterable(commits)
                    .take(maxResults)
                    .map(this::mapGitLabCommitToCommitInfo);

              } catch (final GitLabApiException e) {
                final GitLabRepositoryId gitLabRepo =
                    identifierValidator.validateGitLabRepository(repo);
                log.error(
                    "Failed to fetch commits for file {} in project {}",
                    filePath,
                    gitLabRepo.projectId(),
                    e);
                return Flux.error(
                    new SCMException(
                        "Failed to fetch commit history for file: " + filePath,
                        SourceProvider.GITLAB,
                        "getCommitsFor",
                        e));
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  @Override
  public Flux<CommitInfo> getCommitsSince(
      final RepositoryIdentifier repo, final java.time.LocalDate since, final int maxResults) {
    return Flux.defer(
            () -> {
              try {
                final GitLabRepositoryId gitLabRepo =
                    identifierValidator.validateGitLabRepository(repo);

                log.debug(
                    "Fetching commits for project {} since {}", gitLabRepo.projectId(), since);

                final Object projectIdOrPath = gitLabRepo.projectId();
                final Date sinceDate =
                    Date.from(since.atStartOfDay(ZoneId.systemDefault()).toInstant());

                final List<org.gitlab4j.api.models.Commit> commits =
                    gitLabApi.getCommitsApi().getCommits(projectIdOrPath, null, sinceDate, null);

                log.debug("Found {} commits since {}", commits.size(), since);

                return Flux.fromIterable(commits)
                    .take(maxResults)
                    .flatMap(commit -> fetchCommitWithChangedFiles(projectIdOrPath, commit));

              } catch (final GitLabApiException e) {
                final GitLabRepositoryId gitLabRepo =
                    identifierValidator.validateGitLabRepository(repo);
                log.error("Failed to fetch commits for project {}", gitLabRepo.projectId(), e);
                return Flux.error(
                    new SCMException(
                        "Failed to fetch commit history",
                        SourceProvider.GITLAB,
                        "getCommitsSince",
                        e));
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  private Mono<CommitInfo> fetchCommitWithChangedFiles(
      final Object projectIdOrPath, final org.gitlab4j.api.models.Commit commit) {
    return Mono.fromCallable(
            () -> {
              try {
                final List<org.gitlab4j.api.models.Diff> diffs =
                    gitLabApi.getCommitsApi().getDiff(projectIdOrPath, commit.getId());

                final List<String> changedFiles =
                    diffs != null
                        ? diffs.stream()
                            .map(org.gitlab4j.api.models.Diff::getNewPath)
                            .filter(path -> path != null && !path.isEmpty())
                            .toList()
                        : List.of();

                return new CommitInfo(
                    commit.getId(),
                    commit.getMessage() != null ? commit.getMessage() : "",
                    commit.getAuthorName() != null ? commit.getAuthorName() : "Unknown",
                    commit.getCommittedDate() != null
                        ? commit.getCommittedDate().toInstant()
                        : java.time.Instant.now(),
                    changedFiles);

              } catch (final GitLabApiException e) {
                log.warn("Failed to fetch diff for commit {}, using partial data", commit.getId());
                return mapGitLabCommitToCommitInfo(commit);
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  private CommitInfo mapGitLabCommitToCommitInfo(final org.gitlab4j.api.models.Commit commit) {
    return new CommitInfo(
        commit.getId(),
        commit.getMessage() != null ? commit.getMessage() : "",
        commit.getAuthorName() != null ? commit.getAuthorName() : "Unknown",
        commit.getCommittedDate() != null
            ? commit.getCommittedDate().toInstant()
            : java.time.Instant.now(),
        List.of());
  }

  @Override
  public Mono<String> getFileContent(final RepositoryIdentifier repo, final String filePath) {
    return Mono.fromCallable(
            () -> {
              final GitLabRepositoryId gitLabRepo =
                  identifierValidator.validateGitLabRepository(repo);

              log.debug("Fetching file content: {} from {}", filePath, gitLabRepo.projectId());

              final Object projectIdOrPath = gitLabRepo.projectId();
              final Project project = gitLabApi.getProjectApi().getProject(projectIdOrPath);
              final String ref = project.getDefaultBranch();

              final RepositoryFile file =
                  gitLabApi.getRepositoryFileApi().getFile(projectIdOrPath, filePath, ref);
              final String content = file.getDecodedContentAsString();

              log.debug("Retrieved {} bytes from {}", content.length(), filePath);
              return content;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            error ->
                log.debug(
                    "Failed to fetch file {} from {}: {}",
                    filePath,
                    repo.getDisplayName(),
                    error.getMessage()));
  }

  @Override
  public Mono<PrMetadata> getPullRequestMetadata(
      final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {
    return Mono.fromCallable(
            () -> {
              final GitLabRepositoryId gitLabRepo =
                  identifierValidator.validateGitLabRepository(repo);
              final MergeRequestId mrId =
                  identifierValidator.validateGitLabChangeRequest(changeRequest);

              final Object projectIdOrPath = gitLabRepo.projectId();
              final MergeRequest mergeRequest =
                  gitLabApi
                      .getMergeRequestApi()
                      .getMergeRequestChanges(projectIdOrPath, (long) mrId.iid());

              final int changedFiles =
                  mergeRequest.getChanges() != null ? mergeRequest.getChanges().size() : 0;

              return extractPrMetadata(mergeRequest, changedFiles);
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  private PrMetadata extractPrMetadata(final MergeRequest mergeRequest, final int changedFiles) {
    final String author =
        mergeRequest.getAuthor() != null ? mergeRequest.getAuthor().getUsername() : null;

    final List<String> labels =
        mergeRequest.getLabels() != null ? List.copyOf(mergeRequest.getLabels()) : List.of();

    List<CommitInfo> commits = List.of();
    try {
      final List<org.gitlab4j.api.models.Commit> mrCommits =
          gitLabApi
              .getMergeRequestApi()
              .getCommits(mergeRequest.getProjectId(), (long) mergeRequest.getIid());
      commits = mrCommits.stream().limit(10).map(this::mapGitLabCommitToCommitInfo).toList();
    } catch (final GitLabApiException e) {
      log.warn("Failed to fetch commits for MR!{}: {}", mergeRequest.getIid(), e.getMessage());
    }

    return new PrMetadata(
        mergeRequest.getTitle(),
        mergeRequest.getDescription(),
        author,
        mergeRequest.getTargetBranch(),
        mergeRequest.getSourceBranch(),
        labels,
        commits,
        changedFiles);
  }
}
