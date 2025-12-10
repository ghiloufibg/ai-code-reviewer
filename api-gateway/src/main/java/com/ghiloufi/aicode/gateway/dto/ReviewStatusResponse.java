package com.ghiloufi.aicode.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.async.ReviewStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewStatusResponse(
    String requestId,
    ReviewStatus status,
    ReviewResult result,
    String error,
    Long processingTimeMs) {

  public static ReviewStatusResponse pending(String requestId) {
    return new ReviewStatusResponse(requestId, ReviewStatus.PENDING, null, null, null);
  }

  public static ReviewStatusResponse processing(String requestId) {
    return new ReviewStatusResponse(requestId, ReviewStatus.PROCESSING, null, null, null);
  }

  public static ReviewStatusResponse completed(
      String requestId, ReviewResult result, long processingTimeMs) {
    return new ReviewStatusResponse(
        requestId, ReviewStatus.COMPLETED, result, null, processingTimeMs);
  }

  public static ReviewStatusResponse failed(String requestId, String error) {
    return new ReviewStatusResponse(requestId, ReviewStatus.FAILED, null, error, null);
  }
}
