package com.ghiloufi.aicode.infrastructure.exception;

import com.ghiloufi.aicode.api.model.ErrorResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

/**
 * Gestionnaire global d'exceptions pour tous les contrôleurs.
 *
 * <p>Cette classe centralise la gestion des exceptions pour l'application, offrant une approche
 * cohérente pour la gestion des erreurs et la création de réponses d'erreur standardisées.
 *
 * <p>Utilise les modèles générés par OpenAPI pour assurer la cohérence avec la spécification de
 * l'API.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Gère les exceptions de revue de code introuvable.
   *
   * @param ex Exception de revue non trouvée
   * @return Réponse HTTP 404 avec détails de l'erreur
   */
  @ExceptionHandler(ReviewNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleReviewNotFoundException(ReviewNotFoundException ex) {
    log.warn("Review not found: {}", ex.getMessage());

    ErrorResponse error =
        createErrorResponse("REVIEW_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  /**
   * Gère les exceptions de revue en cours.
   *
   * @param ex Exception de revue en cours
   * @return Réponse HTTP 409 avec détails de l'erreur
   */
  @ExceptionHandler(ReviewInProgressException.class)
  public ResponseEntity<ErrorResponse> handleReviewInProgressException(
      ReviewInProgressException ex) {
    log.warn("Review in progress: {}", ex.getMessage());

    ErrorResponse error =
        createErrorResponse("REVIEW_IN_PROGRESS", ex.getMessage(), HttpStatus.CONFLICT);

    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  /**
   * Gère les exceptions de configuration.
   *
   * @param ex Exception de configuration
   * @return Réponse HTTP 400 avec détails de l'erreur
   */
  @ExceptionHandler(ConfigurationException.class)
  public ResponseEntity<ErrorResponse> handleConfigurationException(ConfigurationException ex) {
    log.error("Configuration error: {}", ex.getMessage(), ex);

    ErrorResponse error =
        createErrorResponse("CONFIGURATION_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Gère les exceptions de service externe.
   *
   * @param ex Exception de service externe
   * @return Réponse HTTP 502 avec détails de l'erreur
   */
  @ExceptionHandler(ExternalServiceException.class)
  public ResponseEntity<ErrorResponse> handleExternalServiceException(ExternalServiceException ex) {
    log.error("External service error: {}", ex.getMessage(), ex);

    ErrorResponse error =
        createErrorResponse("EXTERNAL_SERVICE_ERROR", ex.getMessage(), HttpStatus.BAD_GATEWAY);

    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
  }

  /**
   * Gère les exceptions de validation des arguments.
   *
   * @param ex Exception de validation
   * @return Réponse HTTP 400 avec détails de l'erreur
   */
  @ExceptionHandler({MethodArgumentNotValidException.class, WebExchangeBindException.class})
  public ResponseEntity<ErrorResponse> handleValidationException(Exception ex) {
    log.warn("Validation error: {}", ex.getMessage());

    String message = "Invalid request data";
    if (ex instanceof WebExchangeBindException bindEx) {
      message =
          bindEx.getBindingResult().getAllErrors().stream()
              .map(error -> error.getDefaultMessage())
              .reduce((msg1, msg2) -> msg1 + "; " + msg2)
              .orElse(message);
    }

    ErrorResponse error = createErrorResponse("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Gère les exceptions d'entrée de serveur web.
   *
   * @param ex Exception d'entrée
   * @return Réponse HTTP 400 avec détails de l'erreur
   */
  @ExceptionHandler(ServerWebInputException.class)
  public ResponseEntity<ErrorResponse> handleServerWebInputException(ServerWebInputException ex) {
    log.warn("Invalid input: {}", ex.getMessage());

    ErrorResponse error =
        createErrorResponse(
            "INVALID_INPUT", "Invalid request format: " + ex.getReason(), HttpStatus.BAD_REQUEST);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Gère les exceptions d'argument illégal.
   *
   * @param ex Exception d'argument illégal
   * @return Réponse HTTP 400 avec détails de l'erreur
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.warn("Illegal argument: {}", ex.getMessage());

    ErrorResponse error =
        createErrorResponse("INVALID_ARGUMENT", ex.getMessage(), HttpStatus.BAD_REQUEST);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Gère toutes les autres exceptions non gérées.
   *
   * @param ex Exception générique
   * @return Réponse HTTP 500 avec détails de l'erreur
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

    ErrorResponse error =
        createErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please try again later.",
            HttpStatus.INTERNAL_SERVER_ERROR);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /**
   * Crée une réponse d'erreur standardisée.
   *
   * @param code Code d'erreur
   * @param message Message d'erreur
   * @param status Statut HTTP
   * @return Réponse d'erreur
   */
  private ErrorResponse createErrorResponse(String code, String message, HttpStatus status) {
    return new ErrorResponse()
        .error(code)
        .message(message)
        .timestamp(OffsetDateTime.now())
        .details("Trace ID: " + UUID.randomUUID().toString());
  }
}
