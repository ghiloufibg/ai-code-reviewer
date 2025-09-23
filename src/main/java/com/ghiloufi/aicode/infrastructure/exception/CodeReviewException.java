package com.ghiloufi.aicode.infrastructure.exception;

/**
 * Exception de base pour les erreurs liées aux revues de code.
 *
 * <p>Cette exception sert de classe parent pour toutes les exceptions
 * spécifiques aux opérations de revue de code dans l'application.
 */
public class CodeReviewException extends RuntimeException {

    public CodeReviewException(String message) {
        super(message);
    }

    public CodeReviewException(String message, Throwable cause) {
        super(message, cause);
    }
}