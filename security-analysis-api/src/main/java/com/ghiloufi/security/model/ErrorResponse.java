package com.ghiloufi.security.model;

import java.time.LocalDateTime;

public record ErrorResponse(String error, String message, int status, LocalDateTime timestamp) {

  public static ErrorResponse of(final String error, final String message, final int status) {
    return new ErrorResponse(error, message, status, LocalDateTime.now());
  }
}
