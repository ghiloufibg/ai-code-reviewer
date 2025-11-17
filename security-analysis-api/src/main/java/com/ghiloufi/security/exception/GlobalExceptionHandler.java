package com.ghiloufi.security.exception;

import com.ghiloufi.security.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      final MethodArgumentNotValidException ex) {
    final FieldError fieldError = ex.getBindingResult().getFieldError();
    final String message =
        fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";

    logger.warn("Validation error: {}", message);

    final ErrorResponse errorResponse =
        ErrorResponse.of("Validation error", message, HttpStatus.BAD_REQUEST.value());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(UnsupportedLanguageException.class)
  public ResponseEntity<ErrorResponse> handleUnsupportedLanguageException(
      final UnsupportedLanguageException ex) {
    logger.warn("Unsupported language error: {}", ex.getMessage());

    final ErrorResponse errorResponse =
        ErrorResponse.of(
            "Unsupported language",
            "Only Java is supported in version 0.0.2",
            HttpStatus.UNPROCESSABLE_ENTITY.value());

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
  }

  @ExceptionHandler(SecurityAnalysisException.class)
  public ResponseEntity<ErrorResponse> handleSecurityAnalysisException(
      final SecurityAnalysisException ex) {
    logger.error("Security analysis error: {}", ex.getMessage(), ex);

    final ErrorResponse errorResponse =
        ErrorResponse.of(
            "Security analysis error", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      final HttpMessageNotReadableException ex) {
    logger.warn("Malformed JSON request: {}", ex.getMessage());

    final ErrorResponse errorResponse =
        ErrorResponse.of("Malformed JSON", "Invalid JSON format", HttpStatus.BAD_REQUEST.value());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
      final HttpMediaTypeNotSupportedException ex) {
    logger.warn("Unsupported media type: {}", ex.getMessage());

    final ErrorResponse errorResponse =
        ErrorResponse.of(
            "Unsupported media type",
            "Content-Type must be application/json",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());

    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(final Exception ex) {
    logger.error("Unexpected error: {}", ex.getMessage(), ex);

    final ErrorResponse errorResponse =
        ErrorResponse.of(
            "Internal server error",
            "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR.value());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}
