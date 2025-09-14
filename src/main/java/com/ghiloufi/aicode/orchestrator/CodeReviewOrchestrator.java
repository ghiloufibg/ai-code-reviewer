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
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Orchestrateur central pour la revue de code automatisée.
 *
 * <p>Cette classe coordonne toutes les étapes du processus de revue de code : collecte des diffs,
 * analyse statique, analyse par LLM, fusion des résultats et publication des conclusions.
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

  private static String extractJson(String input) {
    int start = input.indexOf("{");
    int end = input.lastIndexOf("}");
    if (start != -1 && end != -1 && end > start) {
      return input.substring(start, end + 1).trim();
    }
    return input;
  }

  /**
   * Exécute le processus complet de revue de code de manière réactive.
   *
   * @param config Configuration de l'application
   * @return Mono<Void> qui se complète quand l'analyse est terminée
   */
  public Mono<Void> executeCodeReview(ApplicationConfig config) {
    logger.info("Démarrage d'AI Code Reviewer réactif");

    return collectDiff(config)
        .doOnNext(
            diffBundle ->
                logger.info(
                    "Diff collecté: {} fichiers, {} lignes totales",
                    diffBundle.getModifiedFileCount(),
                    diffBundle.getTotalLineCount()))
        .flatMap(
            diffBundle ->
                performAnalysis(config, diffBundle)
                    .flatMap(
                        reviewResult ->
                            saveArtifacts(diffBundle, reviewResult, config)
                                .then(publishResults(config, reviewResult, diffBundle))))
        .doOnSuccess(unused -> logger.info("Analyse terminée avec succès"))
        .doOnError(error -> logger.error("Erreur lors de l'exécution de l'analyse", error))
        .then();
  }

  /**
   * Collecte le diff selon le mode configuré de manière réactive.
   *
   * @param config Configuration de l'application
   * @return Mono<DiffAnalysisBundle> contenant le diff et métadonnées
   */
  private Mono<DiffAnalysisBundle> collectDiff(ApplicationConfig config) {
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
   * Effectue l'analyse complète du diff de manière réactive.
   *
   * @param config Configuration
   * @param diffBundle Bundle de diff
   * @return Mono<ReviewResult> contenant le résultat de la revue fusionné
   */
  private Mono<ReviewResult> performAnalysis(
      ApplicationConfig config, DiffAnalysisBundle diffBundle) {
    logger.info("Début de l'analyse avec le modèle: {}", config.model);

    return staticAnalysisRunner
        .runAndCollect()
        .doOnNext(
            staticReports ->
                logger.debug("Analyse statique collectée: {} outils", staticReports.size()))
        .flatMap(
            staticReports -> {
              List<GitDiffDocument> chunks = diffBundle.splitByMaxLines(config.maxLinesPerChunk);
              logger.info("Diff découpé en {} chunk(s)", chunks.size());

              return Flux.fromIterable(chunks)
                  .index()
                  .flatMap(
                      indexedChunk -> {
                        long index = indexedChunk.getT1();
                        GitDiffDocument chunk = indexedChunk.getT2();
                        logger.info("Analyse du chunk {}/{}", index + 1, chunks.size());
                        return analyzeChunk(config, chunk, staticReports, diffBundle);
                      })
                  .collectList()
                  .flatMap(
                      chunkResults -> {
                        logger.info("Fusion des résultats de {} chunks", chunkResults.size());
                        return resultMerger.merge(chunkResults);
                      })
                  .doOnNext(
                      mergedResult ->
                          logger.info(
                              "Résultats fusionnés: {} issues trouvées",
                              mergedResult.getTotalItemCount()));
            });
  }

  /**
   * Analyse un chunk de diff avec le LLM de manière réactive.
   *
   * @param config Configuration
   * @param chunk Chunk à analyser
   * @param staticReports Rapports d'analyse statique
   * @param fullBundle Bundle complet pour contexte
   * @return Mono<ReviewResult> contenant le résultat de l'analyse du chunk
   */
  private Mono<ReviewResult> analyzeChunk(
      ApplicationConfig config,
      GitDiffDocument chunk,
      Map<String, Object> staticReports,
      DiffAnalysisBundle fullBundle) {

    return Mono.fromCallable(
            () ->
                promptBuilder.buildUserMessage(
                    config.repository,
                    config.defaultBranch,
                    config.javaVersion,
                    config.buildSystem,
                    chunk.toUnifiedString(),
                    staticReports,
                    fullBundle.getProjectConfiguration(),
                    fullBundle.getTestStatus()))
        .doOnNext(
            userPrompt ->
                logger.trace("Prompt construit, taille: {} caractères", userPrompt.length()))
        .flatMap(
            userPrompt ->
                llmClient
                    .review(PromptBuilder.SYSTEM_PROMPT, userPrompt)
                    .map(this::processLlmResponse)
                    .flatMap(llmResponse -> validateAndRetryIfNeeded(userPrompt, llmResponse)))
        .map(this::parseReviewResult);
  }

  /**
   * Analyse un chunk avec streaming du LLM pour une réponse plus rapide.
   *
   * @param config Configuration
   * @param chunk Chunk à analyser
   * @param staticReports Rapports d'analyse statique
   * @param fullBundle Bundle complet pour contexte
   * @return Mono<ReviewResult> avec streaming interne
   */
  public Mono<ReviewResult> analyzeChunkWithStreaming(
      ApplicationConfig config,
      GitDiffDocument chunk,
      Map<String, Object> staticReports,
      DiffAnalysisBundle fullBundle) {

    return Mono.fromCallable(
            () ->
                promptBuilder.buildUserMessage(
                    config.repository,
                    config.defaultBranch,
                    config.javaVersion,
                    config.buildSystem,
                    chunk.toUnifiedString(),
                    staticReports,
                    fullBundle.getProjectConfiguration(),
                    fullBundle.getTestStatus()))
        .flatMap(
            userPrompt -> {
              logger.debug("Démarrage de l'analyse chunk avec streaming");

              return llmClient
                  .reviewStream(PromptBuilder.SYSTEM_PROMPT, userPrompt)
                  .reduce(new StringBuilder(), (sb, streamChunk) -> sb.append(streamChunk))
                  .map(StringBuilder::toString)
                  .map(this::processLlmResponse)
                  .flatMap(llmResponse -> validateAndRetryIfNeeded(userPrompt, llmResponse))
                  .map(this::parseReviewResult)
                  .doOnNext(result -> logger.debug("Analyse chunk terminée avec streaming"));
            });
  }

  private String processLlmResponse(String llmResponse) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(llmResponse);

      // Format OpenAI avec choices
      if (rootNode.has("choices")
          && rootNode.get("choices").isArray()
          && !rootNode.get("choices").isEmpty()) {
        JsonNode contentNode = rootNode.path("choices").get(0).path("message").path("content");
        llmResponse = contentNode.asText();
      }

      // Nettoyer la réponse
      llmResponse = extractJson(llmResponse);
      llmResponse = cleanToValidJsonChars(llmResponse);

      return llmResponse;
    } catch (Exception e) {
      logger.warn("Erreur lors du traitement de la réponse LLM, utilisation brute", e);
      return llmResponse;
    }
  }

  private Mono<String> validateAndRetryIfNeeded(String userPrompt, String llmResponse) {
    if (!reviewValidator.isValid(PromptBuilder.OUTPUT_SCHEMA_JSON, llmResponse)) {
      logger.warn("Réponse LLM invalide, nouvelle tentative avec instruction stricte");

      String strictPrompt =
          userPrompt + "\n\nIMPORTANT: Return ONLY valid JSON complying with the schema above.";

      return llmClient
          .review(PromptBuilder.SYSTEM_PROMPT, strictPrompt)
          .map(this::processLlmResponse)
          .filter(response -> reviewValidator.isValid(PromptBuilder.OUTPUT_SCHEMA_JSON, response))
          .switchIfEmpty(
              Mono.error(new RuntimeException("Impossible d'obtenir une réponse valide du LLM")));
    }

    return Mono.just(llmResponse);
  }

  private ReviewResult parseReviewResult(String llmResponse) {
    return ReviewResult.fromJson(llmResponse);
  }

  private String cleanToValidJsonChars(String raw) {
    if (raw == null) return null;

    String cleaned = raw.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
    cleaned = cleaned.replaceAll("<\\|.*?\\|>", "");

    return cleaned.trim();
  }

  /**
   * Sauvegarde les artifacts de l'analyse de manière réactive.
   *
   * @param diffBundle Bundle de diff
   * @param reviewResult Résultat de la revue
   * @param config Configuration pour les chemins
   * @return Mono<Void> qui se complète quand la sauvegarde est terminée
   */
  private Mono<Void> saveArtifacts(
      DiffAnalysisBundle diffBundle, ReviewResult reviewResult, ApplicationConfig config) {

    return Mono.fromCallable(
            () -> {
              try {
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

                // Sauvegarder un résumé du prompt
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

                return null;
              } catch (IOException e) {
                throw new RuntimeException("Erreur lors de la sauvegarde des artifacts", e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
  }

  /**
   * Publie les résultats selon le mode configuré de manière réactive.
   *
   * @param config Configuration
   * @param reviewResult Résultat à publier
   * @param diffBundle Bundle pour contexte
   * @return Mono<Void> qui se complète quand la publication est terminée
   */
  private Mono<Void> publishResults(
      ApplicationConfig config, ReviewResult reviewResult, DiffAnalysisBundle diffBundle) {

    if (MODE_GITHUB.equals(config.mode)
        && config.pullRequestNumber > 0
        && reviewPublisher != null) {
      logger.info("Publication des résultats sur GitHub PR #{}", config.pullRequestNumber);
      return Mono.fromCallable(
              () -> {
                try {
                  reviewPublisher.publish(config.pullRequestNumber, reviewResult, diffBundle);
                  logger.info("Résultats publiés avec succès sur GitHub");
                  return null;
                } catch (Exception e) {
                  throw new RuntimeException("Erreur lors de la publication sur GitHub", e);
                }
              })
          .subscribeOn(Schedulers.boundedElastic())
          .then();
    } else {
      return Mono.fromRunnable(
          () -> {
            // Mode local ou pas de PR : afficher en console
            System.out.println("\n" + "=".repeat(80));
            System.out.println("RÉSULTAT DE LA REVUE DE CODE");
            System.out.println("=".repeat(80));
            System.out.println(reviewResult.toPrettyJson());
            System.out.println("=".repeat(80));

            logger.info("Résultats affichés en console");
          });
    }
  }

  /** Configuration complète de l'application. */
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
