package com.ghiloufi.aicode.llm;

/**
 * Exception spécifique aux erreurs du client LLM.
 *
 * <p>Cette exception encapsule toutes les erreurs qui peuvent survenir lors de l'interaction avec
 * l'API du LLM, incluant les erreurs réseau, les erreurs de parsing JSON, et les réponses
 * invalides.
 */
public class LlmClientException extends RuntimeException {

  /**
   * Construit une nouvelle exception avec un message.
   *
   * @param message Le message d'erreur
   */
  public LlmClientException(String message) {
    super(message);
  }

  /**
   * Construit une nouvelle exception avec un message et une cause.
   *
   * @param message Le message d'erreur
   * @param cause L'exception originale
   */
  public LlmClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
