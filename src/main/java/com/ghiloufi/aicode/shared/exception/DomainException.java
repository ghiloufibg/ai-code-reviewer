package com.ghiloufi.aicode.shared.exception;

/**
 * Base exception for domain-related errors.
 *
 * <p>This exception represents violations of domain rules or invalid domain operations that should
 * not occur in a well-designed system.
 */
public class DomainException extends RuntimeException {

  public DomainException(String message) {
    super(message);
  }

  public DomainException(String message, Throwable cause) {
    super(message, cause);
  }
}
