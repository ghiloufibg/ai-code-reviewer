package com.ghiloufi.aicode.core.exception;

import com.ghiloufi.aicode.core.domain.model.SourceProvider;
import java.util.Optional;
import lombok.Getter;

@Getter
public class SCMException extends RuntimeException {

  private final SourceProvider provider;
  private final String operationContext;

  public SCMException(
      final String message,
      final SourceProvider provider,
      final String operationContext,
      final Throwable cause) {
    super(formatMessage(message, provider, operationContext), cause);
    this.provider = provider;
    this.operationContext = operationContext;
  }

  public SCMException(
      final String message, final SourceProvider provider, final String operationContext) {
    this(message, provider, operationContext, null);
  }

  private static String formatMessage(
      final String message, final SourceProvider provider, final String operationContext) {
    return String.format(
        "[%s] %s - Context: %s",
        Optional.ofNullable(provider).map(SourceProvider::getDisplayName).orElse("UNKNOWN"),
        message,
        operationContext);
  }
}
