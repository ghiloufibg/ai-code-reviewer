package com.ghiloufi.aicode.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Conteneur qui associe un diff Git brut (texte) avec sa représentation structurée pour faciliter
 * l'analyse de code et le traitement en aval.
 *
 * <p>Cette classe encapsule à la fois :
 *
 * <ul>
 *   <li>La version structurée du diff ({@link UnifiedDiff}) pour l'analyse programmatique
 *   <li>La version textuelle brute du diff Git pour l'affichage ou l'archivage
 * </ul>
 *
 * <p>Le bundle permet de diviser un diff volumineux en chunks plus petits pour optimiser les
 * traitements par batch ou respecter des limites de taille.
 *
 * @param structuredDiff La représentation structurée du diff, analysée et parsée
 * @param rawDiffText Le texte brut du diff Git tel qu'il sort de la commande git
 * @author Ghiloufi
 * @version 1.0
 * @since 1.0
 * @see UnifiedDiff
 * @see FileDiff
 */
public record DiffAnalysisBundle(UnifiedDiff structuredDiff, String rawDiffText) {

  /** Nombre maximum de lignes par défaut pour les chunks lors de la division */
  private static final int DEFAULT_MAX_LINES_PER_CHUNK = 1000;

  /**
   * Constructeur compact qui valide les paramètres du record.
   *
   * @throws IllegalArgumentException si structuredDiff est null
   * @throws IllegalArgumentException si rawDiffText est null ou vide
   */
  public DiffAnalysisBundle {
    Objects.requireNonNull(structuredDiff, "Le diff structuré ne peut pas être null");
    Objects.requireNonNull(rawDiffText, "Le texte brut du diff ne peut pas être null");

    if (rawDiffText.trim().isEmpty()) {
      throw new IllegalArgumentException("Le texte brut du diff ne peut pas être vide");
    }
  }

  /**
   * Retourne la configuration du projet associée à ce diff.
   *
   * <p>Pour l'instant, retourne une Map vide. Cette méthode est prévue pour être étendue avec des
   * informations de configuration spécifiques au projet analysé (règles de style, exclusions,
   * etc.).
   *
   * @return Une Map contenant la configuration du projet (actuellement vide)
   * @apiNote Cette méthode est destinée à évoluer dans les versions futures
   */
  public Map<String, Object> getProjectConfiguration() {
    return Map.of();
  }

  /**
   * Retourne le statut des tests associés à ce diff.
   *
   * <p>Pour l'instant, retourne une Map vide. Cette méthode est prévue pour être étendue avec des
   * informations sur l'état des tests (succès, échecs, couverture, etc.).
   *
   * @return Une Map contenant le statut des tests (actuellement vide)
   * @apiNote Cette méthode est destinée à évoluer dans les versions futures
   */
  public Map<String, Object> getTestStatus() {
    return Map.of();
  }

  /**
   * Divise le diff en chunks plus petits respectant une limite de lignes.
   *
   * <p>Cette méthode est utile pour :
   *
   * <ul>
   *   <li>Traiter de gros diffs par petits morceaux
   *   <li>Respecter les limites d'API externes
   *   <li>Optimiser l'utilisation mémoire lors du traitement
   *   <li>Paralléliser l'analyse de code
   * </ul>
   *
   * <p>La division se fait par hunk complet : un hunk n'est jamais divisé au milieu. Si un hunk
   * dépasse la limite, il sera placé seul dans un chunk.
   *
   * @param maxLinesPerChunk Nombre maximum de lignes par chunk. Doit être positif.
   * @return Liste des diffs divisés, chacun respectant la limite de lignes. Retourne une liste vide
   *     si le diff original est vide.
   * @throws IllegalArgumentException si maxLinesPerChunk est inférieur ou égal à 0
   * @see #splitByMaxLines()
   */
  public List<UnifiedDiff> splitByMaxLines(int maxLinesPerChunk) {
    validateMaxLines(maxLinesPerChunk);
    return performSplitByMaxLines(maxLinesPerChunk);
  }

  /**
   * Divise le diff en chunks avec la taille par défaut.
   *
   * <p>Utilise la constante {@link #DEFAULT_MAX_LINES_PER_CHUNK} comme limite.
   *
   * @return Liste des diffs divisés avec la taille par défaut
   * @see #splitByMaxLines(int)
   */
  public List<UnifiedDiff> splitByMaxLines() {
    return splitByMaxLines(DEFAULT_MAX_LINES_PER_CHUNK);
  }

