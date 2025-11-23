package com.ghiloufi.aicode.core.exception;

import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import java.time.Instant;
import lombok.Getter;

@Getter
public class SCMRateLimitException extends SCMException {

  private final Instant resetTime;
  private final int remainingRequests;

  public SCMRateLimitException(
      final String message,
      final SourceProvider provider,
      final String operationContext,
      final Instant resetTime,
      final int remainingRequests,
      final Throwable cause) {
    super(message, provider, operationContext, cause);
    this.resetTime = resetTime;
    this.remainingRequests = remainingRequests;
  }
}
