package com.ghiloufi.aicode.core.domain.model.async;

import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import java.time.Instant;

public record AsyncReviewRequest(
    String requestId,
    SourceProvider provider,
    String repositoryId,
    int changeRequestId,
    String userPrompt,
    Instant createdAt) {

  public static AsyncReviewRequest create(
      String requestId,
      SourceProvider provider,
      String repositoryId,
      int changeRequestId,
      String userPrompt) {
    return new AsyncReviewRequest(
        requestId, provider, repositoryId, changeRequestId, userPrompt, Instant.now());
  }
}
