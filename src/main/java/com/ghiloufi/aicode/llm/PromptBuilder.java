package com.ghiloufi.aicode.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructeur de prompts pour l'assistant de revue de code IA.
 *
 * <p>Cette classe est responsable de la construction des prompts système et utilisateur qui seront
 * envoyés au modèle de langage (LLM) pour effectuer des revues de code automatisées sur des Pull
 * Requests Java.
 *
 * <p><strong>Architecture du prompt :</strong>
 *
 * <ul>
 *   <li>Prompt système : Définit le rôle, les principes et les politiques de revue
 *   <li>Prompt utilisateur : Contient le contexte du projet et le diff à analyser
 *   <li>Schéma de sortie : Format JSON strict pour les résultats
 * </ul>
 *
 * <p><strong>Catégories de sévérité :</strong>
 *
 * <ul>
 *   <li><b>critical</b> : Bugs de sécurité exploitables, perte de données, crash
 *   <li><b>major</b> : Bugs visibles par l'utilisateur, risques de correctness
 *   <li><b>minor</b> : Petits défauts ou patterns risqués
 *   <li><b>info</b> : Suggestions ou clarifications
 * </ul>
 *
 * <p><strong>Catégories d'issues :</strong>
 *
 * <ul>
 *   <li>Correctness : Erreurs logiques et bugs fonctionnels
 *   <li>Security : Vulnérabilités et failles de sécurité
 *   <li>Concurrency : Problèmes de thread-safety et synchronisation
 *   <li>Performance : Problèmes de performance et optimisation
 *   <li>Maintainability : Qualité du code et maintenabilité
 *   <li>Test gaps : Manques de tests et couverture insuffisante
 * </ul>
 *
 * <p><strong>Exemple d'utilisation :</strong>
 *
 * <pre>{@code
 * PromptBuilder builder = new PromptBuilder();
 *
 * String userMessage = builder.buildUserMessage(
 *     "myproject/backend",
 *     "main",
 *     "17",
 *     "maven",
 *     diffContent,
 *     staticAnalysisResults,
 *     projectConfig,
 *     testStatus
 * );
 *
 * // Utiliser avec LlmClient
 * String review = llmClient.review(
 *     PromptBuilder.SYSTEM_PROMPT,
 *     userMessage
 * );
 * }</pre>
 *
 * @version 1.0
 * @since 1.0
 * @see LlmClient
 */
public class PromptBuilder {

