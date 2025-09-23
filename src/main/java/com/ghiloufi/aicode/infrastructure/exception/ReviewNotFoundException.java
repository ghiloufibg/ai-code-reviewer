package com.ghiloufi.aicode.infrastructure.exception;

/**
 * Exception lancée lorsqu'une revue de code est introuvable.
 *
 * <p>Cette exception est utilisée quand on tente d'accéder à une revue de code qui n'existe pas
 * dans le système.
 */
public class ReviewNotFoundException extends CodeReviewException {

  public ReviewNotFoundException(String reviewId) {
    super("Code review not found with ID: " + reviewId);
  }

  public ReviewNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
