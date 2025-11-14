package com.ghiloufi.aicode.core.application.service;

import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.CommitResult;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public final class FixApplicationService {

  private final SCMPort scmPort;

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
}
