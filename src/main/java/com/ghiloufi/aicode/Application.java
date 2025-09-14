package com.ghiloufi.aicode;

import com.ghiloufi.aicode.orchestrator.CodeReviewOrchestrator;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;

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
 * <h2>Configuration via arguments ou variables d'environnement</h2>
 *
 * <pre>
 * Arguments CLI :
 *   --mode        : "local" ou "github" (défaut: github)
 *   --repo        : Repository GitHub (format: owner/repo)
 *   --pr          : Numéro de Pull Request
 *   --model       : Modèle LLM à utiliser
 *   --ollama      : URL du serveur Ollama
 *   --max-lines   : Nombre max de lignes par chunk
 *   --context     : Lignes de contexte autour des modifications
 *   --timeout     : Timeout pour les requêtes LLM (secondes)
 *
 * Variables d'environnement :
 *   GITHUB_REPOSITORY : Repository GitHub
 *   PR_NUMBER         : Numéro de PR
 *   GITHUB_TOKEN      : Token d'authentification GitHub
 *   MODEL             : Modèle LLM
 *   OLLAMA_HOST       : URL du serveur Ollama
 * </pre>
 *
 * <h2>Exemples d'utilisation</h2>
 *
 * <h3>Test en local avec commits Git</h3>
 *
 * <pre>{@code
 * # Analyser le dernier commit
 * java -jar ai-code-reviewer.jar \
 *   --mode local \
 *   --ollama http://localhost:11434 \
 *   --model codellama:13b
 *
 * # Analyser les 3 derniers commits
 * java -jar ai-code-reviewer.jar \
 *   --mode local \
 *   --from HEAD~3 \
 *   --to HEAD
 * }</pre>
 *
 * <h3>Analyse d'une Pull Request GitHub</h3>
 *
 * <pre>{@code
 * export GITHUB_TOKEN=ghp_xxxxx
 * java -jar ai-code-reviewer.jar \
 *   --mode github \
 *   --repo myorg/myproject \
 *   --pr 42
 * }</pre>
 *
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication(
    exclude = {HttpClientAutoConfiguration.class, RestClientAutoConfiguration.class})
