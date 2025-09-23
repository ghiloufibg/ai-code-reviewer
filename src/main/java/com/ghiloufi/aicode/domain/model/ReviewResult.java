package com.ghiloufi.aicode.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * Représente le résultat d'une analyse de code contenant un résumé, des issues détectées et des
 * notes non-bloquantes.
 *
 * <p>Cette classe sert de conteneur pour les résultats d'analyse de code et fournit des méthodes
 * utilitaires pour la sérialisation/désérialisation JSON.
 *
 * <p>Les champs null ne sont pas inclus dans la sérialisation JSON grâce à l'annotation
 * {@code @JsonInclude(JsonInclude.Include.NON_NULL)}.
 *
 * @author Ghiloufi
 * @version 1.0
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResult {

  /** Instance réutilisable d'ObjectMapper pour optimiser les performances */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** Résumé textuel de l'analyse effectuée */
  public String summary;

  /** Liste des issues détectées lors de l'analyse */
  public List<Issue> issues = new ArrayList<>();

  /** Liste des notes informatives non-bloquantes */
  public List<Note> non_blocking_notes = new ArrayList<>();

  /**
   * Crée une instance de ReviewResult à partir d'une chaîne JSON.
   *
   * <p>Cette méthode tente de parser le JSON fourni. En cas d'échec, elle essaie d'extraire un
   * sous-ensemble JSON valide en recherchant les premières accolades ouvrante et fermante.
   *
   * @param json Chaîne JSON à désérialiser
   * @return Une nouvelle instance de ReviewResult
   * @throws RuntimeException si le parsing JSON échoue définitivement
   * @throws IllegalArgumentException si json est null ou vide
   * @see #toJson()
   * @see #toPrettyJson()
   */
  public static ReviewResult fromJson(String json) {
    if (json == null || json.trim().isEmpty()) {
      throw new IllegalArgumentException("La chaîne JSON ne peut pas être null ou vide");
    }

    try {
      return OBJECT_MAPPER.readValue(json, ReviewResult.class);
    } catch (Exception e) {
      return tryExtractAndParseJsonSubset(json, e);
    }
  }

  /**
   * Tente d'extraire et de parser un sous-ensemble JSON valide.
   *
   * @param json Chaîne JSON originale
   * @param originalException Exception du premier tentative de parsing
   * @return ReviewResult parsé à partir du sous-ensemble
   * @throws RuntimeException si l'extraction et le parsing échouent
   */
  private static ReviewResult tryExtractAndParseJsonSubset(
      String json, Exception originalException) {
    int openBrace = json.indexOf('{');
    int closeBrace = json.lastIndexOf('}');

    if (openBrace >= 0 && closeBrace > openBrace) {
      String extractedJson = json.substring(openBrace, closeBrace + 1);
      try {
        return OBJECT_MAPPER.readValue(extractedJson, ReviewResult.class);
      } catch (Exception extractionException) {
        throw new RuntimeException(
            "Échec du parsing JSON même après extraction: " + extractionException.getMessage(),
            extractionException);
      }
    }

    throw new RuntimeException(
        "Impossible de parser le JSON fourni: " + originalException.getMessage(),
        originalException);
  }

  /**
   * Convertit cette instance en chaîne JSON compacte.
   *
   * @return Représentation JSON compacte de cet objet
   * @throws RuntimeException si la sérialisation JSON échoue
   * @see #toPrettyJson()
   * @see #fromJson(String)
   */
  public String toJson() {
    try {
      return OBJECT_MAPPER.writeValueAsString(this);
    } catch (Exception e) {
      throw new RuntimeException("Erreur lors de la sérialisation JSON: " + e.getMessage(), e);
    }
  }

  /**
   * Convertit cette instance en chaîne JSON formatée avec indentation.
   *
   * @return Représentation JSON formatée et indentée de cet objet
   * @throws RuntimeException si la sérialisation JSON échoue
   * @see #toJson()
   * @see #fromJson(String)
   */
  public String toPrettyJson() {
    try {
      return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (Exception e) {
      throw new RuntimeException(
          "Erreur lors de la sérialisation JSON formatée: " + e.getMessage(), e);
    }
  }

  /**
   * Calcule le nombre total d'éléments dans ce résultat d'analyse.
   *
   * @return La somme du nombre d'issues et de notes
   */
  public int getTotalItemCount() {
    int issuesCount = issues != null ? issues.size() : 0;
    int notesCount = non_blocking_notes != null ? non_blocking_notes.size() : 0;
    return issuesCount + notesCount;
  }

  /**
   * Vérifie si ce résultat contient des éléments significatifs.
   *
   * @return true si le résultat contient au moins une issue, une note ou un résumé
   */
  public boolean hasContent() {
    boolean hasIssues = issues != null && !issues.isEmpty();
    boolean hasNotes = non_blocking_notes != null && !non_blocking_notes.isEmpty();
    boolean hasSummary = summary != null && !summary.trim().isEmpty();

    return hasIssues || hasNotes || hasSummary;
  }

  /**
   * Représente une issue détectée lors de l'analyse de code.
   *
   * <p>Une issue contient des informations sur la localisation du problème, sa gravité, et des
   * suggestions de correction.
   */
  public static class Issue {
    /** Chemin du fichier concerné par l'issue */
    public String file;

    /** Ligne de début de l'issue (inclusive) */
    public int start_line;

    /** Ligne de fin de l'issue (inclusive) */
    public int end_line;

    /** Niveau de gravité de l'issue (ex: ERROR, WARNING, INFO) */
    public String severity;

    /** Identifiant unique de la règle violée */
    public String rule_id;

    /** Titre descriptif de l'issue */
    public String title;

    /** Explication détaillée de pourquoi c'est un problème */
    public String rationale;

    /** Suggestion de correction pour résoudre l'issue */
    public String suggestion;

    /** Liste de références externes (documentation, liens) */
    public List<String> references;

    /** Index du hunk de diff concerné (pour les analyses de PR) */
    public int hunk_index;

    /**
     * Vérifie si cette issue couvre plusieurs lignes.
     *
     * @return true si l'issue s'étend sur plusieurs lignes, false sinon
     */
    public boolean isMultiLine() {
      return end_line > start_line;
    }

    /**
     * Calcule le nombre de lignes concernées par cette issue.
     *
     * @return Le nombre de lignes couvertes par l'issue (minimum 1)
     */
    public int getLineCount() {
      return Math.max(1, end_line - start_line + 1);
    }
  }

  /**
   * Représente une note informative non-bloquante.
   *
   * <p>Les notes servent à signaler des observations ou suggestions qui n'empêchent pas la
   * validation du code.
   */
  public static class Note {
    /** Chemin du fichier concerné par la note */
    public String file;

    /** Numéro de ligne concernée par la note */
    public int line;

    /** Contenu textuel de la note */
    public String note;

    /**
     * Vérifie si cette note a un contenu significatif.
     *
     * @return true si la note contient du texte non vide, false sinon
     */
    public boolean hasContent() {
      return note != null && !note.trim().isEmpty();
    }
  }
}
