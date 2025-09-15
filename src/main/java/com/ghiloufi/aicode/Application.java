package com.ghiloufi.aicode;

import com.ghiloufi.aicode.config.ApplicationConfig;
import com.ghiloufi.aicode.orchestrator.CodeReviewOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application principale du plugin AI Code Reviewer.
 *
 * <p>Cette application analyse les modifications de code (diffs) provenant de Git local ou de Pull
 * Requests GitHub, les envoie à un modèle de langage (LLM) pour analyse, et publie les résultats de
 * la revue de code.
 *
 * <h2>Modes de fonctionnement</h2>
 *
 * <ul>
 *   <li><b>Mode local</b> : Analyse les commits locaux pour tests et développement
 *   <li><b>Mode GitHub</b> : Analyse les Pull Requests GitHub en production
 * </ul>
 *
 * <h2>Workflow de l'application</h2>
 *
 * <ol>
 *   <li>Collecte du diff (local Git ou GitHub PR)
 *   <li>Exécution de l'analyse statique (Checkstyle, PMD, SpotBugs, Semgrep)
 *   <li>Découpage du diff en chunks si nécessaire
 *   <li>Envoi de chaque chunk au LLM pour analyse
 *   <li>Validation et fusion des résultats
 *   <li>Publication des résultats (console ou GitHub)
 * </ol>
 *
 * <h2>Configuration via application.properties</h2>
 *
 * <p>La configuration se fait maintenant exclusivement via le fichier application.properties et les
 * variables d'environnement, conformément aux bonnes pratiques Spring Boot.
 *
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class Application implements CommandLineRunner {

  private static final String MODE_LOCAL = "local";

  private final CodeReviewOrchestrator orchestrator;
  private final ApplicationConfig applicationConfig;

  /**
   * Point d'entrée principal de l'application.
   *
   * @param args Arguments de ligne de commande
   */
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  /**
   * Méthode principale d'exécution après le démarrage Spring.
   *
   * @param args Arguments de ligne de commande (ignorés)
   * @throws Exception Si une erreur survient pendant l'exécution
   */
  @Override
  public void run(String... args) throws Exception {
    try {
      // Initialiser le token GitHub depuis l'environnement
      applicationConfig.initializeGithubToken();

      // Valider la configuration GitHub spécifique
      applicationConfig.validateGithubMode();
      logConfiguration(applicationConfig);

      // Déléguer l'exécution à l'orchestrateur (réactif)
      orchestrator.executeCodeReview(applicationConfig).block();

    } catch (Exception e) {
      log.error("Erreur lors de l'exécution de l'analyse", e);
      throw e;
    }
  }

  /**
   * Log la configuration pour debug.
   *
   * @param config Configuration à logger
   */
  private void logConfiguration(ApplicationConfig config) {
    log.info("Configuration:");
    log.info("  Mode: {}", config.mode);
    log.info("  Repository: {}", config.repository);
    log.info("  Model: {}", config.model);
    log.info("  Ollama: {}", config.ollamaHost);
    log.info("  Max lines/chunk: {}", config.maxLinesPerChunk);
    log.info("  Context lines: {}", config.contextLines);
    log.info("  Timeout: {}s", config.timeoutSeconds);

    if (MODE_LOCAL.equals(config.mode)) {
      log.info("  From commit: {}", config.fromCommit);
      log.info("  To commit: {}", config.toCommit);
    } else {
      log.info("  PR number: {}", config.pullRequestNumber);
    }
  }
}