  /**
   * Prompt système définissant le rôle et les règles de l'assistant de revue de code.
   *
   * <p>Ce prompt configure le LLM comme un assistant spécialisé dans la revue de code Java avec des
   * principes stricts :
   *
   * <ul>
   *   <li>Focus sur les lignes modifiées et leur contexte (±5 lignes)
   *   <li>Préférence pour des corrections minimales et sûres
   *   <li>Classification par sévérité et catégorie
   *   <li>Références aux standards de sécurité (CWE/OWASP)
   * </ul>
   *
   * <p><strong>Politiques de revue incluses :</strong>
   *
   * <ul>
   *   <li>Gestion des ressources avec try-with-resources
   *   <li>Validation des entrées non fiables
   *   <li>Utilisation de PreparedStatement pour SQL
   *   <li>Thread-safety et immutabilité
   *   <li>Protection des données sensibles
   * </ul>
   */
  public static final String SYSTEM_PROMPT =
      """
            You are a rigorous code review assistant specialized in Java. Review ONLY the changes in this Pull Request and output STRICT JSON.
            Principles:
            - Comment only on changed lines and ±5 lines of context.
            - Prefer minimal, safe patches; avoid restyling unless it prevents defects.
            - If uncertain, use severity=info and explain briefly.
            - Never invent files or lines; ground all comments in provided diff.
            - Classify findings: critical  major  minor  info.
            - Categories: correctness, security, concurrency, performance, maintainability, test gaps.
            - Cite references (CWE/OWASP) when applicable.
            [Policy]
            - Use try-with-resources for IO/DB; avoid resource leaks.
            - Validate untrusted input; never build SQL with string concatenation (use PreparedStatement).
            - Avoid catching generic Exception without context.
            - Avoid mutable static state; ensure thread-safety.
            - Use equals() not '==' for objects; keep equals/hashCode consistent.
            - Don't log secrets/PII.
            - In Spring, validate request inputs; set safe defaults.
            - Prefer immutable DTOs or defensive copies.
            - Severity guidance:
              - critical: exploitable security bug, data loss, crash under normal use
              - major: likely user-visible bug or strong correctness risk
              - minor: small defect or risky pattern
              - info: suggestion/clarification
            """;
  /**
   * Schéma JSON définissant le format de sortie attendu du LLM.
   *
   * <p>Structure de la réponse :
   *
   * <ul>
   *   <li><b>summary</b> : Résumé en 1-3 phrases des findings
   *   <li><b>issues</b> : Liste des problèmes identifiés avec :
   *       <ul>
   *         <li>file : Chemin du fichier
   *         <li>start_line/end_line : Lignes concernées
   *         <li>severity : Niveau de sévérité
   *         <li>rule_id : Identifiant de la règle (format: JAVA.CATEGORY.CODE)
   *         <li>title : Titre court du problème
   *         <li>rationale : Explication détaillée
   *         <li>suggestion : Correction proposée
   *         <li>references : Références aux standards
   *         <li>hunk_index : Index du hunk dans le diff
   *       </ul>
   *   <li><b>non_blocking_notes</b> : Notes informatives non bloquantes
   * </ul>
   */
  public static final String OUTPUT_SCHEMA_JSON =
      """
            { "summary":"string (1-3 sentences)", "issues":[ { "file":"string", "start_line":"int", "end_line":"int", "severity":"critical
            major
            minor
            info", "rule_id":"JAVA.<CATEGORY>.<CODE>", "title":"string", "rationale":"string", "suggestion":"string", "references":["string"], "hunk_index":"int" } ], "non_blocking_notes":[{"file":"string","line":"int","note":"string"}] }
            """;
  private static final Logger logger = LoggerFactory.getLogger(PromptBuilder.class);
  // Sections du template de message utilisateur
  private static final String SECTION_REPO = "[REPO]";
  private static final String SECTION_REPO_END = "[/REPO]";
  private static final String SECTION_DIFF = "[DIFF_UNIFIED]";
  private static final String SECTION_DIFF_END = "[/DIFF_UNIFIED]";
  private static final String SECTION_STATIC_ANALYSIS = "[STATIC_ANALYSIS]";
  private static final String SECTION_STATIC_ANALYSIS_END = "[/STATIC_ANALYSIS]";
  private static final String SECTION_PROJECT_CONFIG = "[PROJECT_CONFIG]";
  private static final String SECTION_PROJECT_CONFIG_END = "[/PROJECT_CONFIG]";
  private static final String SECTION_TEST_STATUS = "[TEST_STATUS]";
  private static final String SECTION_TEST_STATUS_END = "[/TEST_STATUS]";

  private final ObjectMapper objectMapper;
  private final ObjectWriter prettyWriter;

  /**
   * Construit un nouveau PromptBuilder avec configuration par défaut.
   *
   * <p>Initialise l'ObjectMapper pour la sérialisation JSON avec pretty printing activé pour une
   * meilleure lisibilité des données dans les prompts.
   */
  public PromptBuilder() {
    this.objectMapper = new ObjectMapper();
    this.prettyWriter = objectMapper.writerWithDefaultPrettyPrinter();
    logger.debug("PromptBuilder initialisé");
  }

  /**
   * Construit un nouveau PromptBuilder avec un ObjectMapper personnalisé.
   *
   * <p>Permet d'utiliser une configuration Jackson spécifique si nécessaire.
   *
   * @param objectMapper L'ObjectMapper à utiliser pour la sérialisation JSON
   * @throws IllegalArgumentException si objectMapper est null
   */
  public PromptBuilder(ObjectMapper objectMapper) {
    Objects.requireNonNull(objectMapper, "ObjectMapper ne peut pas être null");
    this.objectMapper = objectMapper;
    this.prettyWriter = objectMapper.writerWithDefaultPrettyPrinter();
    logger.debug("PromptBuilder initialisé avec ObjectMapper personnalisé");
  }

