package com.ghiloufi.aicode.core.exception;

public final class JsonValidationException extends RuntimeException {

  public JsonValidationException(final String message) {
    super(message);
  }

  public JsonValidationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
