package com.ghiloufi.aicode.infrastructure.exception;

/**
 * Exception lancée lors d'erreurs avec les services externes.
 *
 * <p>Cette exception est utilisée quand il y a des problèmes de communication
 * avec les services externes comme GitHub API, LLM, etc.
 */
public class ExternalServiceException extends CodeReviewException {

    public ExternalServiceException(String service, String message) {
        super("External service error [" + service + "]: " + message);
    }

    public ExternalServiceException(String service, String message, Throwable cause) {
        super("External service error [" + service + "]: " + message, cause);
    }
}