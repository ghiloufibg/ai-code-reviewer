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
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.DiscussionsApi;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Diff;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Permissions;
import org.gitlab4j.api.models.Position;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectAccess;
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
                  splitResult.validForInline().issues.size(),
                  splitResult.invalidForFallback().issues.size());

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

  @Override
  public Mono<CommitResult> applyFix(
      final RepositoryIdentifier repo,
      final String branchName,
      final String filePath,
      final String fixDiff,
      final String commitMessage) {
    return Mono.fromCallable(
            () -> {
              final GitLabRepositoryId gitLabRepo =
                  identifierValidator.validateGitLabRepository(repo);

              if (filePath == null || filePath.isBlank()) {
                throw new SCMException(
                    "File path is required for fix application",
                    SourceProvider.GITLAB,
                    "applyFix",
                    null);
              }

              if (fixDiff == null || fixDiff.isBlank()) {
                throw new SCMException(
                    "Fix diff is required for fix application",
                    SourceProvider.GITLAB,
                    "applyFix",
                    null);
              }

              if (branchName == null || branchName.isBlank()) {
                throw new SCMException(
                    "Branch name is required for fix application",
                    SourceProvider.GITLAB,
                    "applyFix",
                    null);
              }

              if (commitMessage == null || commitMessage.isBlank()) {
                throw new SCMException(
                    "Commit message is required for fix application",
                    SourceProvider.GITLAB,
                    "applyFix",
                    null);
              }

              log.debug(
                  "Applying fix to {}/{} on branch {}",
                  gitLabRepo.projectId(),
                  filePath,
                  branchName);

              final Object projectIdOrPath = gitLabRepo.toNumericId();

              try {
                final org.gitlab4j.api.models.RepositoryFile currentFile =
                    gitLabApi.getRepositoryFileApi().getFile(projectIdOrPath, filePath, branchName);

                final String currentContent =
                    new String(
                        java.util.Base64.getDecoder().decode(currentFile.getContent()),
                        java.nio.charset.StandardCharsets.UTF_8);

                final String updatedContent = applyDiffPatch(currentContent, fixDiff);

                final org.gitlab4j.api.models.RepositoryFile fileToUpdate =
                    new org.gitlab4j.api.models.RepositoryFile();
                fileToUpdate.setFilePath(filePath);
                fileToUpdate.setContent(updatedContent);

                gitLabApi
                    .getRepositoryFileApi()
                    .updateFile(projectIdOrPath, fileToUpdate, branchName, commitMessage);

                final org.gitlab4j.api.models.Branch branch =
                    gitLabApi.getRepositoryApi().getBranch(projectIdOrPath, branchName);
                final String commitSha = branch.getCommit().getId();

                final Project project = gitLabApi.getProjectApi().getProject(projectIdOrPath);
                final String commitUrl =
                    String.format("%s/-/commit/%s", project.getWebUrl(), commitSha);

                final CommitResult commitResult =
                    new CommitResult(
                        commitSha,
                        commitUrl,
                        branchName,
                        List.of(filePath),
                        java.time.Instant.now());

                log.info(
                    "Successfully applied fix to {}/{} (commit {})",
                    gitLabRepo.projectId(),
                    filePath,
                    commitSha);

                return commitResult;

              } catch (final GitLabApiException e) {
                log.error(
                    "Failed to apply fix to {}/{} on branch {}",
                    gitLabRepo.projectId(),
                    filePath,
                    branchName,
                    e);

                final String errorMessage;
                if (e.getHttpStatus() == 403) {
                  errorMessage =
                      String.format(
                          "Access denied: Personal Access Token lacks 'write_repository' scope or "
                              + "insufficient permissions to modify file '%s' on branch '%s'. "
                              + "Please ensure your token has the required scopes: api, write_repository",
                          filePath, branchName);
                  log.error(
                      "PAT scope error for {}/{}: {}",
                      gitLabRepo.projectId(),
                      filePath,
                      errorMessage);
                } else if (e.getHttpStatus() == 404) {
                  errorMessage =
                      String.format(
                          "Not found: File '%s' does not exist on branch '%s' in project %s",
                          filePath, branchName, gitLabRepo.projectId());
                  log.error(
                      "File not found for {}/{} on branch {}: {}",
                      gitLabRepo.projectId(),
                      filePath,
                      branchName,
                      errorMessage);
                } else if (e.getHttpStatus() == 400) {
                  errorMessage =
                      String.format(
                          "Invalid request: The diff patch for file '%s' is malformed or cannot be applied. "
                              + "Original error: %s",
                          filePath, e.getMessage());
                  log.error(
                      "Invalid diff for {}/{}: {}", gitLabRepo.projectId(), filePath, errorMessage);
                } else {
                  errorMessage =
                      String.format(
                          "Failed to apply fix to file '%s' on branch '%s': %s (HTTP %d)",
                          filePath, branchName, e.getMessage(), e.getHttpStatus());
                  log.error(
                      "GitLab API error for {}/{}: {}",
                      gitLabRepo.projectId(),
                      filePath,
                      errorMessage);
                }

                throw new SCMException(errorMessage, SourceProvider.GITLAB, "applyFix", e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            error ->
                log.error(
                    "Failed to apply fix to {}/{} on branch {}",
                    repo.getDisplayName(),
                    filePath,
                    branchName,
                    error));
  }

  @Override
  public Mono<Boolean> hasWriteAccess(final RepositoryIdentifier repo) {
    return Mono.fromCallable(
            () -> {
              final GitLabRepositoryId gitLabRepo =
                  identifierValidator.validateGitLabRepository(repo);

              log.debug("Checking write access for: {}", gitLabRepo.projectId());

              final Object projectIdOrPath = gitLabRepo.toNumericId();
              final Project project = gitLabApi.getProjectApi().getProject(projectIdOrPath);

              final Permissions permissions = project.getPermissions();
              if (permissions == null) {
                log.debug(
                    "No permissions object found for {}, checking with PAT capabilities",
                    gitLabRepo.projectId());
                return checkWriteAccessViaPAT(project);
              }

              final ProjectAccess projectAccess = permissions.getProjectAccess();
              if (projectAccess == null || projectAccess.getAccessLevel() == null) {
                log.debug(
                    "No project access level found for {}, checking group access",
                    gitLabRepo.projectId());
                final boolean hasGroupAccess = checkGroupAccess(permissions);
                if (!hasGroupAccess) {
                  log.debug(
                      "No group access found, checking with PAT capabilities for {}",
                      gitLabRepo.projectId());
                  return checkWriteAccessViaPAT(project);
                }
                return true;
              }

              final org.gitlab4j.api.models.AccessLevel accessLevel =
                  projectAccess.getAccessLevel();
              final boolean hasWrite = accessLevel.toValue() >= 30;

              log.debug(
                  "Write access for {}: {} (access level: {})",
                  gitLabRepo.projectId(),
                  hasWrite,
                  accessLevel);

              return hasWrite;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            error ->
                log.error("Failed to check write access for: {}", repo.getDisplayName(), error));
  }

  private String applyDiffPatch(final String originalContent, final String unifiedDiff) {
    final String[] originalLines = originalContent.split("\n");
    final List<String> resultLines = new ArrayList<>(List.of(originalLines));

    final String[] diffLines = unifiedDiff.split("\n");
    int currentLineIndex = 0;

    for (final String diffLine : diffLines) {
      if (diffLine.startsWith("@@")) {
        final String rangeInfo =
            diffLine.substring(diffLine.indexOf('-') + 1, diffLine.indexOf('+') - 1).trim();
        final int startLine = Integer.parseInt(rangeInfo.split(",")[0]) - 1;
        currentLineIndex = startLine;
      } else if (diffLine.startsWith("-")) {
        if (currentLineIndex < resultLines.size()) {
          resultLines.remove(currentLineIndex);
        }
      } else if (diffLine.startsWith("+")) {
        final String lineToAdd = diffLine.substring(1);
        resultLines.add(currentLineIndex, lineToAdd);
        currentLineIndex++;
      } else if (diffLine.startsWith(" ")) {
        currentLineIndex++;
      }
    }

    return String.join("\n", resultLines);
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

    for (final ReviewResult.Issue issue : splitResult.validForInline().issues) {
      try {
        final String commentBody = formatInlineComment(issue);
        final Position position = createPosition(mergeRequest, issue.file, issue.start_line);

        final Discussion discussion =
            discussionsApi.createMergeRequestDiscussion(
                projectIdOrPath, mergeRequestIid, commentBody, null, null, position);

        discussionIds.add(discussion.getId());
        inlineCommentsCreated++;

        log.debug(
            "Created inline comment on {}:{} (discussion {})",
            issue.file,
            issue.start_line,
            discussion.getId());

      } catch (final GitLabApiException e) {
        log.error("Failed to create inline comment for {}:{}", issue.file, issue.start_line, e);
        errors.add(
            new PublishError(
                issue.file,
                issue.start_line,
                "Failed to create inline comment: " + e.getMessage()));
      }
    }

    for (final ReviewResult.Note note : splitResult.validForInline().non_blocking_notes) {
      try {
        final String commentBody = formatInlineNote(note);
        final Position position = createPosition(mergeRequest, note.file, note.line);

        final Discussion discussion =
            discussionsApi.createMergeRequestDiscussion(
                projectIdOrPath, mergeRequestIid, commentBody, null, null, position);

        discussionIds.add(discussion.getId());
        inlineCommentsCreated++;

        log.debug(
            "Created inline note on {}:{} (discussion {})",
            note.file,
            note.line,
            discussion.getId());

      } catch (final GitLabApiException e) {
        log.error("Failed to create inline note for {}:{}", note.file, note.line, e);
        errors.add(
            new PublishError(
                note.file, note.line, "Failed to create inline note: " + e.getMessage()));
      }
    }

    if (!splitResult.invalidForFallback().issues.isEmpty()
        || !splitResult.invalidForFallback().non_blocking_notes.isEmpty()) {
      try {
        final String fallbackBody = formatFallbackComment(splitResult);
        gitLabApi
            .getNotesApi()
            .createMergeRequestNote(projectIdOrPath, mergeRequestIid, fallbackBody, null, null);

        log.debug(
            "Published fallback comment with {} invalid issues",
            splitResult.invalidForFallback().issues.size());

      } catch (final GitLabApiException e) {
        log.error("Failed to publish fallback comment", e);
        errors.add(
            new PublishError(null, 0, "Failed to publish fallback comment: " + e.getMessage()));
      }
    }

    log.info(
        "Published review: {} inline comments, {} fallback items",
        inlineCommentsCreated,
        splitResult.invalidForFallback().issues.size());

    return new PublishResult(
        inlineCommentsCreated,
        splitResult.invalidForFallback().issues.size(),
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

  private String convertMarkdownDiffToGitLabSuggestion(final String markdownDiff) {
    if (markdownDiff == null || markdownDiff.isBlank()) {
      return "";
    }

    final StringBuilder suggestion = new StringBuilder();
    final String[] lines = markdownDiff.split("\\n");
    int linesAbove = 0;
    int linesBelow = 0;
    final StringBuilder suggestionContent = new StringBuilder();

    for (final String line : lines) {
      if (line.startsWith("```diff") || line.startsWith("```")) {
        continue;
      }

      if (line.startsWith("+")) {
        suggestionContent.append(line.substring(1)).append("\n");
      } else if (line.startsWith("-")) {
        linesAbove++;
      } else if (line.trim().startsWith("@@")) {
        continue;
      } else if (!line.trim().isEmpty()) {
        suggestionContent.append(line).append("\n");
      }
    }

    suggestion
        .append("```suggestion:-")
        .append(linesAbove)
        .append("+")
        .append(linesBelow)
        .append("\n");
    suggestion.append(suggestionContent);
    suggestion.append("```\n");

    return suggestion.toString();
  }

  private String formatInlineComment(final ReviewResult.Issue issue) {
    final String label = "issue";

    final String blockingStatus =
        switch (issue.severity) {
          case "critical" -> "(blocking)";
          case "major" -> "(blocking)";
          case "minor" -> "(non-blocking)";
          case "info" -> "(non-blocking)";
          default -> "(non-blocking)";
        };

    final String severityDecoration =
        switch (issue.severity) {
          case "critical" -> "critical";
          case "major" -> "major";
          case "minor" -> "minor";
          case "info" -> "info";
          default -> issue.severity;
        };

    final StringBuilder comment = new StringBuilder();
    comment.append(
        String.format("%s %s, %s: %s\n\n", label, blockingStatus, severityDecoration, issue.title));

    if (issue.suggestion != null && !issue.suggestion.isBlank()) {
      comment.append(String.format("**Recommendation:** %s\n\n", issue.suggestion));
    }

    if (issue.isHighConfidence() && issue.hasFixSuggestion()) {
      if (issue.confidenceScore != null) {
        comment.append(String.format("**Confidence: %.0f%%**\n\n", issue.confidenceScore * 100));
      }
      final String gitlabSuggestion = convertMarkdownDiffToGitLabSuggestion(issue.suggestedFix);
      comment.append(gitlabSuggestion);
      if (!gitlabSuggestion.endsWith("\n")) {
        comment.append("\n");
      }
    }

    return comment.toString();
  }

  private String formatInlineNote(final ReviewResult.Note note) {
    return "note (non-blocking): Code observation\n\n" + note.note;
  }

  private String formatFallbackComment(final CommentPlacementRouter.SplitResult splitResult) {
    final StringBuilder body = new StringBuilder();
    body.append("## Additional Review Findings\n\n");
    body.append("The following issues were found in code areas outside the current diff:\n\n");

    for (final ReviewResult.Issue issue : splitResult.invalidForFallback().issues) {
      final String blockingStatus =
          switch (issue.severity) {
            case "critical" -> "(blocking)";
            case "major" -> "(blocking)";
            case "minor" -> "(non-blocking)";
            case "info" -> "(non-blocking)";
            default -> "(non-blocking)";
          };

      final String severityLabel =
          switch (issue.severity) {
            case "critical" -> "CRITICAL";
            case "major" -> "MAJOR";
            case "minor" -> "MINOR";
            case "info" -> "INFO";
            default -> issue.severity.toUpperCase();
          };

      body.append("---\n\n");
      body.append(String.format("**Issue:** %s\n", issue.title));
      body.append(String.format("**Severity:** %s %s\n", severityLabel, blockingStatus));
      body.append(String.format("**Location:** `%s:%d`\n\n", issue.file, issue.start_line));

      if (issue.suggestion != null && !issue.suggestion.isBlank()) {
        body.append(String.format("**Recommendation:** %s\n\n", issue.suggestion));
      }
    }

    if (!splitResult.invalidForFallback().non_blocking_notes.isEmpty()) {
      body.append("---\n\n");
      body.append("## Additional Notes\n\n");
      for (final ReviewResult.Note note : splitResult.invalidForFallback().non_blocking_notes) {
        body.append("---\n\n");
        body.append("**Note:** Code observation\n");
        body.append(String.format("**Location:** `%s:%d`\n\n", note.file, note.line));
        body.append(String.format("%s\n\n", note.note));
      }
    }

    return body.toString();
  }

  private boolean checkGroupAccess(final Permissions permissions) {
    try {
      final Object groupAccessObj = permissions.getGroupAccess();
      if (groupAccessObj == null) {
        log.debug("No group access found");
        return false;
      }

      if (groupAccessObj instanceof ProjectAccess) {
        final ProjectAccess groupAccess = (ProjectAccess) groupAccessObj;
        final org.gitlab4j.api.models.AccessLevel accessLevel = groupAccess.getAccessLevel();
        if (accessLevel != null) {
          final boolean hasWrite = accessLevel.toValue() >= 30;
          log.debug("Group access level: {}, has write: {}", accessLevel, hasWrite);
          return hasWrite;
        }
      }

      log.debug("Unable to determine group access level");
      return false;
    } catch (final Exception e) {
      log.debug("Error checking group access", e);
      return false;
    }
  }

  private boolean checkWriteAccessViaPAT(final Project project) {
    try {
      final Boolean canPushCode = project.getSharedRunnersEnabled();
      final Boolean canCreateMergeRequest = project.getMergeRequestsEnabled();

      if (canPushCode != null || canCreateMergeRequest != null) {
        log.debug(
            "PAT has access to project {} (push: {}, MR: {})",
            project.getId(),
            canPushCode,
            canCreateMergeRequest);
        return true;
      }

      log.debug("Unable to verify PAT write access for project {}", project.getId());
      return true;
    } catch (final Exception e) {
      log.debug(
          "Error checking PAT capabilities for project {}, assuming write access",
          project.getId(),
          e);
      return true;
    }
  }
}
