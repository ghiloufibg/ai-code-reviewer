package com.ghiloufi.aicode.core.domain.service;

import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import java.time.Instant;
import java.util.Optional;
import org.gitlab4j.api.models.MergeRequest;
import org.springframework.stereotype.Service;

@Service
public final class GitLabMergeRequestMapper {

  public MergeRequestSummary toMergeRequestSummary(final MergeRequest mergeRequest) {
    return new MergeRequestSummary(
        extractMergeRequestNumber(mergeRequest),
        mergeRequest.getTitle(),
        extractDescription(mergeRequest.getDescription()),
        mergeRequest.getState(),
        extractAuthorUsername(mergeRequest),
        mergeRequest.getSourceBranch(),
        mergeRequest.getTargetBranch(),
        extractCreatedAt(mergeRequest),
        extractUpdatedAt(mergeRequest),
        mergeRequest.getWebUrl());
  }

  private int extractMergeRequestNumber(final MergeRequest mergeRequest) {
    return Optional.ofNullable(mergeRequest.getIid()).map(Long::intValue).orElse(0);
  }

  private String extractDescription(final String description) {
    return Optional.ofNullable(description).orElse("");
  }

  private String extractAuthorUsername(final MergeRequest mergeRequest) {
    return mergeRequest.getAuthor() != null ? mergeRequest.getAuthor().getUsername() : "unknown";
  }

  private Instant extractCreatedAt(final MergeRequest mergeRequest) {
    return mergeRequest.getCreatedAt() != null ? mergeRequest.getCreatedAt().toInstant() : null;
  }

  private Instant extractUpdatedAt(final MergeRequest mergeRequest) {
    return mergeRequest.getUpdatedAt() != null ? mergeRequest.getUpdatedAt().toInstant() : null;
  }
}
