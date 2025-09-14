package com.ghiloufi.aicode.sast;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Collecteur de résultats d'analyse statique pour le plugin AI Code Reviewer.
 *
 * <p>Cette classe est responsable de la collecte et de l'agrégation des résultats provenant de
 * différents outils d'analyse statique de code Java. Les résultats sont ensuite formatés pour être
 * transmis au LLM pour une analyse contextuelle lors de la revue de code.
 *
 * <p><strong>Outils supportés :</strong>
 *
 * <ul>
 *   <li><b>Checkstyle</b> : Vérification des standards de codage et conventions
 *   <li><b>PMD</b> : Détection de code problématique et anti-patterns
 *   <li><b>SpotBugs</b> : Analyse de bytecode pour détecter les bugs potentiels
 *   <li><b>Semgrep</b> : Analyse sémantique et détection de patterns de sécurité
 * </ul>
 *
 * <p><strong>Structure des fichiers :</strong> Les fichiers de résultats doivent être présents dans
 * le répertoire {@code target/} :
 *
 * <ul>
 *   <li>{@code target/checkstyle-result.xml} - Rapport Checkstyle
 *   <li>{@code target/pmd.xml} - Rapport PMD
 *   <li>{@code target/spotbugs.xml} - Rapport SpotBugs
 *   <li>{@code target/semgrep.json} - Rapport Semgrep
 * </ul>
 *
 * <p><strong>Traitement des données :</strong>
 *
 * <ul>
 *   <li>Fichiers JSON : Parsés et retournés comme objets Java
 *   <li>Fichiers XML : Retournés comme chaînes (tronqués à 200,000 caractères)
 *   <li>Fichiers manquants : Retournés comme chaînes vides
 * </ul>
 *
 * <p><strong>Exemple d'utilisation :</strong>
 *
 * <pre>{@code
 * StaticAnalysisRunner runner = new StaticAnalysisRunner();
 * Map<String, Object> results = runner.runAndCollect();
 *
 * // Utiliser avec PromptBuilder
 * String userMessage = promptBuilder.buildUserMessage(
 *     repoName,
 *     branch,
 *     javaVersion,
 *     buildSystem,
 *     diff,
 *     results,  // Les résultats d'analyse statique
 *     projectConfig,
 *     testStatus
 * );
 * }</pre>
 *
 * @version 1.0
 * @since 1.0
 * @see com.ghiloufi.aicode.llm.PromptBuilder
 */
@Service
public class StaticAnalysisRunner {

  private static final Logger logger = LoggerFactory.getLogger(StaticAnalysisRunner.class);

  // Configuration des chemins et limites
  private static final String DEFAULT_TARGET_DIR = "target";
  private static final int MAX_XML_CONTENT_LENGTH = 200_000;

  // Noms des fichiers de résultats
  private static final String CHECKSTYLE_FILE = "checkstyle-result.xml";
  private static final String PMD_FILE = "pmd.xml";
  private static final String SPOTBUGS_FILE = "spotbugs.xml";
  private static final String SEMGREP_FILE = "semgrep.json";

  // Clés pour la map de résultats
  private static final String KEY_CHECKSTYLE = "checkstyle";
  private static final String KEY_PMD = "pmd";
  private static final String KEY_SPOTBUGS = "spotbugs";
  private static final String KEY_SEMGREP = "semgrep";

  // Extensions de fichiers
  private static final String JSON_EXTENSION = ".json";

  private final Path targetDirectory;
  private final ObjectMapper objectMapper;
  private final int maxContentLength;

  /**
   * Construit un nouveau StaticAnalysisRunner avec configuration par défaut.
   *
   * <p>Utilise le répertoire "target" comme source des fichiers de résultats et limite le contenu
   * XML à 200,000 caractères.
   */
  public StaticAnalysisRunner() {
    this(Path.of(DEFAULT_TARGET_DIR), MAX_XML_CONTENT_LENGTH);
  }

  /**
   * Construit un nouveau StaticAnalysisRunner avec un répertoire personnalisé.
   *
   * @param targetDirectory Le répertoire contenant les fichiers de résultats
   * @throws IllegalArgumentException si targetDirectory est null
   */
  public StaticAnalysisRunner(Path targetDirectory) {
    this(targetDirectory, MAX_XML_CONTENT_LENGTH);
  }

