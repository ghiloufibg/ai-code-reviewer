package com.ghiloufi.security.exception;

public class UnsupportedLanguageException extends SecurityAnalysisException {

  public UnsupportedLanguageException(final String language) {
    super(
        String.format(
            "Unsupported language: %s. Only Java is supported in version 0.0.2", language));
  }
}