  /**
   * Valide le paramètre maxLinesPerChunk.
   *
   * @param maxLinesPerChunk Valeur à valider
   * @throws IllegalArgumentException si la valeur est invalide
   */
  private void validateMaxLines(int maxLinesPerChunk) {
    if (maxLinesPerChunk <= 0) {
      throw new IllegalArgumentException(
          "Le nombre maximum de lignes par chunk doit être positif, reçu: " + maxLinesPerChunk);
    }
  }

  /**
   * Effectue la division effective du diff en respectant la limite de lignes.
   *
   * <p>La logique reprend celle de la méthode originale en préservant la compatibilité avec
   * l'implémentation existante.
   *
   * @param maxLinesPerChunk Limite de lignes par chunk
   * @return Liste des chunks créés
   */
  private List<UnifiedDiff> performSplitByMaxLines(int maxLinesPerChunk) {
    List<UnifiedDiff> chunks = new ArrayList<>();

    if (structuredDiff.files.isEmpty()) {
      return chunks;
    }

    UnifiedDiff currentChunk = new UnifiedDiff();
    int currentLineCount = 0;

    for (var fileDiff : structuredDiff.files) {
      for (var hunk : fileDiff.hunks) {
        int hunkLineCount = hunk.lines.size();

        // Si ajouter ce hunk dépasse la limite ET qu'on a déjà du contenu
        if (shouldStartNewChunk(currentLineCount, hunkLineCount, maxLinesPerChunk)) {
          chunks.add(currentChunk);
          currentChunk = new UnifiedDiff();
          currentLineCount = 0;
        }

        // Utilise la méthode existante pour créer une copie avec un seul hunk
        var singleHunkFile = fileDiff.shallowCopyWithSingleHunk(hunk);
        currentChunk.files.add(singleHunkFile);
        currentLineCount += hunkLineCount;
      }
    }

    // Ajouter le dernier chunk s'il contient des données
    if (!currentChunk.files.isEmpty()) {
      chunks.add(currentChunk);
    }

    return chunks;
  }

  /**
   * Détermine s'il faut commencer un nouveau chunk.
   *
   * @param currentLineCount Nombre de lignes dans le chunk actuel
   * @param hunkLineCount Nombre de lignes du hunk à ajouter
   * @param maxLinesPerChunk Limite maximale par chunk
   * @return true s'il faut commencer un nouveau chunk
   */
  private boolean shouldStartNewChunk(
      int currentLineCount, int hunkLineCount, int maxLinesPerChunk) {
    return currentLineCount + hunkLineCount > maxLinesPerChunk && currentLineCount > 0;
  }

  /**
   * Calcule le nombre total de lignes dans ce diff.
   *
   * @return Le nombre total de lignes de toutes les modifications
   */
  public int getTotalLineCount() {
    return structuredDiff.files.stream()
        .flatMap(file -> file.hunks.stream())
        .mapToInt(hunk -> hunk.lines.size())
        .sum();
  }

  /**
   * Calcule le nombre total de fichiers modifiés dans ce diff.
   *
   * @return Le nombre de fichiers affectés par les modifications
   */
  public int getModifiedFileCount() {
    return structuredDiff.files.size();
  }

  /**
   * Vérifie si ce diff contient des modifications.
   *
   * @return true si le diff contient au moins une modification, false sinon
   */
  public boolean hasModifications() {
    return !structuredDiff.files.isEmpty()
        && structuredDiff.files.stream().anyMatch(file -> !file.hunks.isEmpty());
  }

  /**
   * Retourne un résumé textuel de ce bundle de diff.
   *
   * @return Description concise du contenu du diff
   */
  public String getSummary() {
    if (!hasModifications()) {
      return "Diff vide - aucune modification";
    }

    return String.format(
        "Diff: %d fichier(s) modifié(s), %d ligne(s) total",
        getModifiedFileCount(), getTotalLineCount());
  }

  /**
   * Retourne la représentation unifiée du diff structuré.
   *
   * <p>Délègue à la méthode {@link UnifiedDiff#toUnifiedString()} pour générer le format textuel
   * standard.
   *
   * @return Le diff au format unifié Git
   * @see UnifiedDiff#toUnifiedString()
   */
  public String getUnifiedDiffString() {
    return structuredDiff.toUnifiedString();
  }

  /**
   * Méthode toString personnalisée pour un affichage informatif.
   *
   * @return Représentation textuelle de ce bundle
   */
  @Override
  public String toString() {
    return String.format(
        "DiffAnalysisBundle[%s, rawText=%d caractères]", getSummary(), rawDiffText.length());
  }
}
