package com.ghiloufi.aicode.orchestrator;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Orchestrateur central pour la revue de code automatisée.
 *
 * <p>Cette classe coordonne toutes les étapes du processus de revue de code :
 * collecte des diffs, analyse statique, analyse par LLM, fusion des résultats
 * et publication des conclusions.
 *
 * @since 1.0
 */
@Service
public class CodeReviewOrchestrator {

  private static final Logger logger = LoggerFactory.getLogger(CodeReviewOrchestrator.class);

  // Modes d'exécution
  private static final String MODE_LOCAL = "local";
  private static final String MODE_GITHUB = "github";

  // Chemins de sortie
  private static final String OUTPUT_DIR = "target/artifacts";
  private static final String REVIEW_FILE = "review.json";
  private static final String PROMPT_FILE = "prompt.txt";
  private static final String DIFF_FILE = "diff.patch";

  private final DiffCollectionService diffCollectionService;
  private final StaticAnalysisRunner staticAnalysisRunner;
  private final PromptBuilder promptBuilder;
  private final LlmClient llmClient;
  private final LlmReviewValidator reviewValidator;
  private final ReviewResultMerger resultMerger;
  private final GithubClient githubClient;
  private final GitHubReviewPublisher reviewPublisher;

  public CodeReviewOrchestrator(
      DiffCollectionService diffCollectionService,
      StaticAnalysisRunner staticAnalysisRunner,
      PromptBuilder promptBuilder,
      LlmClient llmClient,
      LlmReviewValidator reviewValidator,
      ReviewResultMerger resultMerger,
      @Autowired(required = false) GithubClient githubClient,
      @Autowired(required = false) GitHubReviewPublisher reviewPublisher) {
    this.diffCollectionService = diffCollectionService;
    this.staticAnalysisRunner = staticAnalysisRunner;
    this.promptBuilder = promptBuilder;
    this.llmClient = llmClient;
    this.reviewValidator = reviewValidator;
    this.resultMerger = resultMerger;
    this.githubClient = githubClient;
    this.reviewPublisher = reviewPublisher;
  }

  /**
   * Exécute le processus complet de revue de code.
   *
   * @param config Configuration de l'application
   * @throws Exception Si une erreur survient pendant l'exécution
   */
  public void executeCodeReview(ApplicationConfig config) throws Exception {
    logger.info("Démarrage d'AI Code Reviewer");

    try {
      // Collecter le diff
      DiffAnalysisBundle diffBundle = collectDiff(config);
      logger.info(
          "Diff collecté: {} fichiers, {} lignes totales",
          diffBundle.getModifiedFileCount(),
          diffBundle.getTotalLineCount());

      // Effectuer l'analyse
      ReviewResult reviewResult = performAnalysis(config, diffBundle);

      // Sauvegarder et publier les résultats
      saveArtifacts(diffBundle, reviewResult, config);
      publishResults(config, reviewResult, diffBundle);

      logger.info("Analyse terminée avec succès");

    } catch (Exception e) {
      logger.error("Erreur lors de l'exécution de l'analyse", e);
      throw e;
    }
  }

  /**
   * Collecte le diff selon le mode configuré.
   *
   * @param config Configuration de l'application
   * @return Bundle contenant le diff et métadonnées
   */
  private DiffAnalysisBundle collectDiff(ApplicationConfig config) throws IOException {
    logger.info("Collecte du diff en mode: {}", config.mode);

    if (MODE_LOCAL.equals(config.mode)) {
      logger.info("Analyse locale de {} à {}", config.fromCommit, config.toCommit);
      return diffCollectionService.collectFromLocalGit(config.fromCommit, config.toCommit);
    } else {
      logger.info("Analyse de la PR #{} sur {}", config.pullRequestNumber, config.repository);
      return diffCollectionService.collectFromGitHub(githubClient, config.pullRequestNumber);
    }
  }

