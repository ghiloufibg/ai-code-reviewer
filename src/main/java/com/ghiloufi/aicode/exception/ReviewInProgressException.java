package com.ghiloufi.aicode.exception;

/**
 * Exception lancée lorsqu'on tente d'accéder aux résultats d'une revue en cours.
 *
 * <p>Cette exception est utilisée quand on essaie de récupérer les résultats
 * d'une revue de code qui n'est pas encore terminée.
 */
public class ReviewInProgressException extends CodeReviewException {

    public ReviewInProgressException(String reviewId) {
        super("Code review is still in progress, results not available yet: " + reviewId);
    }

    public ReviewInProgressException(String message, Throwable cause) {
        super(message, cause);
    }
}