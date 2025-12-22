package com.ghiloufi.aicode.core.domain.model.async;

import com.ghiloufi.aicode.core.domain.model.ReviewMode;
import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import java.time.Instant;

public record AsyncReviewRequest(
    String requestId,
    SourceProvider provider,
    String repositoryId,
    int changeRequestId,
    ReviewMode reviewMode,
    Instant createdAt) {

  public static AsyncReviewRequest create(
      final String requestId,
      final SourceProvider provider,
      final String repositoryId,
      final int changeRequestId) {
    return create(requestId, provider, repositoryId, changeRequestId, ReviewMode.DIFF);
  }

  public static AsyncReviewRequest create(
      final String requestId,
      final SourceProvider provider,
      final String repositoryId,
      final int changeRequestId,
      final ReviewMode reviewMode) {
    return new AsyncReviewRequest(
        requestId, provider, repositoryId, changeRequestId, reviewMode, Instant.now());
  }

  public boolean isAgenticMode() {
    return reviewMode == ReviewMode.AGENTIC;
  }
}