public class Application implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(Application.class);

  private final CodeReviewOrchestrator orchestrator;

  public Application(CodeReviewOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  // Modes d'exécution
  private static final String MODE_LOCAL = "local";
  private static final String MODE_GITHUB = "github";

  // Valeurs par défaut
  private static final String DEFAULT_MODEL = "deepseek-coder-6.7b-instruct";
  private static final String DEFAULT_OLLAMA_HOST = "http://localhost:1234";
  private static final String DEFAULT_BRANCH = "main";
  private static final String DEFAULT_JAVA_VERSION = "17";
  private static final String DEFAULT_BUILD_SYSTEM = "maven";
  private static final int DEFAULT_MAX_LINES = 1500;
  private static final int DEFAULT_CONTEXT_LINES = 5;
  private static final int DEFAULT_TIMEOUT_SECONDS = 45;

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
   * @param args Arguments de ligne de commande
   * @throws Exception Si une erreur survient pendant l'exécution
   */
  @Override
  public void run(String... args) throws Exception {
    try {
      // Parser la configuration
      CodeReviewOrchestrator.ApplicationConfig config = parseConfiguration(args);
      logConfiguration(config);

      // Déléguer l'exécution à l'orchestrateur
      orchestrator.executeCodeReview(config);

    } catch (Exception e) {
      logger.error("Erreur lors de l'exécution de l'analyse", e);
      throw e;
    }
  }

  /**
   * Parse la configuration depuis les arguments et variables d'environnement.
   *
   * @param args Arguments de ligne de commande
   * @return Configuration complète de l'application
   */
  private CodeReviewOrchestrator.ApplicationConfig parseConfiguration(String[] args) {
    Map<String, String> cliArgs = parseCommandLineArgs(args);

    CodeReviewOrchestrator.ApplicationConfig config = new CodeReviewOrchestrator.ApplicationConfig();

    // Mode d'exécution
    config.mode = cliArgs.getOrDefault("--mode", MODE_GITHUB);

    // Configuration GitHub
    config.repository = cliArgs.getOrDefault("--repo", getEnvOrDefault("GITHUB_REPOSITORY", ""));
    config.pullRequestNumber =
        Integer.parseInt(cliArgs.getOrDefault("--pr", getEnvOrDefault("PR_NUMBER", "0")));
    config.githubToken = System.getenv("GITHUB_TOKEN");

    // Configuration Git local
    config.fromCommit = cliArgs.getOrDefault("--from", "HEAD~1");
    config.toCommit = cliArgs.getOrDefault("--to", "HEAD");

    // Configuration LLM
    config.model = cliArgs.getOrDefault("--model", getEnvOrDefault("MODEL", DEFAULT_MODEL));
    config.ollamaHost =
        cliArgs.getOrDefault("--ollama", getEnvOrDefault("OLLAMA_HOST", DEFAULT_OLLAMA_HOST));
    config.timeoutSeconds =
        Integer.parseInt(
            cliArgs.getOrDefault("--timeout", String.valueOf(DEFAULT_TIMEOUT_SECONDS)));

    // Configuration de l'analyse
    config.maxLinesPerChunk =
        Integer.parseInt(cliArgs.getOrDefault("--max-lines", String.valueOf(DEFAULT_MAX_LINES)));
    config.contextLines =
        Integer.parseInt(cliArgs.getOrDefault("--context", String.valueOf(DEFAULT_CONTEXT_LINES)));

    // Métadonnées du projet
    config.defaultBranch = cliArgs.getOrDefault("--branch", DEFAULT_BRANCH);
    config.javaVersion = cliArgs.getOrDefault("--java-version", DEFAULT_JAVA_VERSION);
    config.buildSystem = cliArgs.getOrDefault("--build-system", DEFAULT_BUILD_SYSTEM);

    validateConfiguration(config);

    return config;
  }

  /**
   * Parse les arguments de ligne de commande.
   *
   * @param args Tableau d'arguments
   * @return Map des arguments parsés
   */
  private Map<String, String> parseCommandLineArgs(String[] args) {
    Map<String, String> result = new HashMap<>();
    String currentKey = null;

    for (String arg : args) {
      if (arg.startsWith("--")) {
        currentKey = arg;
        result.putIfAbsent(currentKey, "");
      } else if (currentKey != null) {
        result.put(currentKey, arg);
        currentKey = null;
      }
    }

    return result;
  }


  /**
   * Valide la configuration.
   *
   * @param config Configuration à valider
   * @throws IllegalArgumentException Si la configuration est invalide
   */
  private void validateConfiguration(CodeReviewOrchestrator.ApplicationConfig config) {
    if (!MODE_LOCAL.equals(config.mode) && !MODE_GITHUB.equals(config.mode)) {
      throw new IllegalArgumentException(
          "Mode invalide: " + config.mode + ". Utiliser 'local' ou 'github'");
    }

    if (MODE_GITHUB.equals(config.mode)) {
      if (config.repository.isBlank()) {
        throw new IllegalArgumentException(
            "Repository requis en mode GitHub (--repo ou GITHUB_REPOSITORY)");
      }
      if (config.pullRequestNumber <= 0) {
        throw new IllegalArgumentException(
            "Numéro de PR requis en mode GitHub (--pr ou PR_NUMBER)");
      }
      if (config.githubToken == null || config.githubToken.isBlank()) {
        throw new IllegalArgumentException("Token GitHub requis (variable GITHUB_TOKEN)");
      }
    }

    if (config.maxLinesPerChunk <= 0) {
      throw new IllegalArgumentException("max-lines doit être positif");
    }

    if (config.timeoutSeconds <= 0) {
      throw new IllegalArgumentException("timeout doit être positif");
    }
  }

  /**
   * Log la configuration pour debug.
   *
   * @param config Configuration à logger
   */
  private void logConfiguration(CodeReviewOrchestrator.ApplicationConfig config) {
    logger.info("Configuration:");
    logger.info("  Mode: {}", config.mode);
    logger.info("  Repository: {}", config.repository);
    logger.info("  Model: {}", config.model);
    logger.info("  Ollama: {}", config.ollamaHost);
    logger.info("  Max lines/chunk: {}", config.maxLinesPerChunk);
    logger.info("  Context lines: {}", config.contextLines);
    logger.info("  Timeout: {}s", config.timeoutSeconds);

    if (MODE_LOCAL.equals(config.mode)) {
      logger.info("  From commit: {}", config.fromCommit);
      logger.info("  To commit: {}", config.toCommit);
    } else {
      logger.info("  PR number: {}", config.pullRequestNumber);
    }
  }

  /**
   * Obtient une variable d'environnement avec valeur par défaut.
   *
   * @param name Nom de la variable
   * @param defaultValue Valeur par défaut
   * @return Valeur de la variable ou défaut
   */
  private String getEnvOrDefault(String name, String defaultValue) {
    String value = System.getenv(name);
    return (value != null && !value.isBlank()) ? value : defaultValue;
  }

}
