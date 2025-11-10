package com.ghiloufi.aicode.core.exception;

import com.ghiloufi.aicode.core.domain.model.SourceProvider;

public class SCMAuthenticationException extends SCMException {

  public SCMAuthenticationException(
      final String message,
      final SourceProvider provider,
      final String operationContext,
      final Throwable cause) {
    super(message, provider, operationContext, cause);
  }

  public SCMAuthenticationException(
      final String message, final SourceProvider provider, final String operationContext) {
    super(message, provider, operationContext);
  }
}