  /**
   * Construit le message utilisateur complet pour la revue de code.
   *
   * <p>Ce message contient toutes les informations contextuelles nécessaires au LLM pour effectuer
   * une revue de code pertinente :
   *
   * <ul>
   *   <li>Métadonnées du repository (nom, branche, version Java, build)
   *   <li>Le diff unifié des modifications
   *   <li>Résultats de l'analyse statique
   *   <li>Configuration du projet
   *   <li>Statut des tests
   *   <li>Un exemple few-shot pour guider le LLM
   * </ul>
   *
   * <p><strong>Format de sortie :</strong> Le message est structuré avec des balises de section
   * pour faciliter le parsing par le LLM.
   *
   * @param repoName Le nom du repository (format: owner/repo)
   * @param defaultBranch La branche par défaut du repository
   * @param javaVersion La version de Java utilisée dans le projet
   * @param buildSystem Le système de build (maven, gradle, etc.)
   * @param unifiedDiff Le diff unifié Git des modifications
   * @param staticAnalysis Les résultats de l'analyse statique (peut être null)
   * @param projectConfig La configuration du projet (peut être null)
   * @param testStatus Le statut des tests (peut être null)
   * @return Le message utilisateur formaté pour le LLM
   * @throws PromptBuilderException si une erreur survient lors de la construction
   * @throws IllegalArgumentException si les paramètres requis sont invalides
   */
  public String buildUserMessage(
      String repoName,
      String defaultBranch,
      String javaVersion,
      String buildSystem,
      String unifiedDiff,
      Map<String, Object> staticAnalysis,
      Map<String, Object> projectConfig,
      Map<String, Object> testStatus) {

    // Validation des paramètres requis
    validateRequiredParameters(repoName, defaultBranch, javaVersion, buildSystem, unifiedDiff);

    logger.debug("Construction du message utilisateur pour le repo: {}", repoName);

    try {
      // Sérialisation des données optionnelles
      String staticAnalysisJson = serializeToJson(staticAnalysis, "analyse statique");
      String projectConfigJson = serializeToJson(projectConfig, "configuration projet");
      String testStatusJson = serializeToJson(testStatus, "statut des tests");

      // Construction du message avec le template
      String userMessage =
          buildMessageFromTemplate(
              repoName,
              defaultBranch,
              javaVersion,
              buildSystem,
              unifiedDiff,
              staticAnalysisJson,
              projectConfigJson,
              testStatusJson);

      logger.debug(
          "Message utilisateur construit avec succès, taille: {} caractères", userMessage.length());

      return userMessage;

    } catch (JsonProcessingException e) {
      String errorMessage = "Erreur lors de la sérialisation JSON des données";
      logger.error(errorMessage, e);
      throw new PromptBuilderException(errorMessage, e);
    } catch (Exception e) {
      String errorMessage = "Erreur inattendue lors de la construction du prompt";
      logger.error(errorMessage, e);
      throw new PromptBuilderException(errorMessage, e);
    }
  }

  /** Valide les paramètres requis pour la construction du prompt. */
  private void validateRequiredParameters(
      String repoName,
      String defaultBranch,
      String javaVersion,
      String buildSystem,
      String unifiedDiff) {

    if (repoName == null || repoName.isBlank()) {
      throw new IllegalArgumentException("Le nom du repository ne peut pas être null ou vide");
    }
    if (defaultBranch == null || defaultBranch.isBlank()) {
      throw new IllegalArgumentException("La branche par défaut ne peut pas être null ou vide");
    }
    if (javaVersion == null || javaVersion.isBlank()) {
      throw new IllegalArgumentException("La version Java ne peut pas être null ou vide");
    }
    if (buildSystem == null || buildSystem.isBlank()) {
      throw new IllegalArgumentException("Le système de build ne peut pas être null ou vide");
    }
    if (unifiedDiff == null || unifiedDiff.isBlank()) {
      throw new IllegalArgumentException("Le diff unifié ne peut pas être null ou vide");
    }
  }

