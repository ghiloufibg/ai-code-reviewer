package com.ghiloufi.aicode.core.domain.port.input;

import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryInfo;
import com.ghiloufi.aicode.core.domain.model.ReviewChunk;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReviewManagementUseCase {

  Flux<ReviewChunk> streamReview(
      RepositoryIdentifier repository, ChangeRequestIdentifier changeRequest);

  Flux<ReviewChunk> streamAndPublishReview(
      RepositoryIdentifier repository, ChangeRequestIdentifier changeRequest);

  Mono<Void> publishReview(
      RepositoryIdentifier repository,
      ChangeRequestIdentifier changeRequest,
      ReviewResult reviewResult);

  Mono<Void> publishReviewFromAsync(
      SourceProvider provider, String repositoryId, int changeRequestId, ReviewResult reviewResult);

  Flux<MergeRequestSummary> getOpenChangeRequests(RepositoryIdentifier repository);

  Flux<RepositoryInfo> getAllRepositories(SourceProvider provider);
}
