package com.ghiloufi.aicode.llm;

/**
 * Exception spécifique aux erreurs de validation JSON.
 *
 * <p>Cette exception encapsule les erreurs qui peuvent survenir lors de la validation, incluant les
 * erreurs de parsing et les erreurs de schéma.
 */
public class ReviewValidationException extends RuntimeException {

  /**
   * Construit une exception avec un message.
   *
   * @param message Le message d'erreur
   */
  public ReviewValidationException(String message) {
    super(message);
  }

  /**
   * Construit une exception avec un message et une cause.
   *
   * @param message Le message d'erreur
   * @param cause L'exception originale
   */
  public ReviewValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