  /**
   * Construit un nouveau StaticAnalysisRunner avec configuration complète.
   *
   * @param targetDirectory Le répertoire contenant les fichiers de résultats
   * @param maxContentLength La taille maximale du contenu XML à conserver
   * @throws IllegalArgumentException si les paramètres sont invalides
   */
  public StaticAnalysisRunner(Path targetDirectory, int maxContentLength) {
    validateConstructorParameters(targetDirectory, maxContentLength);

    this.targetDirectory = targetDirectory;
    this.maxContentLength = maxContentLength;
    this.objectMapper = new ObjectMapper();

    logger.info(
        "StaticAnalysisRunner initialisé - Répertoire: {}, Limite: {} caractères",
        targetDirectory,
        maxContentLength);
  }

  /**
   * Exécute la collecte des résultats d'analyse statique.
   *
   * <p>Cette méthode parcourt le répertoire cible et collecte les résultats de tous les outils
   * d'analyse statique configurés. Les résultats sont retournés dans une Map avec les clés
   * suivantes :
   *
   * <ul>
   *   <li>"checkstyle" : Résultats Checkstyle (XML tronqué)
   *   <li>"pmd" : Résultats PMD (XML tronqué)
   *   <li>"spotbugs" : Résultats SpotBugs (XML tronqué)
   *   <li>"semgrep" : Résultats Semgrep (objet JSON parsé)
   * </ul>
   *
   * <p><strong>Comportement :</strong>
   *
   * <ul>
   *   <li>Si un fichier n'existe pas, une chaîne vide est retournée
   *   <li>Les fichiers JSON sont parsés en objets Java
   *   <li>Les fichiers XML sont tronqués à maxContentLength caractères
   *   <li>Les erreurs de lecture sont loggées mais n'interrompent pas le processus
   * </ul>
   *
   * @return Une Map contenant les résultats de chaque outil d'analyse
   * @throws StaticAnalysisException si une erreur critique survient lors de la collecte
   */
  public Map<String, Object> runAndCollect() {
    logger.debug(
        "Début de la collecte des résultats d'analyse statique depuis: {}", targetDirectory);

    try {
      Map<String, Object> results = new HashMap<>();

      // Collecter les résultats de chaque outil
      results.put(KEY_CHECKSTYLE, collectToolResult(CHECKSTYLE_FILE, KEY_CHECKSTYLE));
      results.put(KEY_PMD, collectToolResult(PMD_FILE, KEY_PMD));
      results.put(KEY_SPOTBUGS, collectToolResult(SPOTBUGS_FILE, KEY_SPOTBUGS));
      results.put(KEY_SEMGREP, collectToolResult(SEMGREP_FILE, KEY_SEMGREP));

      logCollectionSummary(results);

      return results;

    } catch (Exception e) {
      String errorMessage = "Erreur inattendue lors de la collecte des résultats d'analyse";
      logger.error(errorMessage, e);
      throw new StaticAnalysisException(errorMessage, e);
    }
  }

  /**
   * Collecte le résultat d'un outil spécifique.
   *
   * @param fileName Le nom du fichier de résultat
   * @param toolName Le nom de l'outil (pour le logging)
   * @return Le contenu du fichier ou une chaîne vide si absent
   */
  private Object collectToolResult(String fileName, String toolName) {
    Path filePath = targetDirectory.resolve(fileName);
    Object result = readIfExists(filePath);

    if (result instanceof String && ((String) result).isEmpty()) {
      logger.trace("Pas de résultats {} trouvés à: {}", toolName, filePath);
    } else {
      logger.trace("Résultats {} collectés depuis: {}", toolName, filePath);
    }

    return result;
  }

  /**
   * Lit le contenu d'un fichier s'il existe.
   *
   * <p>Cette méthode gère différents formats de fichiers :
   *
   * <ul>
   *   <li>Fichiers JSON : Parsés et retournés comme objets
   *   <li>Fichiers XML/texte : Retournés comme chaînes (tronqués si nécessaire)
   *   <li>Fichiers manquants : Retourne une chaîne vide
   * </ul>
   *
   * @param filePath Le chemin du fichier à lire
   * @return Le contenu du fichier (objet pour JSON, chaîne pour autres) ou chaîne vide
   */
  private Object readIfExists(Path filePath) {
    try {
      if (!Files.exists(filePath)) {
        logger.trace("Fichier non trouvé: {}", filePath);
        return "";
      }

      if (!Files.isRegularFile(filePath)) {
        logger.warn("Le chemin n'est pas un fichier régulier: {}", filePath);
        return "";
      }

      if (!Files.isReadable(filePath)) {
        logger.warn("Fichier non lisible: {}", filePath);
        return "";
      }

      String content = Files.readString(filePath, StandardCharsets.UTF_8);
      logger.trace("Fichier lu: {}, taille: {} caractères", filePath, content.length());

      // Traitement spécifique pour les fichiers JSON
      if (isJsonFile(filePath)) {
        return parseJsonContent(content, filePath);
      }

      // Pour les fichiers non-JSON (XML, etc.), tronquer si nécessaire
      return truncateContent(content, filePath);

    } catch (IOException e) {
      logger.error("Erreur lors de la lecture du fichier: {}", filePath, e);
      throw new StaticAnalysisException("Erreur lors de la lecture du fichier: " + filePath, e);
    } catch (Exception e) {
      logger.error("Erreur inattendue lors du traitement du fichier: {}", filePath, e);
      throw new StaticAnalysisException("Erreur lors du traitement du fichier: " + filePath, e);
    }
  }

