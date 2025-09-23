package com.ghiloufi.aicode.exception;

/**
 * Exception lancée lors d'erreurs de configuration.
 *
 * <p>Cette exception est utilisée quand la configuration de l'application est invalide ou
 * incomplète.
 */
public class ConfigurationException extends CodeReviewException {

  public ConfigurationException(String message) {
    super(message);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
