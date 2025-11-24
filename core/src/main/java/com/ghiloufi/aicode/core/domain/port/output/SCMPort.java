package com.ghiloufi.aicode.core.domain.port.output;

import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import com.ghiloufi.aicode.core.domain.model.CommitResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import java.time.LocalDate;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SCMPort {

  Mono<DiffAnalysisBundle> getDiff(
      RepositoryIdentifier repo, ChangeRequestIdentifier changeRequest);

  Mono<Void> publishReview(
      RepositoryIdentifier repo, ChangeRequestIdentifier changeRequest, ReviewResult reviewResult);

  Mono<Void> publishSummaryComment(
      RepositoryIdentifier repo, ChangeRequestIdentifier changeRequest, String summaryComment);

  Mono<Boolean> isChangeRequestOpen(
      RepositoryIdentifier repo, ChangeRequestIdentifier changeRequest);

  Mono<RepositoryInfo> getRepository(RepositoryIdentifier repo);

  Flux<MergeRequestSummary> getOpenChangeRequests(RepositoryIdentifier repo);

  Flux<RepositoryInfo> getAllRepositories();

  SourceProvider getProviderType();

  Mono<CommitResult> applyFix(
      RepositoryIdentifier repo,
      String branchName,
      String filePath,
      String fixDiff,
      String commitMessage);

  Mono<Boolean> hasWriteAccess(RepositoryIdentifier repo);

  Mono<List<String>> listRepositoryFiles();

  Flux<CommitInfo> getCommitsFor(
      RepositoryIdentifier repo, String filePath, LocalDate since, int maxResults);

  Flux<CommitInfo> getCommitsSince(RepositoryIdentifier repo, LocalDate since, int maxResults);
}