  /**
   * Vérifie si un fichier est un fichier JSON basé sur son extension.
   *
   * @param filePath Le chemin du fichier
   * @return true si le fichier a une extension .json
   */
  private boolean isJsonFile(Path filePath) {
    return filePath.toString().toLowerCase().endsWith(JSON_EXTENSION);
  }

  /**
   * Parse le contenu JSON en objet Java.
   *
   * @param content Le contenu JSON à parser
   * @param filePath Le chemin du fichier (pour le logging)
   * @return L'objet Java représentant le JSON
   */
  private Object parseJsonContent(String content, Path filePath) {
    try {
      Object jsonObject = objectMapper.readValue(content, Object.class);
      logger.debug("JSON parsé avec succès depuis: {}", filePath);
      return jsonObject;
    } catch (IOException e) {
      logger.error("Erreur lors du parsing JSON du fichier: {}", filePath, e);
      // En cas d'erreur de parsing, retourner le contenu brut tronqué
      return truncateContent(content, filePath);
    }
  }

  /**
   * Tronque le contenu si nécessaire.
   *
   * @param content Le contenu à tronquer
   * @param filePath Le chemin du fichier (pour le logging)
   * @return Le contenu tronqué si nécessaire
   */
  private String truncateContent(String content, Path filePath) {
    if (content.length() <= maxContentLength) {
      return content;
    }

    logger.debug(
        "Contenu tronqué pour {}: {} -> {} caractères",
        filePath.getFileName(),
        content.length(),
        maxContentLength);

    return content.substring(0, Math.min(content.length(), maxContentLength));
  }

  /**
   * Enregistre un résumé de la collecte dans les logs.
   *
   * @param results Les résultats collectés
   */
  private void logCollectionSummary(Map<String, Object> results) {
    int toolsWithResults = 0;
    for (Map.Entry<String, Object> entry : results.entrySet()) {
      Object value = entry.getValue();
      if (value != null && !(value instanceof String && ((String) value).isEmpty())) {
        toolsWithResults++;
      }
    }

    logger.info(
        "Collecte terminée: {}/{} outils ont des résultats", toolsWithResults, results.size());
  }

  /** Valide les paramètres du constructeur. */
  private void validateConstructorParameters(Path targetDirectory, int maxContentLength) {
    if (targetDirectory == null) {
      throw new IllegalArgumentException("Le répertoire cible ne peut pas être null");
    }
    if (maxContentLength <= 0) {
      throw new IllegalArgumentException(
          "La taille maximale du contenu doit être positive, reçu: " + maxContentLength);
    }
  }

  /**
   * Retourne le répertoire cible configuré.
   *
   * @return Le chemin du répertoire cible
   */
  public Path getTargetDirectory() {
    return targetDirectory;
  }

  /**
   * Retourne la limite de taille configurée.
   *
   * @return La taille maximale du contenu XML
   */
  public int getMaxContentLength() {
    return maxContentLength;
  }

  /**
   * Vérifie si le répertoire cible existe et est accessible.
   *
   * @return true si le répertoire existe et est lisible
   */
  public boolean isTargetDirectoryAccessible() {
    return Files.exists(targetDirectory)
        && Files.isDirectory(targetDirectory)
        && Files.isReadable(targetDirectory);
  }

  /**
   * Liste les fichiers d'analyse disponibles dans le répertoire cible.
   *
   * @return Une Map indiquant la présence de chaque fichier d'analyse
   */
  public Map<String, Boolean> getAvailableAnalysisFiles() {
    Map<String, Boolean> availability = new HashMap<>();

    availability.put(KEY_CHECKSTYLE, Files.exists(targetDirectory.resolve(CHECKSTYLE_FILE)));
    availability.put(KEY_PMD, Files.exists(targetDirectory.resolve(PMD_FILE)));
    availability.put(KEY_SPOTBUGS, Files.exists(targetDirectory.resolve(SPOTBUGS_FILE)));
    availability.put(KEY_SEMGREP, Files.exists(targetDirectory.resolve(SEMGREP_FILE)));

    return availability;
  }
}