  /**
   * Sérialise un objet en JSON avec pretty printing.
   *
   * @param data L'objet à sérialiser (peut être null)
   * @param dataName Le nom des données pour le logging
   * @return Le JSON formaté ou une chaîne vide si data est null
   */
  private String serializeToJson(Map<String, Object> data, String dataName)
      throws JsonProcessingException {
    if (data == null || data.isEmpty()) {
      logger.trace("Pas de données pour: {}", dataName);
      return "";
    }

    String json = prettyWriter.writeValueAsString(data);
    logger.trace("{} sérialisée, taille: {} caractères", dataName, json.length());
    return json;
  }

  /** Construit le message final à partir du template. */
  private String buildMessageFromTemplate(
      String repoName,
      String defaultBranch,
      String javaVersion,
      String buildSystem,
      String unifiedDiff,
      String staticAnalysisJson,
      String projectConfigJson,
      String testStatusJson) {

    return """
            [REPO]
            name: %s
            default_branch: %s
            java_version: %s
            build_system: %s
            [/REPO]
            [DIFF_UNIFIED]
            %s
            [/DIFF_UNIFIED]
            [STATIC_ANALYSIS]
            %s
            [/STATIC_ANALYSIS]
            [PROJECT_CONFIG]
            %s
            [/PROJECT_CONFIG]
            [TEST_STATUS]
            %s
            [/TEST_STATUS]
            [Few-shot Example]
            [EXAMPLE_DIFF]
            --- a/src/Foo.java
            +++ b/src/Foo.java
            @@ -10,6 +10,10 @@
            + Connection c = DriverManager.getConnection(url+user+pass);
            + Statement s = c.createStatement();
            + ResultSet rs = s.executeQuery("SELECT * FROM users WHERE name = '" + input + "'");
            + // no close
            [/EXAMPLE_DIFF]
            [EXAMPLE_OUTPUT]
            {
             "summary": "High risk SQL injection and resource leak introduced.",
             "issues": [
              {
               "file": "src/Foo.java",
               "start_line": 11,
               "end_line": 14,
               "severity": "critical",
               "rule_id": "JAVA.SEC.SQL_INJECTION",
               "title": "SQL injection via string concatenation",
               "rationale": "User-supplied 'input' is concatenated into SQL.",
               "suggestion": "Use PreparedStatement with placeholders and try-with-resources.",
               "references": ["CWE-89","OWASP A03: Injection"],
               "hunk_index": 0
              },
              {
               "file": "src/Foo.java",
               "start_line": 11,
               "end_line": 14,
               "severity": "major",
               "rule_id": "JAVA.RES.LEAK",
               "title": "Missing resource closing",
               "rationale": "Connection/Statement/ResultSet not closed.",
               "suggestion": "Wrap in try-with-resources.",
               "references": ["CWE-772"],
               "hunk_index": 0
              }
             ],
             "non_blocking_notes": []
            }
            [/EXAMPLE_OUTPUT]
            """
        .formatted(
            repoName,
            defaultBranch,
            javaVersion,
            buildSystem,
            unifiedDiff,
            staticAnalysisJson,
            projectConfigJson,
            testStatusJson);
  }

  /**
   * Retourne le prompt système configuré.
   *
   * @return Le prompt système
   */
  public String getSystemPrompt() {
    return SYSTEM_PROMPT;
  }

  /**
   * Retourne le schéma JSON de sortie.
   *
   * @return Le schéma JSON
   */
  public String getOutputSchema() {
    return OUTPUT_SCHEMA_JSON;
  }

  /**
   * Exception spécifique aux erreurs de construction de prompt.
   *
   * <p>Cette exception encapsule toutes les erreurs qui peuvent survenir lors de la construction
   * des prompts, notamment les erreurs de sérialisation JSON et de validation des paramètres.
   */
  public static class PromptBuilderException extends RuntimeException {

    /**
     * Construit une nouvelle exception avec un message.
     *
     * @param message Le message d'erreur
     */
    public PromptBuilderException(String message) {
      super(message);
    }

    /**
     * Construit une nouvelle exception avec un message et une cause.
     *
     * @param message Le message d'erreur
     * @param cause L'exception originale
     */
    public PromptBuilderException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
