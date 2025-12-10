package com.ghiloufi.aicode.gateway.dto;

import com.ghiloufi.aicode.core.domain.model.async.ReviewStatus;

public record ReviewSubmissionResponse(String requestId, ReviewStatus status, String statusUrl) {

  public static ReviewSubmissionResponse pending(String requestId, String statusUrl) {
    return new ReviewSubmissionResponse(requestId, ReviewStatus.PENDING, statusUrl);
  }
}