  /**
   * Effectue l'analyse complète du diff.
   *
   * @param config Configuration
   * @param diffBundle Bundle de diff
   * @return Résultat de la revue fusionné
   */
  private ReviewResult performAnalysis(ApplicationConfig config, DiffAnalysisBundle diffBundle)
      throws Exception {

    logger.info("Début de l'analyse avec le modèle: {}", config.model);

    // Collecter les rapports d'analyse statique
    Map<String, Object> staticReports = staticAnalysisRunner.runAndCollect();
    logger.debug("Analyse statique collectée: {} outils", staticReports.size());

    // Découper le diff en chunks
    List<GitDiffDocument> chunks = diffBundle.splitByMaxLines(config.maxLinesPerChunk);
    logger.info("Diff découpé en {} chunk(s)", chunks.size());

    // Analyser chaque chunk
    List<ReviewResult> chunkResults = new ArrayList<>();
    for (int i = 0; i < chunks.size(); i++) {
      logger.info("Analyse du chunk {}/{}", i + 1, chunks.size());
      ReviewResult result = analyzeChunk(config, chunks.get(i), staticReports, diffBundle);
      chunkResults.add(result);
    }

    // Fusionner les résultats
    ReviewResult mergedResult = resultMerger.merge(chunkResults);
    logger.info("Résultats fusionnés: {} issues trouvées", mergedResult.getTotalItemCount());

    return mergedResult;
  }

  /**
   * Analyse un chunk de diff avec le LLM.
   *
   * @param config Configuration
   * @param chunk Chunk à analyser
   * @param staticReports Rapports d'analyse statique
   * @param fullBundle Bundle complet pour contexte
   * @return Résultat de l'analyse du chunk
   */
  private ReviewResult analyzeChunk(
      ApplicationConfig config,
      GitDiffDocument chunk,
      Map<String, Object> staticReports,
      DiffAnalysisBundle fullBundle)
      throws IOException {

    // Construire le prompt
    String userPrompt =
        promptBuilder.buildUserMessage(
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
    String llmResponse = llmClient.review(PromptBuilder.SYSTEM_PROMPT, userPrompt);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(llmResponse);
    JsonNode contentNode = rootNode.path("choices").get(0).path("message").path("content");
    llmResponse = contentNode.asText();

    // Trim whitespace
    llmResponse = extractJson(llmResponse);
    llmResponse = cleanToValidJsonChars(llmResponse);

    // Valider et retry si nécessaire
    if (!reviewValidator.isValid(PromptBuilder.OUTPUT_SCHEMA_JSON, llmResponse)) {
      logger.warn("Réponse LLM invalide, nouvelle tentative avec instruction stricte");

      String strictPrompt =
          userPrompt + "\n\nIMPORTANT: Return ONLY valid JSON complying with the schema above.";

      llmResponse = llmClient.review(PromptBuilder.SYSTEM_PROMPT, strictPrompt);

      if (!reviewValidator.isValid(PromptBuilder.OUTPUT_SCHEMA_JSON, llmResponse)) {
        logger.error("Réponse LLM toujours invalide après retry");
        throw new RuntimeException("Impossible d'obtenir une réponse valide du LLM");
      }
    }

    return ReviewResult.fromJson(llmResponse);
  }

  private String cleanToValidJsonChars(String raw) {
    if (raw == null) return null;

    // Supprimer tout caractère de contrôle sauf tabulation, retour chariot et saut de ligne
    String cleaned = raw.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

    // Supprimer les séquences parasites connues (exemple : <｜begin▁of▁sentence｜>)
    cleaned = cleaned.replaceAll("<\\|.*?\\|>", "");

    return cleaned.trim();
  }

  private static String extractJson(String input) {
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
   * @param reviewResult Résultat à publier
   * @param diffBundle Bundle pour contexte
   */
  private void publishResults(
      ApplicationConfig config, ReviewResult reviewResult, DiffAnalysisBundle diffBundle)
      throws Exception {

    if (MODE_GITHUB.equals(config.mode)
        && config.pullRequestNumber > 0
        && reviewPublisher != null) {
      logger.info("Publication des résultats sur GitHub PR #{}", config.pullRequestNumber);
      reviewPublisher.publish(config.pullRequestNumber, reviewResult, diffBundle);
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
   * Configuration complète de l'application.
   */
  public static class ApplicationConfig {
    public String mode;
    public String repository;
    public int pullRequestNumber;
    public String githubToken;
    public String fromCommit;
    public String toCommit;
    public String model;
    public String ollamaHost;
    public int timeoutSeconds;
    public int maxLinesPerChunk;
    public int contextLines;
    public String defaultBranch;
    public String javaVersion;
    public String buildSystem;
  }
}