package com.ghiloufi.aicode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application principale du service AI Code Reviewer.
 *
 * <p>Cette application web fournit des API REST pour analyser les modifications de code (diffs)
 * provenant de Git local ou de Pull Requests GitHub. Elle utilise un modèle de langage (LLM)
 * pour effectuer l'analyse et retourne les résultats via les endpoints REST.
 *
 * <h2>Architecture Web API</h2>
 *
 * <ul>
 *   <li><b>Code Review API</b> : Gestion des revues de code (démarrage, statut, résultats)
 *   <li><b>Configuration API</b> : Gestion de la configuration applicative
 *   <li><b>System API</b> : Endpoints système (health check)
 * </ul>
 *
 * <h2>Endpoints disponibles</h2>
 *
 * <ul>
 *   <li><b>POST /api/v1/reviews</b> : Démarrer une nouvelle revue de code
 *   <li><b>GET /api/v1/reviews/{id}</b> : Récupérer les détails d'une revue
 *   <li><b>GET /api/v1/reviews/{id}/status</b> : Obtenir le statut d'une revue
 *   <li><b>GET /api/v1/reviews/{id}/results</b> : Récupérer les résultats d'une revue
 *   <li><b>GET /api/v1/configuration</b> : Obtenir la configuration actuelle
 *   <li><b>PUT /api/v1/configuration</b> : Mettre à jour la configuration
 *   <li><b>GET /api/v1/health</b> : Vérifier l'état de l'application
 * </ul>
 *
 * <h2>Documentation OpenAPI</h2>
 *
 * <p>La documentation complète de l'API est disponible via Swagger UI à l'endpoint
 * <code>/swagger-ui.html</code> une fois l'application démarrée.
 *
 * @version 2.0
 * @since 1.0
 */
@SpringBootApplication
@Slf4j
public class Application {

  /**
   * Point d'entrée principal de l'application web.
   *
   * <p>Lance le serveur Spring Boot avec les endpoints REST pour le service de revue de code.
   * L'application se contente de démarrer le serveur web et d'exposer les API REST.
   * Aucune logique de traitement n'est exécutée au démarrage.
   *
   * @param args Arguments de ligne de commande (ignorés)
   */
  public static void main(String[] args) {
    log.info("Démarrage du service AI Code Reviewer Web API");
    SpringApplication.run(Application.class, args);
    log.info("Service AI Code Reviewer Web API démarré avec succès");
  }
}
