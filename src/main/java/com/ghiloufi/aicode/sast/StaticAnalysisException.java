package com.ghiloufi.aicode.sast;

/**
 * Exception spécifique aux erreurs d'analyse statique.
 *
 * <p>Cette exception encapsule toutes les erreurs qui peuvent survenir lors de la collecte et du
 * traitement des résultats d'analyse statique.
 */
public class StaticAnalysisException extends RuntimeException {

  /**
   * Construit une nouvelle exception avec un message.
   *
   * @param message Le message d'erreur
   */
  public StaticAnalysisException(String message) {
    super(message);
  }

  /**
   * Construit une nouvelle exception avec un message et une cause.
   *
   * @param message Le message d'erreur
   * @param cause L'exception originale
   */
  public StaticAnalysisException(String message, Throwable cause) {
    super(message, cause);
  }
}
