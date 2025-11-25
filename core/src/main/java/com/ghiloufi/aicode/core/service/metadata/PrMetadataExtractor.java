package com.ghiloufi.aicode.core.service.metadata;

import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.CommitInfo;
import com.ghiloufi.aicode.core.domain.model.PrMetadata;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public final class PrMetadataExtractor {

  private final SCMPort scmPort;
  private final ContextRetrievalConfig config;

  public Mono<PrMetadata> extractMetadata(
      final RepositoryIdentifier repo, final ChangeRequestIdentifier changeRequest) {

    if (!config.isPrMetadataEnabled()) {
      log.debug("PR metadata extraction disabled");
      return Mono.just(PrMetadata.empty());
    }

    return scmPort
        .getPullRequestMetadata(repo, changeRequest)
        .map(this::filterMetadata)
        .onErrorResume(
            error -> {
              log.warn("Failed to extract PR metadata: {}", error.getMessage());
              return Mono.just(PrMetadata.empty());
            });
  }

  private PrMetadata filterMetadata(final PrMetadata rawMetadata) {
    final var metadataConfig = config.prMetadata();

    final List<String> labels = metadataConfig.includeLabels() ? rawMetadata.labels() : List.of();

    final List<CommitInfo> commits =
        metadataConfig.includeCommits()
            ? rawMetadata.commits().stream().limit(metadataConfig.maxCommitMessages()).toList()
            : List.of();

    final String author = metadataConfig.includeAuthor() ? rawMetadata.author() : null;

    return new PrMetadata(
        rawMetadata.title(),
        rawMetadata.description(),
        author,
        rawMetadata.baseBranch(),
        rawMetadata.headBranch(),
        labels,
        commits,
        rawMetadata.changedFilesCount());
  }
}
