package com.ghiloufi.aicode.core.exception;

import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import lombok.Getter;

@Getter
public class SCMResourceNotFoundException extends SCMException {

  private final String resourceType;
  private final String resourceId;

  public SCMResourceNotFoundException(
      final String message,
      final SourceProvider provider,
      final String operationContext,
      final String resourceType,
      final String resourceId,
      final Throwable cause) {
    super(message, provider, operationContext, cause);
    this.resourceType = resourceType;
    this.resourceId = resourceId;
  }

  public SCMResourceNotFoundException(
      final String message,
      final SourceProvider provider,
      final String operationContext,
      final String resourceType,
      final String resourceId) {
    this(message, provider, operationContext, resourceType, resourceId, null);
  }
}
