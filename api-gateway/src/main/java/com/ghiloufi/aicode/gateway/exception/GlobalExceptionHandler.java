package com.ghiloufi.aicode.gateway.exception;

import com.ghiloufi.aicode.core.exception.SCMAuthenticationException;
import com.ghiloufi.aicode.core.exception.SCMException;
import com.ghiloufi.aicode.core.exception.SCMRateLimitException;
import com.ghiloufi.aicode.core.exception.SCMResourceNotFoundException;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(SCMAuthenticationException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleAuthenticationException(
      final SCMAuthenticationException ex) {
    log.error("SCM authentication error: {}", ex.getMessage(), ex);

    final Map<String, Object> errorResponse =
        Map.of(
            "error", "authentication_failed",
            "message", "Failed to authenticate with SCM provider",
            "provider", ex.getProvider().name(),
            "context", ex.getOperationContext(),
            "timestamp", Instant.now().toString());

    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
  }

  @ExceptionHandler(SCMResourceNotFoundException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleResourceNotFoundException(
      final SCMResourceNotFoundException ex) {
    log.warn("SCM resource not found: {}", ex.getMessage());

    final Map<String, Object> errorResponse =
        Map.of(
            "error", "resource_not_found",
            "message", ex.getMessage(),
            "provider", ex.getProvider().name(),
            "resourceType", ex.getResourceType(),
            "resourceId", ex.getResourceId(),
            "timestamp", Instant.now().toString());

    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
  }

  @ExceptionHandler(SCMRateLimitException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleRateLimitException(
      final SCMRateLimitException ex) {
    log.warn("SCM rate limit exceeded: {}", ex.getMessage());

    final Map<String, Object> errorResponse =
        Map.of(
            "error", "rate_limit_exceeded",
            "message", "API rate limit exceeded for SCM provider",
            "provider", ex.getProvider().name(),
            "resetTime", ex.getResetTime().toString(),
            "remainingRequests", ex.getRemainingRequests(),
            "timestamp", Instant.now().toString());

    return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse));
  }

  @ExceptionHandler(SCMException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleSCMException(final SCMException ex) {
    log.error("SCM operation error: {}", ex.getMessage(), ex);

    final Map<String, Object> errorResponse =
        Map.of(
            "error", "scm_error",
            "message", ex.getMessage(),
            "provider", ex.getProvider().name(),
            "context", ex.getOperationContext(),
            "timestamp", Instant.now().toString());

    return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleValidationException(
      final MethodArgumentNotValidException ex) {
    log.warn("Validation error: {}", ex.getMessage());

    final Map<String, Object> errorResponse =
        Map.of(
            "error",
            "validation_failed",
            "message",
            "Request validation failed",
            "details",
            ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList(),
            "timestamp",
            Instant.now().toString());

    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
  }

  @ExceptionHandler(ServerWebInputException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleServerWebInputException(
      final ServerWebInputException ex) {
    log.warn("Invalid request input: {}", ex.getMessage());

    final Map<String, Object> errorResponse =
        Map.of(
            "error",
            "invalid_input",
            "message",
            "Invalid request input: " + ex.getReason(),
            "timestamp",
            Instant.now().toString());

    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleIllegalArgumentException(
      final IllegalArgumentException ex) {
    log.warn("Illegal argument: {}", ex.getMessage());

    final Map<String, Object> errorResponse =
        Map.of(
            "error", "illegal_argument",
            "message", ex.getMessage(),
            "timestamp", Instant.now().toString());

    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(final Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);

    final Map<String, Object> errorResponse =
        Map.of(
            "error", "internal_server_error",
            "message", "An unexpected error occurred",
            "timestamp", Instant.now().toString());

    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
  }
}
