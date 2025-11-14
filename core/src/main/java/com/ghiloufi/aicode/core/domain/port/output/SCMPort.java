package com.ghiloufi.aicode.core.domain.port.output;

import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.CommitResult;
import com.ghiloufi.aicode.core.domain.model.DiffAnalysisBundle;
import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SCMPort {

  Mono<DiffAnalysisBundle> getDiff(
      RepositoryIdentifier repo, ChangeRequestIdentifier changeRequest);

  Mono<Void> publishReview(
      RepositoryIdentifier repo, ChangeRequestIdentifier changeRequest, ReviewResult reviewResult);

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
}
