package com.ghiloufi.security.exception;

public class SecurityAnalysisException extends RuntimeException {

  public SecurityAnalysisException(final String message) {
    super(message);
  }

  public SecurityAnalysisException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
