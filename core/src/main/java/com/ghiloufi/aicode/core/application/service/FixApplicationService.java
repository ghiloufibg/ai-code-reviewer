package com.ghiloufi.aicode.core.application.service;

import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.CommitResult;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import com.ghiloufi.aicode.core.infrastructure.persistence.entity.ReviewIssueEntity;
import com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewIssueRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "features.fix-application.enabled", havingValue = "true")
public final class FixApplicationService {

  private final SCMPort scmPort;
  private final ReviewIssueRepository reviewIssueRepository;

  public Mono<CommitResult> applyFixByIssueId(
      final RepositoryIdentifier repo,
      final ChangeRequestIdentifier changeRequest,
      final UUID issueId) {

    return Mono.fromCallable(() -> reviewIssueRepository.findApplicableFixById(issueId))
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(
            optionalIssue ->
                optionalIssue
                    .map(Mono::just)
                    .orElseGet(
                        () ->
                            Mono.error(
                                new IllegalArgumentException(
                                    String.format(
                                        "Issue %s not found or cannot be applied (either already applied, "
                                            + "no fix available, or confidence too low)",
                                        issueId)))))
        .flatMap(
            issue -> {
              if (!issue.canApplyFix()) {
                return Mono.error(
                    new IllegalStateException(
                        String.format(
                            "Issue %s cannot be applied: fixApplied=%s, hasFixSuggestion=%s, isHighConfidence=%s",
                            issueId,
                            issue.isFixApplied(),
                            issue.hasFixSuggestion(),
                            issue.isHighConfidence())));
              }

              log.info(
                  "Applying AI-generated fix for issue {} to {}/{} (confidence: {})",
                  issueId,
                  repo.getDisplayName(),
                  issue.getFilePath(),
                  issue.getConfidenceScore());

              return validateWriteAccess(repo)
                  .flatMap(
                      hasAccess -> {
                        if (!hasAccess) {
                          return Mono.error(
                              new IllegalStateException(
                                  "No write access to repository: " + repo.getDisplayName()));
                        }

                        final String branchName = deriveBranchName(changeRequest);
                        final String commitMessage =
                            buildCommitMessage(issue.getFilePath(), issue.getTitle());

                        return scmPort.applyFix(
                            repo,
                            branchName,
                            issue.getFilePath(),
                            issue.getFixDiff(),
                            commitMessage);
                      })
                  .flatMap(
                      commitResult ->
                          updateIssueAsApplied(issue, commitResult).thenReturn(commitResult));
            })
        .doOnSuccess(
            commitResult ->
                log.info(
                    "Successfully applied fix for issue {}: commit {} at {}",
                    issueId,
                    commitResult.commitSha(),
                    commitResult.commitUrl()))
        .doOnError(
            error ->
                log.error(
                    "Failed to apply fix for issue {} to {}/MR {}",
                    issueId,
                    repo.getDisplayName(),
                    changeRequest.getNumber(),
                    error));
  }

  @Deprecated(forRemoval = true)
  public Mono<CommitResult> applyFix(
      final RepositoryIdentifier repo,
      final ChangeRequestIdentifier changeRequest,
      final String filePath,
      final String fixDiff,
      final String issueTitle) {

    return validateWriteAccess(repo)
        .flatMap(
            hasAccess -> {
              if (!hasAccess) {
                return Mono.error(
                    new IllegalStateException(
                        "No write access to repository: " + repo.getDisplayName()));
              }

              final String branchName = deriveBranchName(changeRequest);
              final String commitMessage = buildCommitMessage(filePath, issueTitle);

              log.info(
                  "Applying fix to {}/{} on branch {} for MR {}",
                  repo.getDisplayName(),
                  filePath,
                  branchName,
                  changeRequest.getNumber());

              return scmPort.applyFix(repo, branchName, filePath, fixDiff, commitMessage);
            })
        .doOnSuccess(
            commitResult ->
                log.info(
                    "Successfully applied fix: commit {} at {}",
                    commitResult.commitSha(),
                    commitResult.commitUrl()))
        .doOnError(
            error ->
                log.error(
                    "Failed to apply fix to {}/{} for MR {}",
                    repo.getDisplayName(),
                    filePath,
                    changeRequest.getNumber(),
                    error));
  }

  private Mono<Boolean> validateWriteAccess(final RepositoryIdentifier repo) {
    return scmPort
        .hasWriteAccess(repo)
        .doOnNext(
            hasAccess ->
                log.debug("Write access check for {}: {}", repo.getDisplayName(), hasAccess));
  }

  private String deriveBranchName(final ChangeRequestIdentifier changeRequest) {
    return "mr-" + changeRequest.getNumber();
  }

  private String buildCommitMessage(final String filePath, final String issueTitle) {
    return String.format("fix: apply AI-suggested fix to %s\n\n%s", filePath, issueTitle);
  }

  private Mono<Void> updateIssueAsApplied(
      final ReviewIssueEntity issue, final CommitResult commitResult) {
    return Mono.fromRunnable(
            () -> {
              issue.setFixApplied(true);
              issue.setAppliedAt(Instant.now());
              issue.setAppliedCommitSha(commitResult.commitSha());
              reviewIssueRepository.save(issue);
              log.debug(
                  "Marked issue {} as applied with commit {}",
                  issue.getId(),
                  commitResult.commitSha());
            })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
  }
}
