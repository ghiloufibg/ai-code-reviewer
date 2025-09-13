package com.ghiloufi.aicode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghiloufi.aicode.core.DiffCollectionService;
import com.ghiloufi.aicode.core.GitHubReviewPublisher;
import com.ghiloufi.aicode.core.ReviewResultMerger;
import com.ghiloufi.aicode.domain.DiffAnalysisBundle;
import com.ghiloufi.aicode.domain.GitDiffDocument;
import com.ghiloufi.aicode.domain.ReviewResult;
import com.ghiloufi.aicode.github.GithubClient;
import com.ghiloufi.aicode.llm.LlmClient;
import com.ghiloufi.aicode.llm.LlmReviewValidator;
import com.ghiloufi.aicode.llm.PromptBuilder;
import com.ghiloufi.aicode.sast.StaticAnalysisRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
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

  // Chemins de sortie
  private static final String OUTPUT_DIR = "target/artifacts";
  private static final String REVIEW_FILE = "review.json";
  private static final String PROMPT_FILE = "prompt.txt";
  private static final String DIFF_FILE = "diff.patch";

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

    logger.info("Démarrage d'AI Code Reviewer");

    try {
      // Parser la configuration
      ApplicationConfig config = parseConfiguration(args);
      logConfiguration(config);

      // Initialiser les services
      ServiceContainer services = initializeServices(config);

      // Collecter le diff
      DiffAnalysisBundle diffBundle = collectDiff(config, services);
      logger.info(
          "Diff collecté: {} fichiers, {} lignes totales",
          diffBundle.getModifiedFileCount(),
          diffBundle.getTotalLineCount());

      // Effectuer l'analyse
      ReviewResult reviewResult = performAnalysis(config, services, diffBundle);

      // Sauvegarder et publier les résultats
      saveArtifacts(diffBundle, reviewResult, config);
      publishResults(config, services, reviewResult, diffBundle);

      logger.info("Analyse terminée avec succès");

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
  private ApplicationConfig parseConfiguration(String[] args) {
    Map<String, String> cliArgs = parseCommandLineArgs(args);

    ApplicationConfig config = new ApplicationConfig();

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
   * Initialise tous les services nécessaires.
   *
   * @param config Configuration de l'application
   * @return Container avec tous les services initialisés
   */
  private ServiceContainer initializeServices(ApplicationConfig config) {
    ServiceContainer services = new ServiceContainer();

    // Services GitHub (si nécessaire)
    if (MODE_GITHUB.equals(config.mode)) {
      services.githubClient = new GithubClient(config.repository, config.githubToken);
      services.reviewPublisher = new GitHubReviewPublisher(services.githubClient);
    }

    // Services de diff et analyse
    services.diffCollector = new DiffCollectionService(config.contextLines, config.repository);
    services.staticAnalysisRunner = new StaticAnalysisRunner();

    // Services LLM
    services.promptBuilder = new PromptBuilder();
    services.llmClient =
        new LlmClient(config.ollamaHost, config.model, Duration.ofSeconds(config.timeoutSeconds));
    services.reviewValidator = new LlmReviewValidator();

    // Service de fusion
    services.resultMerger = new ReviewResultMerger();

    logger.debug("Services initialisés avec succès");
    return services;
  }

  /**
   * Collecte le diff selon le mode configuré.
   *
   * @param config Configuration de l'application
   * @param services Container de services
   * @return Bundle contenant le diff et métadonnées
   */
  private DiffAnalysisBundle collectDiff(ApplicationConfig config, ServiceContainer services)
      throws IOException {

    logger.info("Collecte du diff en mode: {}", config.mode);

    if (MODE_LOCAL.equals(config.mode)) {
      logger.info("Analyse locale de {} à {}", config.fromCommit, config.toCommit);
      return services.diffCollector.collectFromLocalGit(config.fromCommit, config.toCommit);
    } else {
      logger.info("Analyse de la PR #{} sur {}", config.pullRequestNumber, config.repository);
      return services.diffCollector.collectFromGitHub(
          services.githubClient, config.pullRequestNumber);
    }
  }

  /**
   * Effectue l'analyse complète du diff.
   *
   * @param config Configuration
   * @param services Services
   * @param diffBundle Bundle de diff
   * @return Résultat de la revue fusionné
   */
  private ReviewResult performAnalysis(
      ApplicationConfig config, ServiceContainer services, DiffAnalysisBundle diffBundle)
      throws Exception {

    logger.info("Début de l'analyse avec le modèle: {}", config.model);

    // Collecter les rapports d'analyse statique
    Map<String, Object> staticReports = services.staticAnalysisRunner.runAndCollect();
    logger.debug("Analyse statique collectée: {} outils", staticReports.size());

    // Découper le diff en chunks
    List<GitDiffDocument> chunks = diffBundle.splitByMaxLines(config.maxLinesPerChunk);
    logger.info("Diff découpé en {} chunk(s)", chunks.size());

    // Analyser chaque chunk
    List<ReviewResult> chunkResults = new ArrayList<>();
    for (int i = 0; i < chunks.size(); i++) {
      logger.info("Analyse du chunk {}/{}", i + 1, chunks.size());
      ReviewResult result =
          analyzeChunk(config, services, chunks.get(i), staticReports, diffBundle);
      chunkResults.add(result);
    }

    // Fusionner les résultats
    ReviewResult mergedResult = services.resultMerger.merge(chunkResults);
    logger.info("Résultats fusionnés: {} issues trouvées", mergedResult.getTotalItemCount());

    return mergedResult;
  }

  /**
   * Analyse un chunk de diff avec le LLM.
   *
   * @param config Configuration
   * @param services Services
   * @param chunk Chunk à analyser
   * @param staticReports Rapports d'analyse statique
   * @param fullBundle Bundle complet pour contexte
   * @return Résultat de l'analyse du chunk
   */
  private ReviewResult analyzeChunk(
      ApplicationConfig config,
      ServiceContainer services,
      GitDiffDocument chunk,
      Map<String, Object> staticReports,
      DiffAnalysisBundle fullBundle)
      throws IOException {

    // Construire le prompt
    String userPrompt =
        services.promptBuilder.buildUserMessage(
            config.repository,
            config.defaultBranch,
            config.javaVersion,
            config.buildSystem,
            chunk.toUnifiedString(),
            staticReports,
            fullBundle.getProjectConfiguration(),
            fullBundle.getTestStatus());

    logger.trace("Prompt construit, taille: {} caractères", userPrompt.length());

    // Envoyer au LLM
    String llmResponse = services.llmClient.review(PromptBuilder.SYSTEM_PROMPT, userPrompt);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(llmResponse);
    JsonNode contentNode = rootNode.path("choices").get(0).path("message").path("content");
    llmResponse = contentNode.asText();

    // Trim whitespace
    llmResponse = extractJson(llmResponse);

    llmResponse = cleanToValidJsonChars(llmResponse);

    // Valider et retry si nécessaire
    if (!services.reviewValidator.isValid(PromptBuilder.OUTPUT_SCHEMA_JSON, llmResponse)) {
      logger.warn("Réponse LLM invalide, nouvelle tentative avec instruction stricte");

      String strictPrompt =
          userPrompt + "\n\nIMPORTANT: Return ONLY valid JSON complying with the schema above.";

      llmResponse = services.llmClient.review(PromptBuilder.SYSTEM_PROMPT, strictPrompt);

      if (!services.reviewValidator.isValid(PromptBuilder.OUTPUT_SCHEMA_JSON, llmResponse)) {
        logger.error("Réponse LLM toujours invalide après retry");
        throw new RuntimeException("Impossible d'obtenir une réponse valide du LLM");
      }
    }

    return ReviewResult.fromJson(llmResponse);
  }

  public String cleanToValidJsonChars(String raw) {
    if (raw == null) return null;

    // Supprimer tout caractère de contrôle sauf tabulation, retour chariot et saut de ligne
    String cleaned = raw.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

    // Supprimer les séquences parasites connues (exemple : <｜begin▁of▁sentence｜>)
    cleaned = cleaned.replaceAll("<\\|.*?\\|>", "");

    return cleaned.trim();
  }

  public static String extractJson(String input) {
    int start = input.indexOf("{");
    int end = input.lastIndexOf("}");
    if (start != -1 && end != -1 && end > start) {
      return input.substring(start, end + 1).trim();
    }
    return null;
  }

  /**
   * Sauvegarde les artifacts de l'analyse.
   *
   * @param diffBundle Bundle de diff
   * @param reviewResult Résultat de la revue
   * @param config Configuration pour les chemins
   */
  private void saveArtifacts(
      DiffAnalysisBundle diffBundle, ReviewResult reviewResult, ApplicationConfig config)
      throws IOException {

    Path outputDir = Path.of(OUTPUT_DIR);
    Files.createDirectories(outputDir);

    // Sauvegarder le résultat de la revue
    Path reviewPath = outputDir.resolve(REVIEW_FILE);
    Files.writeString(reviewPath, reviewResult.toJson());
    logger.info("Résultat sauvegardé dans: {}", reviewPath);

    // Sauvegarder le diff
    Path diffPath = outputDir.resolve(DIFF_FILE);
    Files.writeString(diffPath, diffBundle.getUnifiedDiffString());
    logger.info("Diff sauvegardé dans: {}", diffPath);

    // Sauvegarder un résumé du prompt (pas le prompt complet pour économiser l'espace)
    Path promptPath = outputDir.resolve(PROMPT_FILE);
    String promptSummary =
        String.format(
            "Mode: %s\nRepository: %s\nModel: %s\nChunks: %d\nTimestamp: %s",
            config.mode,
            config.repository,
            config.model,
            diffBundle.splitByMaxLines(config.maxLinesPerChunk).size(),
            new Date());
    Files.writeString(promptPath, promptSummary);
    logger.debug("Résumé du prompt sauvegardé");
  }

  /**
   * Publie les résultats selon le mode configuré.
   *
   * @param config Configuration
   * @param services Services
   * @param reviewResult Résultat à publier
   * @param diffBundle Bundle pour contexte
   */
  private void publishResults(
      ApplicationConfig config,
      ServiceContainer services,
      ReviewResult reviewResult,
      DiffAnalysisBundle diffBundle)
      throws Exception {

    if (MODE_GITHUB.equals(config.mode) && config.pullRequestNumber > 0) {
      logger.info("Publication des résultats sur GitHub PR #{}", config.pullRequestNumber);
      services.reviewPublisher.publish(config.pullRequestNumber, reviewResult, diffBundle);
      logger.info("Résultats publiés avec succès sur GitHub");
    } else {
      // Mode local ou pas de PR : afficher en console
      System.out.println("\n" + "=".repeat(80));
      System.out.println("RÉSULTAT DE LA REVUE DE CODE");
      System.out.println("=".repeat(80));
      System.out.println(reviewResult.toPrettyJson());
      System.out.println("=".repeat(80));

      logger.info("Résultats affichés en console");
    }
  }

  /**
   * Valide la configuration.
   *
   * @param config Configuration à valider
   * @throws IllegalArgumentException Si la configuration est invalide
   */
  private void validateConfiguration(ApplicationConfig config) {
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
  private void logConfiguration(ApplicationConfig config) {
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

  /** Configuration complète de l'application. */
  private static class ApplicationConfig {
    String mode;
    String repository;
    int pullRequestNumber;
    String githubToken;
    String fromCommit;
    String toCommit;
    String model;
    String ollamaHost;
    int timeoutSeconds;
    int maxLinesPerChunk;
    int contextLines;
    String defaultBranch;
    String javaVersion;
    String buildSystem;
  }

  /** Container pour tous les services de l'application. */
  private static class ServiceContainer {
    GithubClient githubClient;
    GitHubReviewPublisher reviewPublisher;
    DiffCollectionService diffCollector;
    StaticAnalysisRunner staticAnalysisRunner;
    PromptBuilder promptBuilder;
    LlmClient llmClient;
    LlmReviewValidator reviewValidator;
    ReviewResultMerger resultMerger;
  }
}
