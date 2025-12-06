package com.ghiloufi.aicode.core.domain.model.async;

import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import java.time.Instant;

public record AsyncReviewResponse(
    String requestId,
    ReviewResult result,
    String llmProvider,
    String llmModel,
    long processingTimeMs,
    Instant completedAt) {

  public static AsyncReviewResponse success(
      String requestId,
      ReviewResult result,
      String llmProvider,
      String llmModel,
      long processingTimeMs) {
    return new AsyncReviewResponse(
        requestId, result, llmProvider, llmModel, processingTimeMs, Instant.now());
  }
}
